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

package org.qubership.atp.mia.repo.impl;

import static java.lang.String.format;
import static org.qubership.atp.mia.model.Constants.ERROR_SSH_DOWNLOAD_FAILED;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

import javax.xml.ws.Holder;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.repo.impl.pool.ssh.ConnectionPool;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ShellRepository {

    private static final String INTERACTIVE_FORMAT = "%s > %s 2>&1";
    public final String genevaParamKey = "exportGenevaDate";

    private final MiaContext miaContext;
    private final ConnectionPool sshServerPool;
    private final MetricsAggregateService metricsService;

    private static String getRightsMsg(File internalDir) {
        return format("Local directory %s read rights and %s write rights",
                internalDir.canRead() ? "has" : "doesn't have",
                internalDir.canWrite() ? "has" : "doesn't have");
    }

    /**
     * Transfer file on server.
     */
    public void transferFileOnServer(Command command, String pathToFile, String pathToUpLoad) {
        getSshConnection(getServer(command), man -> man.transferFileOnServer(pathToFile, pathToUpLoad));
    }

    /**
     * Upload file on server.
     */
    public void uploadFileOnServer(Server server, String pathToUpLoad, File file) {
        String workDir = miaContext.getFlowData().getCustom(Constants.CustomParameters.WORKING_DIRECTORY, miaContext);
        getSshConnection(server, man -> man.uploadFileOnServer(file, pathToUpLoad, workDir));
    }

    /**
     * Get file from server.
     */
    public CommandOutput getFileOnServer(Command command, String pathToFile, boolean isDisplayed) {
        final Holder<File> file = new Holder<>();
        String errorMessage = "";
        Server server = getServer(command);
        try {
            getSshConnection(server, ssh -> file.value = ssh.getFileFromServer(pathToFile,
                    miaContext.getFlowData().getCustom(Constants.CustomParameters.WORKING_DIRECTORY, miaContext)));
        } catch (Exception e) {
            log.error("Can't get file on server. ProcessName: {},  Server: {}, Command: {}, filePath: {}, Message: {}",
                    miaContext.getFlowData().getCustom(Constants.CustomParameters.PROCESS_NAME, miaContext),
                    server.toString(), command.getToExecute(), pathToFile, e.getMessage(), e);
            errorMessage = e.getMessage();
        }
        CommandOutput output;
        if (file.value != null) {
            long fileSizeInKb = file.value.length() / 1024;
            log.info("[SIZE] Downloaded file size : {} kb", fileSizeInKb);
            metricsService.requestSshDownloadFileSize(fileSizeInKb);
            output = new CommandOutput(file.value.getPath(), pathToFile, isDisplayed, miaContext);
        } else {
            output = fileNotFound(miaContext.getLogPath().toString(), pathToFile, errorMessage);
        }
        return output;
    }

    private String updateCommand(String logFileName, String commandValue, String system) {
        final StringJoiner command =
                new StringJoiner(miaContext.getConfig().getCommonConfiguration().getCommandShellSeparator());
        final Map<String, String> params = miaContext.getFlowData().getParameters();
        checkExportGenevaDateIsValid(params);
        miaContext.getShellPrefixes(system).forEach((k, prefixValue) -> {
            prefixValue = miaContext.evaluate(prefixValue, params);
            final String prefixKey = k.toLowerCase();
            if (prefixKey.startsWith("always")) {
                command.add(prefixValue);
            } else {
                if (params.containsKey(k)) {
                    String param = params.get(k);
                    if (param != null && !param.isEmpty() && !param.equals("false")) {
                        command.add(format(prefixValue, params.get(k)));
                    }
                }
            }
        });
        command.add(format(INTERACTIVE_FORMAT, miaContext.evaluate(commandValue, params), logFileName));
        return command.toString();
    }

    /**
     * Gets Server.
     *
     * @param command command
     * @return Server of command
     */
    public Server getServer(Command command) {
        return miaContext.getFlowData().getSystem(command.getSystem()).getServer(Server.ConnectionType.SSH);
    }

    // check genevaExportDate to avoid its execution in SSH command
    private void checkExportGenevaDateIsValid(Map<String, String> params) {
        if (params.containsKey(genevaParamKey)) {
            String param = params.get(genevaParamKey);
            boolean isExportGenevaInvalid = param == null || param.isEmpty() || !param.equals("true");
            if (isExportGenevaInvalid) {
                log.warn("Command has incorrect value of genevaParamKey [{}], removing it from parameters",
                        genevaParamKey);
                params.remove(genevaParamKey);
            }
        }
    }

    private static void addResponseTextToError(CommandOutput logFile, String command, String sshOutput) {
        boolean commandIsNotEmpty = Strings.isNotEmpty(command);
        if (commandIsNotEmpty && !logFile.getMarkedContent().isEmpty() && Strings.isNotEmpty(sshOutput)) {
            logFile.addContent("------------------------------\n", Statuses.FAIL);
            logFile.addContent("Ssh part of execution response\n", Statuses.FAIL);
            String cutCommand = command.split("\n")[0];
            int maxSize = 20;
            boolean toTakeElements = false;
            Iterator<String> it = Arrays.stream(sshOutput.split("\n")).iterator();
            while (it.hasNext() && logFile.getMarkedContent().size() < maxSize) {
                String line = it.next();
                if (line.contains(cutCommand)) {
                    toTakeElements = true;
                } else if (line.contains("STOP")) {
                    toTakeElements = false;
                }
                if (toTakeElements) {
                    logFile.addContent(line, Statuses.FAIL);
                }
            }
        }
    }

    /**
     * Execute ssh command.
     */
    public CommandResponse executeAndGetLog(Command command) {
        final Server server = getServer(command);
        FlowData flowData = miaContext.getFlowData();
        flowData.addParameters(server.getProperties());
        final String fileName = miaContext.createLogFileName(command);
        final String logFileName = miaContext.evaluate(
                FileUtils.tempFileName(flowData.getCustom(Constants.CustomParameters.WORKING_DIRECTORY, miaContext),
                        fileName));
        CommandResponse commandResponse = new CommandResponse();
        final String updatedCommand = updateCommand(logFileName, command.getToExecute(), command.getSystem());
        Holder<String> output = new Holder<>(null);
        SshConnectionResponse sshResponse = getSshConnection(server, man -> {
            output.value = man.runCommand(updatedCommand);
        });
        commandResponse.setCommand(sshResponse.getExecutedCommand());
        commandResponse.setConnectionInfo(sshResponse.connectionInfo());
        if (commandResponse.getErrors() == null || commandResponse.getErrors().size() < 1) {
            CommandOutput logFile = getFileOnServer(command, logFileName, true);
            addResponseTextToError(logFile, command.getToExecute(), output.value);
            commandResponse.concatCommandOutput(logFile);
        }
        return commandResponse;
    }

    /**
     * Generates when file not found.
     *
     * @param internalPathToFile path to output file on local machine
     * @param externalPathToFile path to output file on server
     * @param errorMessage       message of Error
     */
    public CommandOutput fileNotFound(@NotNull String internalPathToFile, String externalPathToFile,
                                      String errorMessage) {
        log.error("SSH download fail, internal path: {}, external path: {}, error message: {}",
                internalPathToFile, externalPathToFile, errorMessage);
        List<String> errors = new ArrayList<>();
        errors.add(ERROR_SSH_DOWNLOAD_FAILED + " External path : " + externalPathToFile);
        errors.add("Local directory path: " + internalPathToFile);
        File internalDir = new File(internalPathToFile);
        if (internalDir.exists()) {
            if (!internalDir.isDirectory()) {
                if (internalDir.canRead() && internalDir.canWrite()) {
                    errors.add("Error! Make sure that Remote file exists on server: " + externalPathToFile);
                } else {
                    errors.add(getRightsMsg(internalDir));
                }
            } else {
                errors.add("Local path :" + internalPathToFile + " is a Directory and not a file");
            }
        } else {
            errors.add("Local directory not created!");
            errors.add(getRightsMsg(internalDir));
        }
        if (!com.google.common.base.Strings.isNullOrEmpty(errorMessage)) {
            errors.add("Error message: " + errorMessage);
        }
        return new CommandOutput(internalPathToFile, externalPathToFile, errors, true, miaContext);
    }

    /**
     * Gets {@code SshConnectionManager} from ssh connections pool.
     *
     * @param server   server
     * @param consumer SshConnectionManager Consumer
     * @return SshConnectionManager instance
     */
    private SshConnectionResponse getSshConnection(Server server, Consumer<SshConnectionManager> consumer) {
        String prefix = miaContext.getExternalPrefix();
        SshConnectionManager sshConnection = sshServerPool.getConnection(server, prefix);
        consumer.accept(sshConnection);
        return new SshConnectionResponse(sshConnection);
    }

    public void resetCache() {
        sshServerPool.resetCache();
    }
}
