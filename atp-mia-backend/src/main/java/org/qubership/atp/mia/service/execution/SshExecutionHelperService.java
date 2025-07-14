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

package org.qubership.atp.mia.service.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshMissedParameterException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshPathForDownloadEmptyException;
import org.qubership.atp.mia.exceptions.businesslogic.ssh.SshTransferFileFailException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.generation.Template;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.repo.impl.ShellRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.stereotype.Service;

import clover.com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SshExecutionHelperService {

    private static final String FIND_COMMAND = "find";

    private final ShellRepository shellRepository;
    private final MiaContext miaContext;
    private final MiaFileService miaFileService;

    /**
     * Executes single SSH command.
     */
    @AtpJaegerLog()
    public CommandResponse executeSingleCommand(Command command) {
        return shellRepository.executeAndGetLog(command);
    }

    /**
     * Generates a file from ethalon,
     * uploads it on server
     * and executes ssh command.
     */
    @AtpJaegerLog()
    public CommandResponse executeCommandAndGenerateFile(Command command) {
        if (Strings.isNullOrEmpty(Objects.requireNonNull(command.getEthalonFilesForGeneration()).get(0))
                || Strings.isNullOrEmpty(Objects.requireNonNull(command.getNamesOfFilesForGeneration()).get(0))
                || Strings.isNullOrEmpty(command.getPathForUpload())) {
            String params = "ethalonFilesForGeneration, namesOfFilesForGeneration, pathForUpload.";
            throw new SshMissedParameterException(params);
        }
        Server server = shellRepository.getServer(command);
        miaContext.getFlowData().addParameters(server.getProperties());
        String ethalonFileName = miaContext.evaluate(command.getEthalonFilesForGeneration().get(0));
        String nameOfFileForGeneration = miaContext.evaluate(command.getNamesOfFilesForGeneration().get(0));
        Template template = new Template(miaContext, miaFileService, ethalonFileName, nameOfFileForGeneration,
                command.getFileExtension(), command.definedCharsetForGeneratedFile());
        //save generated file to flow data
        applyIfSaveGeneratedFiles(command, (name, value) ->
                miaContext.getFlowData().addParameter(name, template.getFileName()));
        template.evaluateFile();
        String pathForUpload = miaContext.evaluate(command.getPathForUpload());
        shellRepository.uploadFileOnServer(server, pathForUpload, template.getFile());
        final CommandResponse commandResponse = shellRepository.executeAndGetLog(command);
        commandResponse.addCommandOutput(
                new CommandOutput(template.getFile().getPath(), pathForUpload, true, miaContext));
        //Remove parameter with generated files if saved before
        applyIfSaveGeneratedFiles(command, (name, value) -> miaContext.getFlowData().removeParameter(name));
        return commandResponse;
    }

    /**
     * Uploads file from input on server,
     * executes command.
     */
    @AtpJaegerLog()
    public CommandResponse executeCommandAndUploadFile(Command command) {
        if (Strings.isNullOrEmpty(Objects.requireNonNull(command.getFilesForUpload().get(0)))
                || Strings.isNullOrEmpty(command.getPathForUpload())) {
            String params = "fileForUpload, pathForUpload";
            throw new SshMissedParameterException(params);
        }
        Server server = shellRepository.getServer(command);
        miaContext.getFlowData().addParameters(server.getProperties());
        String pathForUpload = miaContext.evaluate(command.getPathForUpload());
        for (String fileName : command.getFilesForUpload()) {
            String fullFileName = miaContext.evaluate(fileName);
            File fileForUpload = miaFileService.getFile(miaContext.evaluate(fullFileName));
            shellRepository.uploadFileOnServer(server, pathForUpload, fileForUpload);
        }
        return shellRepository.executeAndGetLog(command);
    }

    /**
     * Generates a event files from ethalon, uploads files on server, executes a ssh command.
     */
    @AtpJaegerLog()
    public CommandResponse generateEventFilesAndExecuteCommand(Command command) {
        if (Strings.isNullOrEmpty(Objects.requireNonNull(command.getEthalonFilesForGeneration()).get(0))
                || Strings.isNullOrEmpty(Objects.requireNonNull(command.getNamesOfFilesForGeneration()).get(0))
                || Strings.isNullOrEmpty(command.getPathForUpload())
                || Objects.requireNonNull(command.getValues()).size() < 1
                || command.getEthalonFilesForGeneration().size() != command.getNamesOfFilesForGeneration().size()) {
            String params = "ethalonFilesForGeneration, namesOfFilesForGeneration, fileExtension,"
                    + "pathForUpload, values. Size of ethalonFilesForGeneration must be equals to size of "
                    + "namesOfFilesForGeneration";
            throw new SshMissedParameterException(params);
        }
        final LinkedList<CommandOutput> commandOutputs = new LinkedList<>();
        final Server server = shellRepository.getServer(command);
        FlowData flowData = miaContext.getFlowData();
        flowData.addParameters(server.getProperties());
        final Iterator<String> ethalonFilesIterator = command.getEthalonFilesForGeneration().iterator();
        final Iterator<String> namesOfFilesForGeneration = command.getNamesOfFilesForGeneration().iterator();
        final String pathForUpload = miaContext.evaluate(command.getPathForUpload());
        while (ethalonFilesIterator.hasNext() && namesOfFilesForGeneration.hasNext()) {
            final String ethalonFile = miaContext.evaluate(ethalonFilesIterator.next());
            final String fileForGeneration = miaContext.evaluate(namesOfFilesForGeneration.next());
            final Template template = new Template(miaContext, miaFileService, ethalonFile, fileForGeneration,
                    command.getFileExtension(), command.definedCharsetForGeneratedFile());
            template.evaluateFile();
            shellRepository.uploadFileOnServer(server, pathForUpload, template.getFile());
            commandOutputs.add(new CommandOutput(template.getFile().getPath(), null, false, miaContext));
            //save generated files as list into FlowData
            applyIfSaveGeneratedFiles(command, (name, value) -> {
                value = Strings.isNullOrEmpty(value) ? "[]" : value;
                value = value.replaceAll("]", "," + template.getFileName() + "]").replaceAll("\\[,", "[");
                flowData.addParameter(name, value);
            });
        }
        final CommandResponse commandResponse = shellRepository.executeAndGetLog(command);
        //Remove parameter with generated files if saved before
        applyIfSaveGeneratedFiles(command, (name, value) -> flowData.removeParameter(name));
        Utils.getPathToFileOutOfLog(commandResponse.getCommandOutputs().get(0).getInternalPathToFile(),
                command.getRegexpForFileRetrieve()).forEach(path ->
                commandOutputs.add(shellRepository.getFileOnServer(command, pathForUpload + path.trim(), false)));
        commandResponse.addCommandOutputs(commandOutputs);
        return commandResponse;
    }

    /**
     * Executes a ssh command,
     * searches file into logs with regexp and
     * downloads this file.
     */
    @AtpJaegerLog()
    public CommandResponse executeCommandAndCheckFileOnServer(Command command) {
        if (Strings.isNullOrEmpty(command.getRegexpForFileRetrieve())) {
            String params = "regexpForFileRetrieve";
            throw new SshMissedParameterException(params);
        }
        final CommandResponse commandResponse = shellRepository.executeAndGetLog(command);
        Utils.getPathToFileOutOfLog(commandResponse.getCommandOutputs().get(0).getInternalPathToFile(),
                command.getRegexpForFileRetrieve()).forEach(path ->
                commandResponse.addCommandOutput(shellRepository.getFileOnServer(command, path,
                        command.getDisplayDownloadedFileContent())));
        return commandResponse;
    }

    /**
     * Executes a ssh command
     * and transfers files on server.
     */
    @AtpJaegerLog()
    public CommandResponse executeCommandAndTransferFileOnServer(Command command) {
        if (Strings.isNullOrEmpty(command.getRegexpForFileRetrieve())
                || Strings.isNullOrEmpty(command.getPathForUpload())) {
            String params = "regexpForFileRetrieve, pathForUpload";
            throw new SshMissedParameterException(params);
        }
        final Server server = shellRepository.getServer(command);
        miaContext.getFlowData().addParameters(server.getProperties());
        final CommandResponse commandResponse = shellRepository.executeAndGetLog(command);
        ArrayList<String> pathsToFiles = Utils.getPathToFileOutOfLog(
                commandResponse.getCommandOutputs().get(0).getInternalPathToFile(),
                command.getRegexpForFileRetrieve());
        final String pathForUpload = miaContext.evaluate(command.getPathForUpload());
        for (String pathToFile : pathsToFiles) {
            try {
                shellRepository.transferFileOnServer(command, pathToFile, pathForUpload);
                commandResponse.addDescription(String.format("File [%s] has been moved to [%s]",
                        pathToFile, pathForUpload));
            } catch (Exception e) {
                throw new SshTransferFileFailException(pathToFile, pathForUpload, e);
            }
        }
        return commandResponse;
    }

    /**
     * Executes a ssh command,
     * searches files on server with find + regexp
     * and downloads files from server.
     */
    @AtpJaegerLog()
    public CommandResponse executeCommandAndDownloadFilesFromServer(Command command) {
        if (Strings.isNullOrEmpty(Objects.requireNonNull(command.getPathsForDownload()).get(0))
                || Strings.isNullOrEmpty(command.getRegexpForFileRetrieve())) {
            String params = "pathsForDownload, regexpForFileRetrieve";
            throw new SshMissedParameterException(params);
        }
        // execute command
        final CommandResponse commandResponse = shellRepository.executeAndGetLog(command);
        commandResponse.addCommandOutputs(findAndDownloadFiles(command));
        return commandResponse;
    }

    private LinkedList<CommandOutput> findAndDownloadFiles(final Command command) {
        LinkedList<CommandOutput> outputs = new LinkedList<>();
        List<String> paths = command.getPathsForDownload();
        if (paths != null && paths.size() > 0) {
            String evaluateRegexp = miaContext.evaluate(command.getRegexpForFileRetrieve());
            for (String path : paths) {
                String evaluatePath = miaContext.evaluate(path);
                evaluatePath = evaluatePath.endsWith("*") ? evaluatePath : (evaluatePath) + " -maxdepth 1 ";
                String findCommand = FIND_COMMAND + " " + evaluatePath + " " + evaluateRegexp;
                command.setToExecute(findCommand);
                if (!Strings.isNullOrEmpty(command.getDelayForGeneration())) {
                    try {
                        TimeUnit.SECONDS.sleep(Long.parseLong(command.getDelayForGeneration()));
                    } catch (InterruptedException e) {
                        log.warn("Could not do delay for generation of dump.", e);
                    }
                }
                // find result files
                CommandOutput findOutput = null;
                try {
                    findOutput = shellRepository.executeAndGetLog(command).getCommandOutputs().get(0);
                } catch (Exception e) {
                    StringBuilder errMsg = new StringBuilder();
                    errMsg.append("Error during execution of find command [").append(findCommand).append("]. ");
                    if (!evaluatePath.endsWith("/")) {
                        errMsg.append("Try to add '/' (slash) at the end of pathsForDownload parameter in SSH process ")
                                .append("configuration if you specified a directory not a file!");
                    }
                    log.error("{}", errMsg, e);
                    String localPathToFile = miaContext.getLogPath().toString();
                    outputs.add(shellRepository.fileNotFound(localPathToFile, null, errMsg.toString()));
                }
                boolean isDisplay = command.getDisplayDownloadedFileContent();
                Optional.ofNullable(findOutput)
                        .map(CommandOutput::contentFromFile)
                        .ifPresent(resultPaths ->
                                resultPaths.forEach(resultPath ->
                                        outputs.add(shellRepository
                                                .getFileOnServer(command, resultPath, isDisplay))));
            }
        } else {
            throw new SshPathForDownloadEmptyException();
        }
        return outputs;
    }

    /**
     * Generates a files from ethalon,
     * uploads files on server,
     * executes a ssh command,
     * downloads result files.
     */
    @AtpJaegerLog()
    public CommandResponse uploadFilesAndDownloadResults(Command command) {
        if (Strings.isNullOrEmpty(Objects.requireNonNull(command.getEthalonFilesForGeneration()).get(0))
                || Strings.isNullOrEmpty(Objects.requireNonNull(command.getNamesOfFilesForGeneration()).get(0))
                || Strings.isNullOrEmpty(command.getFileExtension())
                || Strings.isNullOrEmpty(command.getPathForUpload())
                || Objects.requireNonNull(command.getValues()).size() < 1) {
            String params = "ethalonFilesForGeneration, namesOfFilesForGeneration, fileExtension,"
                    + "pathForUpload, values.";
            throw new SshMissedParameterException(params);
        }
        Server server = shellRepository.getServer(command);
        miaContext.getFlowData().addParameters(server.getProperties());
        List<String> ethalonFiles = command.getEthalonFilesForGeneration();
        List<String> commandValues = new ArrayList<>(command.getValues());
        int ethalonFilesCount = ethalonFiles.size();
        int commandValuesCount = commandValues.size();
        final CommandResponse commandResponse = new CommandResponse();
        String pathForUpload = miaContext.evaluate(command.getPathForUpload());
        final String fileExtension = command.getFileExtension();
        for (int i = 0; i < ethalonFilesCount; i++) {
            final String nameOfFileForGeneration = miaContext.evaluate(command.getNamesOfFilesForGeneration().get(i));
            final Template template = new Template(miaContext, miaFileService, ethalonFiles.get(i),
                    nameOfFileForGeneration, fileExtension, command.definedCharsetForGeneratedFile());
            shellRepository.uploadFileOnServer(server, pathForUpload, template.getFile());
            //save generated file to flow data
            applyIfSaveGeneratedFiles(command, (name, value) -> miaContext.getFlowData().addParameter(name,
                    template.getFileName()));
            Command singleCommand = new Command(commandValuesCount == 1 ? commandValues.get(0) : commandValues.get(i));
            singleCommand.setSystem(command.getSystem());
            singleCommand.setPathForUpload(pathForUpload);
            commandResponse.addCommandResponse(shellRepository.executeAndGetLog(singleCommand));
            //Remove parameter with generated files if saved before
            applyIfSaveGeneratedFiles(command, (name, value) -> miaContext.getFlowData().removeParameter(name));
            String path = pathForUpload + nameOfFileForGeneration + fileExtension;
            if (!Strings.isNullOrEmpty(command.getDelayForGeneration())) {
                try {
                    TimeUnit.SECONDS.sleep(Long.valueOf(command.getDelayForGeneration()));
                } catch (InterruptedException e) {
                    log.warn("Could not do delay for generation of dump.", e);
                }
            }
            commandResponse.addCommandOutput(shellRepository.getFileOnServer(singleCommand, path,
                    command.getDisplayDownloadedFileContent()));
        }
        return commandResponse;
    }

    /**
     * Apply something with name and value of saveGeneratedFilesToParameter if it is exist.
     *
     * @param command  command
     * @param consumer consumer to call method of {@code SshConnectionManager}
     */
    private void applyIfSaveGeneratedFiles(Command command, BiConsumer<String, String> consumer) {
        if (!Strings.isNullOrEmpty(command.getSaveGeneratedFilesToParameter())) {
            final String parameterName = command.getSaveGeneratedFilesToParameter();
            consumer.accept(parameterName, miaContext.getFlowData().getParameters().get(parameterName));
        }
    }

    public void resetCache() {
        shellRepository.resetCache();
    }
}
