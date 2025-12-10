/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.atp.mia.service;

import static org.qubership.atp.mia.model.file.FileMetaData.PROJECT_FOLDER;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.mia.exceptions.fileservice.ArchiveFileNotFoundException;
import org.qubership.atp.mia.exceptions.fileservice.ArchiveIoExceptionDuringClose;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.ContentType;
import org.qubership.atp.mia.model.configuration.CommandPrefix;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.VariableFormat;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.TableMarker;
import org.qubership.atp.mia.model.impl.macros.MacroRegistryImpl;
import org.qubership.atp.mia.model.impl.macros.MacrosType;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.service.configuration.EnvironmentsService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.utils.AtpMacrosUtils;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.qubership.atp.mia.utils.EnvironmentVariableUtils;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiaContext {

    private final ProjectConfigurationService projectConfigurationService;
    private final ContextRepository contextRepository;
    private final String miaPotTemplate;
    private final EnvironmentsService environmentsService;
    @Value("${catalogue.url}")
    private String catalogueUrl;

    /**
     * Gets path for display on UI.
     *
     * @param path path
     * @return path for display on UI
     */
    public static Link getLogLinkOnUi(String path) {
        final String projectFolder = PROJECT_FOLDER.toString();
        final Path pathOnUi = path.contains(projectFolder)
                ? Paths.get(path.split(projectFolder)[1])
                : Paths.get(path);
        final String parent = pathOnUi.getParent() == null ? "" : pathOnUi.getParent().toString();
        final String fileName = pathOnUi.getFileName().toString();
        String encodedFileName = fileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            log.error("Fail to encode " + fileName);
        }
        return new Link("/rest/downloadFile" + FilenameUtils.separatorsToUnix(parent) + "/" + encodedFileName,
                fileName);
    }

    /**
     * Creates filename for supported content type if content-dispostion not present in header.
     */
    public String createFileName(ContentType contentType) {
        String accountNumber = Strings.nullToEmpty(getFlowData().getCustom(
                Constants.CustomParameters.ACCOUNT_NUMBER, this)).trim().replaceAll(" ", "_");
        String prefix = StringUtils.deleteWhitespace(
                getFlowData().getParameters().get("processName")) + "_" + accountNumber;
        return prefix + Utils.getTimestampFile() + contentType.getExtension();
    }

    /**
     * Creates log file name.
     *
     * @param command command
     * @return log file name
     */
    public String createLogFileName(Command command) {
        String fileName;
        if (command.getLogFileNameFormat() != null) {
            fileName = StringUtils.deleteWhitespace(evaluate(command.getLogFileNameFormat()));
        } else {
            String accountNumber = Strings.nullToEmpty(getFlowData().getCustom(
                    Constants.CustomParameters.ACCOUNT_NUMBER, this)).trim().replaceAll(" ", "_");
            String prefix = StringUtils.deleteWhitespace(
                    getFlowData().getParameters().get("processName")) + "_" + accountNumber;
            fileName = prefix + Utils.getTimestampFile() + ".log";
        }
        return fileName;
    }

    /**
     * Creates log file name.
     *
     * @param suffix    suffix of the file
     * @param extension extension of the file
     * @return log file name with extension
     */
    public String createLogFileName(String suffix, String extension) {
        String prefix = StringUtils.deleteWhitespace(getFlowData().getParameters().get("processName")) + "_" + suffix;
        return prefix + Utils.getTimestampFile() + "." + extension;
    }

    /**
     * Creates csv file name.
     *
     * @param tableName string
     * @return csv file name
     */
    public String createTableFileName(String tableName) {
        String fileName;
        String prefix = StringUtils.deleteWhitespace(getFlowData().getParameters().get("processName"));
        prefix = StringUtils.isBlank(tableName) ? prefix
                : prefix + "_" + tableName;
        String accountNumber = Strings.nullToEmpty(getFlowData().getCustom(Constants.CustomParameters.ACCOUNT_NUMBER,
                        this))
                .trim().replaceAll(" ", "_");
        prefix = StringUtils.isBlank(accountNumber) ? prefix
                : prefix + "_" + accountNumber;
        fileName = prefix + Utils.getTimestampFile() + ".csv";
        return fileName;
    }

    /**
     * Evaluate or replace macros in string.
     */

    public String evaluate(String text) {
        return evaluate(text, new HashMap<>());
    }

    /**
     * Evaluate or replace macros in string.
     */
    public String evaluate(String text, Map<String, String> additionalParameters) {
        if (!Strings.isNullOrEmpty(text)) {
            final String variableFormat = getConfig().getCommonConfiguration().getVariableFormat();
            final VariableFormat varFormat = new VariableFormat(variableFormat);
            Map<String, String> parameters = new HashMap<>(getFlowData().getParameters());
            parameters.putAll(additionalParameters);
            //evaluate inside and after that evaluate macros
            return evaluateWithMacroses(evaluateInside(text, varFormat, parameters));
        }
        return text;
    }

    /**
     * Evaluate or replace macros in table marker.
     */
    public TableMarker evaluateTableMarker(TableMarker tableMarker) {
        TableMarker newTableMarker = new TableMarker();
        LinkedHashMap<String, String> expectedResultForQuery = new LinkedHashMap<>();
        if (tableMarker.getTableRowCount() != null) {
            newTableMarker.setTableRowCount(evaluate(tableMarker.getTableRowCount()));
        }
        if (tableMarker.getExpectedResultForQuery() != null) {
            for (Map.Entry<String, String> entry : tableMarker.getExpectedResultForQuery().entrySet()) {
                expectedResultForQuery.put(entry.getKey(), evaluate(entry.getValue()));
            }
            newTableMarker.setExpectedResultForQuery(expectedResultForQuery);
        }
        return newTableMarker;
    }

    /**
     * Get configuration using project ID from FlowData.
     *
     * @return ProjectConfiguration
     */
    public ProjectConfiguration getConfig() {
        return projectConfigurationService.getConfigByProjectId(getProjectId());
    }

    /**
     * Gets externalPrefix from properties file and update it with values from {@code FlowData}.
     */
    public String getExternalPrefix() {
        final String prefix = getConfig().getCommonConfiguration().getExternalEnvironmentPrefix();
        return Strings.isNullOrEmpty(prefix) ? "" : evaluate(prefix);
    }

    /**
     * Get FlowData.
     *
     * @return FlowData
     */
    public FlowData getFlowData() {
        return contextRepository.getContext();
    }

    /**
     * Gets path of project for log.
     *
     * @return log path
     */
    public Path getLogPath() {
        return getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_LOG);
    }

    /**
     * POT template.
     *
     * @return path to project
     */
    public File getPotTemplate() {
        return new File(miaPotTemplate);
    }

    /**
     * Gets path of project for log.
     *
     * @return log path
     */
    public Path getProjectFilePath() {
        return getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_PROJECT, null);
    }

    /**
     * Get project ID from FlowData.
     *
     * @return project ID
     */
    public UUID getProjectId() {
        return getFlowData().getProjectId();
    }

    /**
     * Gets path of project for files with needed type.
     *
     * @param projectFileType ProjectFileType
     * @return path
     */
    public Path getProjectPathWithType(ProjectFileType projectFileType) {
        FlowData flowData = contextRepository.getContext();
        return projectConfigurationService.getProjectPathWithType(flowData.getProjectId(), projectFileType,
                flowData.getSessionId());
    }

    /**
     * Gets path of project for files with needed type.
     *
     * @param projectFileType ProjectFileType
     * @return path
     */
    public Path getProjectPathWithType(ProjectFileType projectFileType, UUID sessionId) {
        return projectConfigurationService.getProjectPathWithType(contextRepository.getContext().getProjectId(),
                projectFileType, sessionId);
    }

    /**
     * Gets shell prefixes.
     *
     * @return list of prefixes from application.properties
     */
    public LinkedHashMap<String, String> getShellPrefixes(String system) {
        final Optional<CommandPrefix> commandPrefix =
                getConfig().getCommonConfiguration().getCommandShellPrefixes().stream()
                        .filter(p -> p.getSystem().equals(system)).findFirst();
        if (commandPrefix.isPresent()) {
            return commandPrefix.get().getPrefixes();
        }
        return new LinkedHashMap<>();
    }

    /**
     * Gets path of project for upload files.
     *
     * @return upload path
     */
    public Path getUploadsPath() {
        return getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_UPLOAD);
    }

    /**
     * Encodes MIA URL for response to ITF export.
     *
     * @param miaPath     path to mia process.
     * @param processName name of the process.
     * @return prepared url to IFT response.
     */
    public String prepareMiaURL(String miaPath, String processName) {
        String urlSuffix = "";
        try {
            String urlParams = "";
            String[] sections = miaPath.split("/");
            for (String section : sections) {
                urlParams = urlParams + section + ",";
            }
            urlSuffix = URLEncoder.encode(urlParams + processName, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error(ErrorCodes.MIA_1812_MIA_URL_ENCODE_FAIL.getMessage(processName));
        }
        return catalogueUrl + "/project/" + getProjectId() + "/mia?path=" + urlSuffix;
    }

    /**
     * Replaces spaces to `_` to avoid error during bash processes.
     */
    public void replaceSpacesInAccountNumber() {
        getFlowData().getParameters().computeIfPresent(Constants.CustomParameters.ACCOUNT_NUMBER.toString(),
                (k, v) -> v.replaceAll(" ", "_"));
    }

    /**
     * Set context by project ID and session ID.
     *
     * @param projectId project ID
     * @param sessionId session ID
     */
    public void setContext(UUID projectId, UUID sessionId) {
        Preconditions.checkNotNull(projectId, "No project specified.");
        String projectName = environmentsService.getProject(projectId).getName();
        log.debug("Set project id '{}' with name '{}' to FlowData", projectId, projectName);
        FlowData flowData = new FlowData(projectId, projectName, sessionId == null ? UUID.randomUUID() : sessionId);
        contextRepository.setContext(flowData);
    }

    /**
     * Set context by request, project ID and environment name.
     *
     * @param request         request
     * @param projectId       project ID
     * @param environmentName environment name
     */
    public void setContext(ExecutionRequest request, UUID projectId, String environmentName) {
        setContext(projectId, request.getSessionId());
        Preconditions.checkNotNull(environmentName, "No environmentName specified in parameters.");
        log.debug("Set environment with name '{}' to FlowData.", environmentName);
        getFlowData().setEnvironment(environmentsService.getEnvByName(projectId, environmentName));
        setFlowDataFromRequest(request);
    }

    /**
     * It adds request parameters to flow data Parameters.
     *
     * @param request MIA Execution Request
     */
    public void setFlowDataFromRequest(ExecutionRequest request) {
        FlowData flowData = getFlowData();
        if (request.getFlowData() != null) {
            flowData.setTestDataWorkbook(request.getFlowData().getTestDataWorkbook());
            flowData.setParameters(request.getFlowData().getParameters());
            //Evaluate only macros
            flowData.getParameters().forEach((k, v) -> flowData.getParameters().put(k, evaluateWithMacroses(v)));
        }
        addCommonParametersFromConfig();
    }

    /**
     * Zip all files from command output.
     * If file is directory or not present the empty file with error will be created.
     * Generates output archive in Logs directory for current project id.
     *
     * @param processName name of executed process
     * @param filePaths   paths to zip files
     * @return link to generated archive
     */
    public Link zipCommandOutputs(String processName, List<String> filePaths) {
        String zipName = processName + Utils.getTimestampFile() + ".zip";
        return zipCommandOutputs(filePaths, getLogPath().resolve(zipName));
    }

    /**
     * Zip all files from command output.
     * If file is directory or not present the empty file with error will be created.
     *
     * @param filePaths paths to zip files
     * @param zipPath   path where to save resulting archive
     * @return link to generated archive
     */
    public Link zipCommandOutputs(List<String> filePaths, Path zipPath) {
        try (ZipOutputStream zos =
                     new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipPath.toFile())))) {
            zos.setMethod(ZipOutputStream.DEFLATED);
            int i = 0;
            for (String filePath : filePaths) {
                try {
                    File file = new File(filePath);
                    if (file.exists() && !file.isDirectory()) {
                        zos.putNextEntry(new ZipEntry(file.getName()));
                        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
                        zos.write(bytes, 0, bytes.length);
                    } else {
                        final String errMsg = ++i + "_Error_file_is_directory_or_not_present_" + file.getName();
                        zos.putNextEntry(new ZipEntry(errMsg));
                    }
                } catch (IOException e) {
                    log.error(ErrorCodes.MIA_2051_ARCHIVE_IO_ERROR_DURING_SAVE.getMessage(filePath, e.getMessage()));
                }
            }
        } catch (FileNotFoundException e) {
            throw new ArchiveFileNotFoundException(e);
        } catch (IOException e) {
            throw new ArchiveIoExceptionDuringClose(zipPath, e);
        }
        return getLogLinkOnUi(zipPath.toString());
    }

    /**
     * Add CommonParameters to FlowData from configuration.
     */
    private void addCommonParametersFromConfig() {
        projectConfigurationService.getConfigByProjectId(getProjectId()).getCommonConfiguration()
                .getCommonVariables().forEach((k, v) -> getFlowData().addParameter(k, evaluate(v)));
    }

    private String evaluateInside(String text, VariableFormat varFormat, Map<String, String> additionalParameters) {
        Set<LinkedHashSet<String>> avoidInfinityLoops = new HashSet<>();
        String extractMatches = varFormat.getMatches().toString();

        //Pre-compile regex once (avoid compiling per loop)
        Pattern pattern = Pattern.compile(extractMatches);
        final int max_Iteration = 1000;
        int iterationCount = 0;
        while (pattern.matcher(text).find()) {
            if (iterationCount++ > max_Iteration) {
                log.warn("Maximum iterations reached, exiting loop to prevent infinite loop.");
                break;
            }
            String newText = text;
            Matcher matcher = pattern.matcher(newText);
            LinkedHashSet<String> extractedVariables = new LinkedHashSet<>();
            while (matcher.find()) {
                for (int groupIdx = 1; groupIdx <= matcher.groupCount(); groupIdx++) {
                    extractedVariables.add(matcher.group(groupIdx));
                }
            }
            boolean infinityLoopFound = false;
            for (LinkedHashSet<String> avoidInfinityLoop : avoidInfinityLoops) {
                if (!extractedVariables.isEmpty()
                        && avoidInfinityLoop.size() == extractedVariables.size()
                        && avoidInfinityLoop.containsAll(extractedVariables)) {
                    infinityLoopFound = true;
                    break;
                }
            }
            if (infinityLoopFound) {
                log.warn("INFINITY LOOP found in {}", text);
                break;
            }
            avoidInfinityLoops.add(extractedVariables);
            for (String extractedVariable : extractedVariables) {
                if (additionalParameters.containsKey(extractedVariable)) {
                    String decryptedValue = CryptoUtils.decryptValue(additionalParameters.get(extractedVariable));
                    if (decryptedValue != null) {
                        newText = newText.replaceAll(
                                varFormat.getVariableAccordingFormat(extractedVariable),
                                Matcher.quoteReplacement(decryptedValue));
                    }
                }
            }
            text = newText;
        }
        return text;
    }

    /**
     * Evaluates text with using of macroses.
     */
    public String evaluateWithMacroses(String text) {
        boolean contains = text.contains("${");
        if (contains) {
            if (text.contains("${ENV") && text.contains("}") && text.indexOf("${Gen") < text.lastIndexOf("}")) {
                int start = text.indexOf("${ENV");
                int end = text.indexOf("}", start) + 1;
                String environmentVariable = text.substring(start, end);
                String environmentVariableValue = EnvironmentVariableUtils
                        .evaluateEnvironmentVariable(environmentVariable);
                if (environmentVariableValue == null) {
                    log.warn("Problem in evaluating Environment Variable Value. Either System or Connection or "
                            + "Connection parameter doesn't exist. Returning without Converting");
                    return text;
                } else {
                    return text.replace(environmentVariable, environmentVariableValue);
                }
            } else {
                log.info("Found ${ in text, so it will be replaced as macros");
                log.debug("${ in text: {}", text);
                int start = text.indexOf("${") + 2;
                int end = start == text.length() ? text.length() + 1 : text.indexOf("}", start);
                final String macrosText = text.substring(start, end);
                try {
                    final String typeAsString = macrosText.substring(0, macrosText.indexOf("("));
                    MacrosType marcosType = MacrosType.valueOf(typeAsString);
                    String[] macrosParams;
                    String macrosParameters = macrosText.substring(macrosText.indexOf("(") + 1, macrosText.indexOf(")"
                    ));
                    if (macrosParameters.contains("',")) {
                        macrosParams = macrosParameters.split("',");
                        for (int paramIdx = 0; paramIdx < macrosParams.length; paramIdx++) {
                            macrosParams[paramIdx] = macrosParams[paramIdx].trim().replaceAll("'", "");
                        }
                    } else {
                        macrosParams = macrosParameters.split(",");
                    }
                    return evaluateWithMacroses(replaceMacrosWithResultOfMacros(text, marcosType, macrosParams));
                } catch (IndexOutOfBoundsException e) {
                    log.error("Out of bound for macros '${{}}'.\n1. Probably you use curly brackets incorrectly please "
                            + "check { } that it's closes only macros.\n2. Also please check that macros available in"
                            + " MIA "
                            + "documentation", macrosText);
                } catch (Exception e) {
                    log.error("Implementation for macros type '${{}}' has not been found", macrosText);
                }
            }
        }
        if (text.matches("(?s).*\\$\\w+\\(.*\\)")
                || text.matches("(?s).*\\#\\w+\\(.*\\)")) {
            return AtpMacrosUtils.evaluateWithAtpMacros(text, getFlowData().getProjectId());
        }
        return text;
    }

    /**
     * Replaces macros with result of macros.
     */
    private String replaceMacrosWithResultOfMacros(String text, MacrosType macrosType, String[] macrosParams) {
        String macrosResult = MacroRegistryImpl.getMacros(macrosType.name()).evaluate(macrosParams);
        String textBeforeMacros = text.substring(0, text.indexOf("${"));
        int start = text.indexOf("${");
        int end = start == text.length() ? text.length() + 1 : text.indexOf("}", start) + 1;
        String textAfterMacros = start == text.length() ? "" : text.substring(end);
        return textBeforeMacros + macrosResult + textAfterMacros;
    }
}
