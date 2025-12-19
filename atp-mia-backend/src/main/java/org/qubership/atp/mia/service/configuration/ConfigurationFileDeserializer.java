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

package org.qubership.atp.mia.service.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import javax.xml.ws.Holder;

import org.apache.commons.lang.IllegalClassException;
import org.qubership.atp.mia.exceptions.configuration.DeserializeErrorInFileException;
import org.qubership.atp.mia.exceptions.configuration.DeserializeJsonConfigFailedException;
import org.qubership.atp.mia.exceptions.configuration.ErrorInFlowJsonException;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.ProjectNotFoundException;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.UselessParentheses")
public class ConfigurationFileDeserializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectMapper MAPPER_NO_ERROR =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ThreadLocal<List<SectionConfiguration>> threadSections = new ThreadLocal<>();
    private final ThreadLocal<List<CompoundConfiguration>> threadCompounds = new ThreadLocal<>();
    private final ThreadLocal<List<ProcessConfiguration>> threadProcesses = new ThreadLocal<>();
    private final ThreadLocal<List<ProjectDirectory>> threadDirectories = new ThreadLocal<>();
    private final ThreadLocal<Boolean> threadMigration = new ThreadLocal<>();
    private final ThreadLocal<List<ProjectFile>> threadFiles = new ThreadLocal<>();
    private final EnvironmentsService environmentsService;
    private final GridFsService gridFsService;
    private final AtpUserService atpUserService;
    private ThreadLocal<StringJoiner> validationResults = new ThreadLocal<>();

    /**
     * Deserialize config from file.
     *
     * @param pathToConfig pathToConfig
     * @return ProjectConfiguration
     * @throws IOException IOException
     */
    public ProjectConfiguration deserialize(Path pathToConfig, ProjectConfiguration flowConfig,
                                            Path miaPathForEtalonFiles, boolean isMigration) throws IOException {
        threadMigration.set(isMigration);
        Path flowConfigPath = pathToConfig.resolve("flow").resolve("Flow.json");
        JsonNode node = MAPPER.readTree(flowConfigPath.toFile());
        validationResults.set(new StringJoiner("\n"));
        UUID projectId = flowConfig.getProjectId();
        if (flowConfig.getProjectName() == null || flowConfig.getProjectName().isEmpty()) {
            flowConfig.setProjectName(environmentsService.getProjects().stream()
                    .filter(p -> p.getId().equals(projectId)).findFirst()
                    .orElseThrow(() -> new ProjectNotFoundException(projectId))
                    .getName());
        }
        try {
            flowConfig.setCommonConfiguration(parseConfiguration(node, "commonConfiguration", CommonConfiguration.class)
                    .toBuilder().projectId(projectId).projectConfiguration(flowConfig).build());
            if (node.has("defaultSystem")) {
                flowConfig.getCommonConfiguration().setDefaultSystem(node.get("defaultSystem").asText());
            }
            flowConfig.getCommonConfiguration().updateShellPrefixes();
            flowConfig.setHeaderConfiguration(parseConfiguration(node, "headerConfiguration", HeaderConfiguration.class)
                    .toBuilder().projectId(projectId).projectConfiguration(flowConfig).build());
            flowConfig.setPotHeaderConfiguration(parseConfiguration(node, "potHeaderConfiguration",
                    PotHeaderConfiguration.class)
                    .toBuilder().projectId(projectId).projectConfiguration(flowConfig).build());
            threadSections.set(new ArrayList<>(flowConfig.getAllSections()));
            if (flowConfig.getSections() == null) {
                flowConfig.setSections(new ArrayList<>());
            }
            flowConfig.getSections().clear();
            threadCompounds.set(new ArrayList<>(flowConfig.getCompounds()));
            if (flowConfig.getCompounds() == null) {
                flowConfig.setCompounds(new ArrayList<>());
            }
            flowConfig.getCompounds().clear();
            threadProcesses.set(new ArrayList<>(flowConfig.getProcesses()));
            if (flowConfig.getProcesses() == null) {
                flowConfig.setProcesses(new ArrayList<>());
            }
            flowConfig.getProcesses().clear();
            parseSections(flowConfig, node, flowConfigPath, null);
            parseIsolatedProcesses(flowConfig, pathToConfig.resolve("flow").toFile());
            updateConfigWithDefaultSystem(flowConfig.getProcesses(),
                    flowConfig.getCommonConfiguration().getDefaultSystem());
            //Processing with files
            threadDirectories.set(new ArrayList<>(flowConfig.getAllDirectories()));
            if (flowConfig.getDirectories() == null) {
                flowConfig.setDirectories(new ArrayList<>());
            }
            flowConfig.getDirectories().clear();
            threadFiles.set(new ArrayList<>(flowConfig.getFiles()));
            if (flowConfig.getFiles() == null) {
                flowConfig.setFiles(new ArrayList<>());
            }
            flowConfig.getFiles().clear();
            deserializeProjectDirectoriesAndFiles(pathToConfig, flowConfig, miaPathForEtalonFiles);
            flowConfig.setLastLoadedWhen(LocalDateTime.now());
        } catch (Exception e) {
            String err = "Error while reading flowConfig; "
                    + "FlowConfig path: [" + flowConfigPath + "];\n" + e.getMessage();
            log.error(err);
            validationResults.get().add(err);
        } finally {
            threadSections.remove();
            threadCompounds.remove();
            threadProcesses.remove();
            threadDirectories.remove();
            threadFiles.remove();
            flowConfig.setValidationResult(validationResults.get().toString());
            validationResults.remove();
            threadMigration.remove();
        }
        return flowConfig;
    }

    private void deserializeProjectDirectories(File directory,
                                               ProjectConfiguration projectConfiguration,
                                               ProjectDirectory parentDirectory,
                                               Path miaPathForEtalonFiles) {
        ProjectDirectory projectDirectory =
                findProjectDirectory(directory.getName(), parentDirectory).orElse(
                        ProjectDirectory.builder()
                                .id(threadMigration.get() ? null : UUID.randomUUID())
                                .name(directory.getName())
                                .parentDirectory(parentDirectory)
                                .projectConfiguration(projectConfiguration)
                                .build()
                );
        if (parentDirectory == null) {
            projectConfiguration.getDirectories().add(projectDirectory);
        } else {
            parentDirectory.getDirectories().add(projectDirectory);
        }
        for (File file : Optional.ofNullable(directory.listFiles()).map(Arrays::asList)
                .orElse(Collections.emptyList())) {
            if (file.isDirectory()) {
                deserializeProjectDirectories(file, projectConfiguration, projectDirectory, miaPathForEtalonFiles);
            } else {
                deserializeProjectFiles(file, projectDirectory, projectConfiguration, miaPathForEtalonFiles);
            }
        }
    }

    private ProjectConfiguration deserializeProjectDirectoriesAndFiles(Path pathFlowJson,
                                                                       ProjectConfiguration projectConfiguration,
                                                                       Path miaPathForEtalonFiles) {
        File etalonFiles = pathFlowJson.resolve(projectConfiguration.getCommonConfiguration().getEthalonFilesPath())
                .toFile();
        if (etalonFiles.exists()) {
            log.info("Storing of {} files to Mongo DB Process started", etalonFiles.getName());
            for (File file : Optional.ofNullable(etalonFiles.listFiles()).map(Arrays::asList)
                    .orElse(Collections.emptyList())) {
                if (file.isDirectory()) {
                    deserializeProjectDirectories(file, projectConfiguration, null, miaPathForEtalonFiles);
                } else {
                    deserializeProjectFiles(file, null, projectConfiguration, miaPathForEtalonFiles);
                }
            }
        }
        return projectConfiguration;
    }

    private void deserializeProjectFiles(File file, ProjectDirectory projectDirectory,
                                         ProjectConfiguration projectConfiguration,
                                         Path miaPathForEtalonFiles) {
        Optional<ProjectFile> optionalProjectFile = findProjectFile(file.getName(), projectDirectory);
        ProjectFile projectFile;
        if (optionalProjectFile.isPresent()) {
            projectFile = optionalProjectFile.get();
        } else {
            projectFile = ProjectFile.builder()
                    .id(threadMigration.get() ? null : UUID.randomUUID())
                    .name(file.getName())
                    .directory(projectDirectory)
                    .lastUpdateWhen(LocalDateTime.now())
                    .lastUpdateBy(atpUserService.getAtpUser())
                    .size(file.length())
                    .projectConfiguration(projectConfiguration)
                    .build();
        }
        projectFile.setGridFsObjectId(
                gridFsService.uploadFile(
                                new FileMetaData(
                                        projectConfiguration.getProjectId(),
                                        miaPathForEtalonFiles.resolve(projectFile.getPathFile()).toString(),
                                        ProjectFileType.MIA_FILE_TYPE_PROJECT),
                                file)
                        .toString());
        if (projectDirectory != null) {
            projectDirectory.getFiles().add(projectFile);
        }
        projectConfiguration.getFiles().add(projectFile);
    }

    private Optional<ProjectDirectory> findProjectDirectory(String directoryName, ProjectDirectory parentDirectory) {
        return threadDirectories.get().stream()
                .filter(d -> {
                    if (parentDirectory == null) {
                        return d.getName().equals(directoryName) && d.getParentDirectory() == null;
                    } else {
                        return d.getName().equals(directoryName)
                                && parentDirectory.getId().equals(d.getParentDirectory().getId());
                    }
                })
                .findFirst();
    }

    private Optional<ProjectFile> findProjectFile(String fileName, ProjectDirectory directory) {
        return threadFiles.get().stream()
                .filter(f -> {
                    if (directory == null) {
                        return f.getName().equals(fileName) && f.getDirectory() == null;
                    } else {
                        return f.getName().equals(fileName) && f.getDirectory() != null
                                && directory.getId().equals(f.getDirectory().getId());
                    }
                })
                .findFirst();
    }

    private UUID getCompoundId(JsonNode jsonNode) {
        if (threadMigration.get()) {
            return null;
        }
        Optional<CompoundConfiguration> compoundConfigurationOptional = threadCompounds.get().stream()
                .filter(c -> {
                    if (jsonNode.has("id") && c.getId().equals(UUID.fromString(jsonNode.get("id").asText()))) {
                        return true;
                    } else {
                        return c.getName().equals(jsonNode.get("name").asText());
                    }
                }).findFirst();
        if (compoundConfigurationOptional.isPresent()) {
            return compoundConfigurationOptional.get().getId();
        }
        return getIdFromJsonNode(jsonNode);
    }

    private UUID getIdFromJsonNode(JsonNode jsonNode) {
        if (jsonNode.has("id")) {
            return UUID.fromString(jsonNode.get("id").asText());
        }
        return null;
    }

    private UUID getProcessId(JsonNode jsonNode, Path fileName, String procName) {
        if (threadMigration.get()) {
            return null;
        }
        Optional<ProcessConfiguration> processConfigurationOptional = threadProcesses.get().stream()
                .filter(p -> {
                    if (jsonNode.has("id") && p.getId().equals(UUID.fromString(jsonNode.get("id").asText()))) {
                        return true;
                    } else {
                        return (fileName != null && fileName.equals(Paths.get(p.getPathToFile()).normalize()))
                                || (procName != null && procName.equals(p.getName()));
                    }
                }).findFirst();
        if (processConfigurationOptional.isPresent()) {
            return processConfigurationOptional.get().getId();
        }
        return jsonNode == null ? null : getIdFromJsonNode(jsonNode);
    }

    private UUID getSectionId(JsonNode jsonNode, UUID parentSectionId) {
        if (threadMigration.get()) {
            return null;
        }
        Optional<SectionConfiguration> sectionConfigurationOptional = threadSections.get().stream()
                .filter(s -> {
                    if (jsonNode.has("id") && s.getId().equals(UUID.fromString(jsonNode.get("id").asText()))) {
                        return true;
                    } else if (s.getName().equals(jsonNode.get("name").asText())) {
                        if (parentSectionId == null) {
                            return s.getParentSection() == null;
                        } else if (s.getParentSection() != null) {
                            return parentSectionId.equals(s.getParentSection().getId());
                        }
                    }
                    return false;
                }).findFirst();
        if (sectionConfigurationOptional.isPresent()) {
            return sectionConfigurationOptional.get().getId();
        }
        return getIdFromJsonNode(jsonNode);
    }

    private void parseCompound(ProjectConfiguration flowConfig,
                               Path flowConfigPath, JsonNode compoundJsonNode,
                               SectionConfiguration sectionConfiguration) throws IOException {
        String compoundName = compoundJsonNode.get("name").asText();
        UUID compoundId = getCompoundId(compoundJsonNode);
        CompoundConfiguration compound = flowConfig.getCompounds()
                .stream()
                .filter(c -> c.getName().equals(compoundName) || (c.getId() != null && c.getId().equals(compoundId)))
                .findAny()
                .orElseGet(() -> {
                    CompoundConfiguration compoundConfiguration = new CompoundConfiguration().toBuilder()
                            .id(compoundId)
                            .name(compoundName)
                            .referToInput(compoundJsonNode.has("referToInput")
                                    ? compoundJsonNode.get("referToInput").asText()
                                    : null)
                            .build();
                    for (JsonNode processNode : compoundJsonNode.get("processList")) {
                        parseProcess(flowConfig, flowConfigPath, processNode, sectionConfiguration,
                                compoundConfiguration);
                    }
                    compoundConfiguration.setProjectConfiguration(flowConfig);
                    if (sectionConfiguration != null) {
                        compoundConfiguration.getInSections().add(sectionConfiguration);
                    }
                    return compoundConfiguration;
                });
        if (sectionConfiguration != null) {
            sectionConfiguration.addCompound(compound);
        }
        flowConfig.getCompounds().add(compound);
    }

    /**
     * This method parses configurations of header and maps them to the class using ObjectMapper.
     */
    private <T> T parseConfiguration(JsonNode node, String nodeName, Class<T> clazz) {
        T headerConfiguration;
        if (node.has(nodeName)) {
            try {
                headerConfiguration = MAPPER.convertValue(node.get(nodeName), clazz);
            } catch (IllegalArgumentException e) {
                String message = ErrorCodes.MIA_0054_FLOW_CONTAIN_ERROR.getMessage(nodeName, e.getMessage());
                log.warn(message);
                this.validationResults.get().add(message);
                headerConfiguration = MAPPER_NO_ERROR.convertValue(node.get(nodeName), clazz);
                if (headerConfiguration == null) {
                    throw new ErrorInFlowJsonException(nodeName, e);
                }
            }
        } else {
            try {
                headerConfiguration = clazz.newInstance();
            } catch (Exception e) {
                throw new IllegalClassException("Can't create class " + clazz.getName());
            }
        }
        return headerConfiguration;
    }

    private void parseIsolatedProcesses(ProjectConfiguration flowConfig, File folder) {
        if (folder.exists()) {
            for (File file : Optional.ofNullable(folder.listFiles()).map(Arrays::asList)
                    .orElse(Collections.emptyList())) {
                if (file.isDirectory()) {
                    parseIsolatedProcesses(flowConfig, file);
                } else {
                    if (!file.getName().equals("Flow.json") && file.getName().endsWith(".json")) {
                        Path fileName = FileUtils.getPathToFileFromFile(file);
                        if (flowConfig.getProcesses().stream()
                                .noneMatch(p -> Paths.get(p.getPathToFile()).normalize().equals(fileName))) {
                            try {
                                ProcessSettings processSettings = MAPPER_NO_ERROR.readValue(file,
                                        ProcessSettings.class);
                                if (!processSettings.check()) {
                                    log.error("In file '{}' is not Process configuration", file.getName());
                                    continue;
                                }
                                String procName = uniqueProcName(processSettings.getName(), flowConfig);
                                processSettings.setName(procName);
                                ProcessConfiguration processConfiguration = ProcessConfiguration.builder()
                                        .id(null)
                                        .name(procName)
                                        .pathToFile(fileName.toString())
                                        .processSettings(processSettings)
                                        .projectConfiguration(flowConfig)
                                        .build();
                                threadProcesses.get().stream().forEach(p -> {
                                    if (p.getName().equals(procName)) {
                                        processConfiguration.setId(p.getId());
                                    }
                                });
                                flowConfig.getProcesses().add(processConfiguration);
                            } catch (Exception e) {
                                String message = "Unable to parse Isolated Process. '" + fileName + "' contains error:"
                                        + e.getMessage();
                                log.warn(message);
                                validationResults.get().add(message);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseProcess(ProjectConfiguration flowConfig,
                              Path flowConfigPath,
                              JsonNode processJsonNode,
                              SectionConfiguration sectionConfiguration,
                              CompoundConfiguration compoundConfiguration) {
        Path filePath;
        if (processJsonNode.has("template")) {
            filePath = Paths.get(processJsonNode.get("template").asText()).normalize();
        } else if (processJsonNode.has("process")) {
            filePath = Paths.get(processJsonNode.get("process").asText()).normalize();
        } else if (processJsonNode.has("Process")) {
            filePath = Paths.get(processJsonNode.get("Process").asText()).normalize();
        } else if (processJsonNode.has("pathToFile")) {
            filePath = Paths.get(processJsonNode.get("pathToFile").asText()).normalize();
        } else {
            filePath = Paths.get(processJsonNode.asText()).normalize();
        }
        UUID processId = getProcessId(processJsonNode, filePath, null);
        ProcessConfiguration processConfiguration = flowConfig.getProcesses()
                .stream()
                .filter(p -> filePath.equals(Paths.get(p.getPathToFile()).normalize())
                        || (p.getId() != null && p.getId().equals(processId)))
                .findAny()
                .orElseGet(() -> {
                    try {
                        ProcessSettings processSettings = MAPPER_NO_ERROR
                                .readValue(flowConfigPath.getParent().resolve(filePath).toFile(),
                                        ProcessSettings.class);
                        //generate name avoid duplication
                        String procName = uniqueProcName(processSettings.getName(), flowConfig);
                        processSettings.setName(procName);
                        ProcessConfiguration processConfigurationNew = ProcessConfiguration.builder()
                                .id(processId)
                                .name(procName)
                                .processSettings(processSettings)
                                .pathToFile(filePath.toString())
                                .projectConfiguration(flowConfig)
                                .build();
                        flowConfig.getProcesses().add(processConfigurationNew);
                        return processConfigurationNew;
                    } catch (Exception e) {
                        String message = "Fail parsing process from '" + filePath + "' file";
                        validationResults.get().add(message);
                        log.error(message, e);
                        return null;
                    }
                });
        if (processConfiguration != null) {
            if (compoundConfiguration != null) {
                processConfiguration.getInCompounds().add(compoundConfiguration);
                processConfiguration.getCompounds().add(compoundConfiguration.getName());
                compoundConfiguration.addProcess(processConfiguration);
            } else if (sectionConfiguration != null
                    && sectionConfiguration.getProcesses().stream()
                    .noneMatch(p -> p.getName().equals(processConfiguration.getName()))) {
                processConfiguration.getInSections().add(sectionConfiguration);
                processConfiguration.getSections().add(sectionConfiguration.getName());
                sectionConfiguration.addProcess(processConfiguration);
            }
        }
    }

    private String uniqueProcName(String originalName, ProjectConfiguration projectConfiguration) {
        Holder<String> procName = new Holder<>(originalName);
        int duplicateId = 0;
        while (projectConfiguration.getProcesses().stream().anyMatch(p -> p.getName().equals(procName.value))) {
            ++duplicateId;
            procName.value = originalName + "_duplicate" + duplicateId;
        }
        if (duplicateId > 0) {
            String message = "Process with name '" + originalName + "' already exist."
                    + " Make new name to '" + procName.value + "'";
            log.warn(message);
            validationResults.get().add(message);
        }
        return procName.value;
    }

    private void parseProcessesOrCompound(ProjectConfiguration flowConfig,
                                          Path flowConfigPath,
                                          JsonNode processesNode,
                                          SectionConfiguration sectionConfiguration) {
        for (JsonNode processJsonNode : processesNode) {
            try {
                String type;
                try {
                    type = processJsonNode.get("execType").asText();
                } catch (NullPointerException e) {
                    try {
                        type = processJsonNode.get("type").asText();
                    } catch (NullPointerException ex) {
                        JsonNode nameNode = processJsonNode.get("name");
                        String name = nameNode != null
                                ? nameNode.asText()
                                : ". You have second error: you forgot to set name! Check Flow.json for "
                                + "process without name.";
                        String message = "Can't find \"type\" in node " + name;
                        throw new NullPointerException(message);
                    }
                }
                if ("compound".equalsIgnoreCase(type)) {
                    parseCompound(flowConfig, flowConfigPath, processJsonNode,
                            sectionConfiguration);
                } else if ("config".equalsIgnoreCase(type) || "process".equalsIgnoreCase(type)) {
                    parseProcess(flowConfig, flowConfigPath, processJsonNode, sectionConfiguration, null);
                }
            } catch (JsonProcessingException e) {
                StringBuilder errorMessage = new StringBuilder("Error while reading of process: [")
                        .append(processJsonNode).append("].");
                if (processJsonNode.has("pathToFile")) {
                    errorMessage.append("\n<b>Check file ").append(processJsonNode.get("pathToFile"));
                    errorMessage.append(" at line: ").append(e.getLocation().getLineNr())
                            .append(", index: ").append(e.getLocation().getColumnNr()).append("</b>");
                }
                String err = errorMessage.toString().replaceAll("\n", "<p>");
                throw new DeserializeJsonConfigFailedException(err, e);
            } catch (IOException e) {
                throw new DeserializeErrorInFileException(flowConfigPath, processJsonNode, e);
            }
        }
    }

    private void parseSections(ProjectConfiguration flowConfig, JsonNode node, Path flowConfigPath,
                               SectionConfiguration parentSection) {
        JsonNode sections = node.get("sections");
        int sectionPlace = -1;
        for (JsonNode section : sections) {
            SectionConfiguration flowConfigSection = new SectionConfiguration();
            try {
                flowConfigSection.setId(getSectionId(section, parentSection == null ? null : parentSection.getId()));
                flowConfigSection.setName(section.get("name").asText());
                flowConfigSection.setParentSection(parentSection);
                flowConfigSection.setPlace(++sectionPlace);
            } catch (NullPointerException e) {
                log.error("Can't parse section, because it null");
            }
            if (section.has("processes")) {
                parseProcessesOrCompound(flowConfig, flowConfigPath, section.get("processes"), flowConfigSection);
            }
            if (section.has("sections")) {
                parseSections(flowConfig, section, flowConfigPath, flowConfigSection);
            }
            flowConfigSection.setProjectConfiguration(flowConfig);
            if (parentSection == null) {
                flowConfig.getSections().add(flowConfigSection);
            } else {
                parentSection.getSections().add(flowConfigSection);
            }
        }
    }

    /**
     * Update config with default system.
     *
     * @param processes     processes
     * @param defaultSystem default system name
     */
    private void updateConfigWithDefaultSystem(List<ProcessConfiguration> processes, String defaultSystem) {
        processes.forEach(p -> {
            ProcessSettings proc = p.getProcessSettings();
            proc.getCommand().setSystemIfNull(defaultSystem);
            if (proc.getPrerequisites() != null) {
                proc.getPrerequisites().forEach(prereq -> prereq.setSystemIfNull(defaultSystem));
            }
            if (proc.getValidations() != null) {
                proc.getValidations().forEach(valid -> valid.setSystemIfNull(defaultSystem));
            }
            if (proc.getInputs() != null) {
                proc.getInputs().forEach(imp -> imp.setSystemIfNull(defaultSystem));
            }
        });
    }
}
