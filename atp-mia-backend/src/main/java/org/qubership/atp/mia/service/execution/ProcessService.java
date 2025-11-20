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

import static org.qubership.atp.mia.model.impl.CommandResponse.getConnectionNameFromResponseType;
import static org.qubership.atp.mia.model.impl.executable.CommandType.SQL;
import static org.qubership.atp.mia.model.impl.executable.CommandType.getResponseType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.SerializationUtils;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.annotation.AtpSpanTag;
import org.qubership.atp.mia.exceptions.configuration.CurrentStatementListIsEmptyException;
import org.qubership.atp.mia.exceptions.configuration.ProcessOrCompoundNotFoundException;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.EnvConnectionNotFoundInContextException;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.EnvConnectionNotFoundInContextOrNullException;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.EnvSystemNotFoundException;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.EnvSystemNotFoundInContextException;
import org.qubership.atp.mia.exceptions.macrosandevaluations.PatternCompileException;
import org.qubership.atp.mia.exceptions.prerequisite.PrerequisiteNoRecordsAddedException;
import org.qubership.atp.mia.exceptions.prerequisite.PrerequisiteQueryNotValidException;
import org.qubership.atp.mia.exceptions.prerequisite.PrerequisiteTypeUnsupportedException;
import org.qubership.atp.mia.exceptions.runtimeerrors.UnsupportedCommandException;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.CommandType;
import org.qubership.atp.mia.model.impl.executable.Prerequisite;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.TableMarker;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.repo.impl.ProcessStatusRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.configuration.snapshot.CommonConfigurationSnapshot;
import org.qubership.atp.mia.service.configuration.snapshot.HeaderConfigurationSnapshot;
import org.qubership.atp.mia.service.SseEmitterService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.FileUtils;
import org.qubership.atp.mia.utils.HttpUtils;
import org.qubership.atp.mia.utils.Utils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessService {

    private final MiaContext miaContext;
    private final MiaFileService miaFileService;
    private final SshExecutionHelperService sshService;
    private final TestDataService testDataService;
    private final SqlExecutionHelperService sqlService;
    private final RestExecutionHelperService restService;
    private final SoapExecutionHelperService soapService;
    private final MetricsAggregateService metricsService;
    private final RecordingSessionsService recordingSessionsService;
    private final GridFsService gridFsService;
    private final ProcessStatusRepository processStatusRepository;
    private final String fileDownloadPrefix;
    private final String miaEntityUrlFormat;
    private final SseEmitterService sseEmitterService;

    /**
     * Execute command on local host.
     *
     * @param command command
     */
    @Deprecated
    public void executeCommandOnLocalHost(String command) {
    }

    /**
     * Executes current statement queries.
     *
     * @param executableName executable name
     * @return list of query results
     */
    @AtpJaegerLog(spanTags = @AtpSpanTag(key = "current.statement.name", value = "#executableName"))
    public List<SqlResponse> executeCurrentStatement(String executableName) {
        final List<Validation> currentStatement = new ArrayList<>();
        Optional<ProcessConfiguration> optionalProcess = miaContext.getConfig().getProcessByNameSafe(executableName);
        if (optionalProcess.isPresent()) {
            if (optionalProcess.get().getProcessSettings().getCurrentStatement() != null) {
                currentStatement.addAll(optionalProcess.get().getProcessSettings().getCurrentStatement());
            }
        } else {
            Optional<CompoundConfiguration> optionalCompound =
                    miaContext.getConfig().getCompoundByNameSafe(executableName);
            if (optionalCompound.isPresent()) {
                optionalCompound.get().getProcesses().forEach(p -> {
                    if (p.getProcessSettings().getCurrentStatement() != null) {
                        currentStatement.addAll(p.getProcessSettings().getCurrentStatement());
                    }
                });
            } else {
                throw new ProcessOrCompoundNotFoundException(executableName);
            }
        }
        if (currentStatement.isEmpty()) {
            throw new CurrentStatementListIsEmptyException(executableName);
        }
        return sqlService.executeValidations(currentStatement, new Command());
    }

    /**
     * Executes process.
     */
    @AtpJaegerLog(spanTags = @AtpSpanTag(key = "process.name", value = "#request.process"))
    public ExecutionResponse executeProcess(ExecutionRequest request, UUID sseId) {
        final long startDate = System.currentTimeMillis();
        ProjectConfiguration config = miaContext.getConfig();
        CommonConfigurationSnapshot commonConfiguration = miaContext.getCommonConfiguration();
        metricsService.processExecutionWasStarted();
        miaContext.replaceSpacesInAccountNumber();
        ProcessConfiguration process = config.getProcessByName(request.getProcess());
        MDC.put("miaProcessId", process.getId().toString());
        log.info("Execute process {}", process.getName());
        List<Switcher> switchersListFromBe = null;
        List<Switcher> switchersListFromFe = null;
        HeaderConfigurationSnapshot headerConfiguration = miaContext.getHeaderConfiguration();
        if (headerConfiguration != null) {
            List<Switcher> userSwitchersBe = headerConfiguration.getSwitchers() != null
                    ? new ArrayList<>(headerConfiguration.getSwitchers())
                    : new ArrayList<>();
            List<Switcher> systemSwitchersBe = headerConfiguration.getSystemSwitchers();
            List<Switcher> systemSwitchersBeClone =
                    systemSwitchersBe.stream().map(sbe -> sbe.clone()).collect(Collectors.toList());
            switchersListFromBe = Stream.of(userSwitchersBe, systemSwitchersBeClone)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toList());
        }
        if (request != null) {
            List<Switcher> userSwitchersFe = request.getSwitchers() != null
                    ? request.getSwitchers()
                    : new ArrayList<>();
            List<Switcher> systemSwitchersFe = request.getSystemSwitchers() != null
                    ? request.getSystemSwitchers()
                    : new ArrayList<>();
            switchersListFromFe = Stream.of(userSwitchersFe, systemSwitchersFe)
                    .flatMap(x -> x.stream())
                    .collect(Collectors.toList());
        }
        getActualStateOfSwitchers(switchersListFromBe, switchersListFromFe);
        ProcessSettings processSettings = (ProcessSettings) SerializationUtils.clone(process.getProcessSettings());
        Command configCommand = processSettings.getCommand();
        if (request.getCommand() != null && !request.getCommand().isEmpty()) {
            final String labelOrCommand = request.getCommand();
            if (configCommand.getAtpValues().containsKey(labelOrCommand)) {
                //If it is a label then get command by label from atp values
                configCommand.setToExecute(configCommand.getAtpValues().get(labelOrCommand));
            } else {
                configCommand.setToExecute(labelOrCommand);
            }
        } else if (request.getRest() != null) {
            configCommand.setRest(request.getRest());
        } else {
            configCommand.setToExecute(configCommand.getValue());
        }
        final ExecutionResponse response =
                executeProcess(replaceProcessSystems(processSettings), switchersListFromBe, commonConfiguration);
        response.setEntityId(process.getId());
        response.setEntityUrl(HttpUtils.getMiaEntityUrl(
                miaEntityUrlFormat,
                miaContext.getProjectId(),
                process.getId()));
        recordingSessionsService.addExecutionStep(response);
        gridFsService.saveLogResponseAfterExecution(response);
        response.setDuration(Utils.calculateDuration(startDate));
        sseEmitterService.updateResponseAndSendToEmitter(response, sseId, true, 0);
        return response;
    }

    private ExecutionResponse executeProcess(ProcessSettings processSettings, List<Switcher> switchersListFromBe,
                                             CommonConfigurationSnapshot commonConfiguration) {
        ExecutionResponse executionResponse = new ExecutionResponse();
        executionResponse.setProcessName(processSettings.getName());
        executionResponse.setProcessStatus(new ProcessStatus());
        Command command = processSettings.getCommand();
        if (switchersListFromBe != null) {
            switchersListFromBe.forEach(switcher -> {
                if (switcher.getActionType() != null && switcher.getActionType().equals("SQL")) {
                    if (switcher.isValue() && switcher.getActionTrue() != null) {
                        processSettings.addPrerequisites(
                                new Prerequisite(switcher.getActionType(),
                                        commonConfiguration.getDefaultSystem(),
                                        switcher.getActionTrue()));
                    }
                    if (!switcher.isValue() && switcher.getActionFalse() != null) {
                        processSettings.addPrerequisites(
                                new Prerequisite(switcher.getActionType(),
                                        commonConfiguration.getDefaultSystem(),
                                        switcher.getActionFalse()));
                    }
                }
            });
        }
        if (processSettings.getPrerequisites() != null) {
            executionResponse.setPrerequisites(executePrerequisites(processSettings.getPrerequisites(), command));
        }
        if (switchersListFromBe != null) {
            switchersListFromBe.forEach(switcher -> {
                if (switcher.getActionType() != null && switcher.getActionType().equals("SSH")
                        && command.getType().contains("SSH")) {
                    if (switcher.isValue() && switcher.getActionTrue() != null) {
                        command.setToExecute(switcher.getActionTrue() + "\n" + command.getToExecute());
                    }
                    if (!switcher.isValue() && switcher.getActionFalse() != null) {
                        command.setToExecute(switcher.getActionFalse() + "\n" + command.getToExecute());
                    }
                }
            });
        }
        //Execute command
        if (Strings.isNullOrEmpty(command.getSystem())) {
            command.setSystem(commonConfiguration.getDefaultSystem());
        }
        try {
            miaContext.getFlowData().getSystem(command.getSystem());
        } catch (Exception e) {
            throw new EnvSystemNotFoundException(command.getSystem());
        }
        if (command.getToExecute() != null) {
            executionResponse.setExecutedCommand(miaContext.evaluate(command.getToExecute()));
        }
        executionResponse.setCommandResponse(executeCommand(command));
        validateProcessStatus(executionResponse);
        // Update status according HTTP response code
        if (command.getRest() != null) {
            String statusCode = executionResponse.getCommandResponse().getConnectionInfo().get("code");
            executionResponse.getCommandResponse().setStatusCode(statusCode);
            boolean isCheckStatusCodeFlag = command.getRest().isCheckStatusCodeFlag();
            executionResponse.getCommandResponse().setCheckStatusCodeFlag(isCheckStatusCodeFlag);
            if (isCheckStatusCodeFlag) {
                executionResponse.getCommandResponse().setExpectedCodes(command.getRest().getCheckStatusCode());
                processStatusRepository.parseReturnCodeAndUpdateStatus(executionResponse);
            }
        }
        //Check logs for markers
        processStatusRepository.parseLogMarkers(executionResponse.getProcessStatus(), command);
        processStatusRepository.parseLogStatus(executionResponse);
        //Validation and check
        List<Validation> validations = processSettings.getValidations();
        if (validations != null) {
            executionResponse.setValidations(sqlService.executeValidations(validations, command));
            if (validations.stream().anyMatch(validation -> validation.getTableMarker() != null)
                    || validations.stream().anyMatch(validation -> validation.getExportVariables() != null)) {
                addValidationStatusAndSaveVariables(executionResponse, validations);
            }
        }
        //Get global variables
        if (processSettings.getGlobalVariables() != null) {
            HashMap<String, String> globalVariables = new HashMap<>();
            processSettings.getGlobalVariables().forEach((globalVariableKey, globalVariableValue) -> {
                if (globalVariableValue != null) {
                    String value = miaContext.evaluate(globalVariableValue);
                    if (value != null) {
                        globalVariables.put(globalVariableKey, value);
                        log.info("Parameter '{}' saved as '{}' to globalVariables", value, globalVariableKey);
                    }
                }
            });
            executionResponse.setGlobalVariables(globalVariables);
        }
        return executionResponse;
    }

    /**
     * Takes two parameters SYSTEM and CONNECTION_TYPE from {@link FlowData}
     * and adds corresponding environment variables using {@link ContextRepository}.
     */
    public void fillFlowDataWithEnvVars(String system, CommandResponse.CommandResponseType type) {
        if (!type.equals(CommandResponse.CommandResponseType.NO_SYSTEM_REQUIRED)) {
            String connection = getConnectionNameFromResponseType(type);
            fillFlowDataWithEnvVars(system, connection);
        }
    }

    /**
     * Adds all variables from connection to {@link FlowData}.
     *
     * @param system     name from environment.
     * @param connection connection name which placed inside the system.
     */
    public void fillFlowDataWithEnvVars(String system, String connection) {
        if (Strings.isNullOrEmpty(system) || Strings.isNullOrEmpty(connection)) {
            log.warn("System or connection is empty. Cannot fulfill variables.");
            return;
        }
        try {
            Server server = miaContext.getFlowData().getSystem(system).getServer(connection);
            miaContext.getFlowData().addParameters(server.getProperties().entrySet()
                    .stream()
                    .collect(Collectors.toMap(k -> {
                        if (k.getKey().equals("url")) {
                            return k.getKey().toUpperCase();
                        }
                        return k.getKey();
                    }, Map.Entry::getValue, (v1, v2) -> v1)));
            miaContext.getFlowData().addParameter("Connection_UID", server.getConnection().getId().toString());
            miaContext.getFlowData().addParameter("System_UID", server.getConnection().getSystemId().toString());
        } catch (IllegalArgumentException e) {
            throw new EnvConnectionNotFoundInContextException(connection, system);
        } catch (NullPointerException e) {
            throw new EnvConnectionNotFoundInContextOrNullException(system, connection);
        } catch (RuntimeException e) {
            throw new EnvSystemNotFoundInContextException(system);
        }
    }

    /**
     * Gets next bill date for account number.
     */
    public String getNextBillDate() {
        return sqlService.getNextBillDate();
    }

    /**
     * Replaces Systems in Command, PreRequisites and Validations of Process,
     * with Custom Input (if any).
     *
     * @param processSettings instance of Process
     * @return updated Process.
     */
    public ProcessSettings replaceProcessSystems(ProcessSettings processSettings) {
        processSettings.getCommand().setSystem(miaContext.evaluate(processSettings.getCommand().getSystem()));
        if (processSettings.getPrerequisites() != null) {
            processSettings.getPrerequisites().stream().forEach(prerequisite -> {
                prerequisite.setSystem(miaContext.evaluate(prerequisite.getSystem()));
            });
        }
        if (processSettings.getValidations() != null) {
            processSettings.getValidations().stream().forEach(validation -> {
                validation.setSystem(miaContext.evaluate(validation.getSystem()));
            });
        }
        return processSettings;
    }

    /**
     * Rest DB Cashe on Environment.
     *
     * @return true if clear successful.
     */
    public boolean resetDbCache() {
        return sqlService.resetDbCache();
    }

    private void addPrefixToDownloadLinks(CommandResponse commandResponse) {
        if (commandResponse.getSqlResponse() != null && commandResponse.getSqlResponse().getLink() != null) {
            commandResponse.getSqlResponse().getLink()
                    .setPath(fileDownloadPrefix + commandResponse.getSqlResponse().getLink().getPath());
        }
        if (commandResponse.getCommandOutputs() != null && commandResponse.getCommandOutputs().size() > 0) {
            commandResponse.getCommandOutputs().forEach(commandOutput -> {
                commandOutput.getLink().setPath(fileDownloadPrefix + commandOutput.getLink().getPath());
            });
        }
    }

    private void addValidationStatusAndSaveVariables(ExecutionResponse executionResponse,
                                                     List<Validation> validations) {
        for (SqlResponse sqlResponse : executionResponse.getValidations()) {
            Optional validationOption = validations.stream().filter(v -> {
                if (v.getValue().contains(".sql")) {
                    String content =
                            miaContext.evaluate(FileUtils.readFile(miaFileService.getFile(v.getValue()).toPath()));
                    return content.contains(sqlResponse.getQuery());
                } else {
                    return miaContext.evaluate(v.getValue()).equals(sqlResponse.getQuery());
                }
            }).findFirst();
            if (!validationOption.isPresent()) {
                log.debug("Validation options is not present, setting status to: FAIL");
                executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
            } else {
                Validation validation = (Validation) validationOption.get();
                TableMarker tableMarker = validation.getTableMarker();
                if (tableMarker != null
                        && (tableMarker.getTableRowCount() != null
                        || MapUtils.isNotEmpty(tableMarker.getExpectedResultForQuery()))) {
                    tableMarker = miaContext.evaluateTableMarker(validation.getTableMarker());
                    TableMarkerResult tableMarkerResult = processTableWithTableMarker(tableMarker, sqlResponse);
                    if (tableMarkerResult.getColumnStatuses() != null) {
                        for (TableMarkerResult.TableMarkerColumnStatus tableMarkerColumnStatus :
                                tableMarkerResult.getColumnStatuses()) {
                            if (tableMarkerColumnStatus.getStatus() == Statuses.FAIL) {
                                log.debug("Table Marker Column Status is FAIL, so setting response status to: FAIL");
                                executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
                                break;
                            }
                        }
                    }
                    if (tableMarkerResult.getTableRowCount() != null
                            && tableMarkerResult.getTableRowCount().getStatus() == Statuses.FAIL) {
                        log.debug("Table Row Count status is FAIL, so setting response status to: FAIL");
                        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
                    }
                    sqlResponse.setTableMarkerResult(tableMarkerResult);
                }
                if (validation.getExportVariables() != null) {
                    for (Map.Entry<String, String> entry : validation.getExportVariables().entrySet()) {
                        String columnName = entry.getValue();
                        String variableName = entry.getKey();
                        String value;
                        if (sqlResponse.getRecords() == 0) {
                            value = "Not Found";
                        } else if (!sqlResponse.getData().getColumns().stream()
                                .filter(s -> s.equalsIgnoreCase(columnName)).findFirst().isPresent()) {
                            value = "Column '" + columnName + "' not found";
                        } else {
                            String returnColumnName = sqlResponse.getData().getColumns().stream()
                                    .filter(s -> s.equalsIgnoreCase(columnName)).findFirst().get();
                            int rowIndex = 0;
                            if (sqlResponse.getData().getData().get(0).get(0).equals("ER")) {
                                rowIndex = 1;
                            }
                            value = sqlResponse.getData().getData().get(rowIndex).get(
                                    sqlResponse.getData().getColumns().indexOf(returnColumnName));
                        }
                        miaContext.getFlowData().addParameter(variableName, value);
                        log.info("Parameter '{}' saved as '{}' from table record", value, variableName);
                    }
                }
            }
        }
    }

    /**
     * check if we have input.
     *
     * @param inputNamesToRefer inputs to check if filled
     * @return {@code true} if prerequisite should be skipped, {@code false} otherwise
     */
    private boolean checkInput(List<String> inputNamesToRefer) {
        Map<String, String> params = miaContext.getFlowData().getParameters();
        if (inputNamesToRefer != null) {
            for (String inputName : inputNamesToRefer) {
                if (params.containsKey(inputName)
                        && !Strings.isNullOrEmpty(params.get(inputName))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private CommandResponse executeCommand(Command command) {
        final String type = command.getType();
        final CommandType commandType = CommandType.valueOf(type);
        CommandResponse.CommandResponseType responseType = getResponseType(commandType);
        fillFlowDataWithEnvVars(command.getSystem(), responseType);
        CommandResponse commandResponse;
        if (CommandType.isTestData(commandType)) {
            metricsService.testDataExecutionWasStarted();
        }
        switch (commandType) {
            case SSH:
                commandResponse = sshService.executeSingleCommand(command);
                break;
            case SSH_CheckFileOnServer:
                commandResponse = sshService.executeCommandAndCheckFileOnServer(command);
                break;
            case SSH_TransferFile:
                commandResponse = sshService.executeCommandAndTransferFileOnServer(command);
                break;
            case SSH_GenerationFile:
                commandResponse = sshService.executeCommandAndGenerateFile(command);
                break;
            case SQL:
                commandResponse = sqlService.executeCommand(command.getToExecute(), command.getSystem())
                        .stream().findFirst().orElse(new CommandResponse());
                break;
            case SSH_UploadFileAndDownloadResult:
                commandResponse = sshService.uploadFilesAndDownloadResults(command);
                break;
            case SSH_DownloadFiles:
                commandResponse = sshService.executeCommandAndDownloadFilesFromServer(command);
                break;
            case SSH_GenerateEvent:
                commandResponse = sshService.generateEventFilesAndExecuteCommand(command);
                break;
            case SSH_UploadFile:
                commandResponse = sshService.executeCommandAndUploadFile(command);
                break;
            case REST:
                commandResponse = restService.sendRestRequest(command);
                break;
            case SOAP:
                commandResponse = soapService.sendSoapRequest(command);
                break;
            case SOAP_FROM_TEST_DATA:
                commandResponse = testDataService.soap(command);
                break;
            case REST_FROM_TEST_DATA:
                commandResponse = testDataService.rest(command);
                break;
            case VALIDATE_TEST_DATA:
                commandResponse = testDataService.validate(command);
                break;
            case EVENT_TEST_DATA:
                commandResponse = testDataService.event(command);
                break;
            case SSH_FROM_TEST_DATA:
                commandResponse = testDataService.ssh(command);
                break;
            case SQL_FROM_TEST_DATA:
                commandResponse = testDataService.sql(command);
                break;
            default:
                throw new UnsupportedCommandException(type);
        }
        if (commandResponse != null) {
            commandResponse.setType(responseType);
            addPrefixToDownloadLinks(commandResponse);
        }
        getVariablesFromLogs(commandType, commandResponse, command);
        return commandResponse;
    }

    /**
     * Execute prerequisites.
     *
     * @param prerequisites List of {@code Prerequisite}
     * @param command       command
     * @return List of {@code CommandResponse}
     */
    private List<CommandResponse> executePrerequisites(List<Prerequisite> prerequisites, Command command) {
        //FlowData flowData = contextRepository.getContext();
        List<CommandResponse> responses = new ArrayList<>();
        List<String> sshPrerequisitesToExecute = new ArrayList<>();
        for (Prerequisite prerequisite : prerequisites) {
            if (skipPrerequisite(prerequisite, command)) {
                log.debug("Prerequisite " + prerequisite + " has been skipped");
                continue;
            }
            log.debug("Execute prerequisite: " + prerequisite);
            String type = prerequisite.getType();
            if (SQL.toString().equals(type)) {
                final List<CommandResponse> prerequisiteResponses = sqlService.executeCommand(prerequisite.getValue(),
                        prerequisite.getSystem());
                responses.addAll(prerequisiteResponses);
                saveSqlPrerequisiteResponseToFlowData(prerequisite, prerequisiteResponses);
            } else if (CommandType.SSH.toString().equals(type)) {
                sshPrerequisitesToExecute.add(prerequisite.getValue());
            } else if (CommandType.COMMAND_ON_LOCALHOST.equals(type)) {
                prerequisite.getValues().forEach(c -> executeCommandOnLocalHost(c));
            } else {
                throw new PrerequisiteTypeUnsupportedException(type);
            }
        }
        sshPrerequisitesToExecute.forEach(prerequisite -> {
            String currentCommandValue = command.getToExecute();
            command.setToExecute(prerequisite
                    + miaContext.getCommonConfiguration().getCommandShellSeparator() + currentCommandValue);
        });
        return responses;
    }

    private boolean filterQueryType(CommandResponse r) {
        final String query = r.getSqlResponse().getQuery().toLowerCase();
        return Constants.sqlOperators.stream().anyMatch(query::startsWith);
    }

    /**
     * Get actual state of switchers.
     *
     * @param switchersListFromBe switchers BE
     * @param switchersListFromFe switchers FE
     */
    public void getActualStateOfSwitchers(List<Switcher> switchersListFromBe,
                                          List<Switcher> switchersListFromFe) {
        log.info("Actualization of switchers state");
        if (switchersListFromBe != null && switchersListFromFe != null) {
            List<Switcher> finalSwitchersListFromFe = new ArrayList<>(switchersListFromFe);
            switchersListFromBe.forEach(sBe ->
                    sBe.setValue(finalSwitchersListFromFe
                            .stream()
                            .filter(sFe -> sFe.getName().equals(sBe.getName()))
                            .findAny()
                            .orElse(sBe)
                            .isValue()));
        }
    }

    private void getVariablesFromLogs(CommandType commandType, CommandResponse commandResponse, Command command) {
        switch (commandType) {
            case EVENT_TEST_DATA:
            case SQL_FROM_TEST_DATA:
            case VALIDATE_TEST_DATA:
            case SSH_FROM_TEST_DATA:
            case REST_FROM_TEST_DATA:
            case SOAP_FROM_TEST_DATA:
            case SQL:
                break;
            default:
                if (commandResponse != null && commandResponse.getCommandOutputs() != null
                        && command.getVariablesToExtractFromLog() != null) {
                    saveVariableFromLog(commandResponse, command.getVariablesToExtractFromLog());
                }
                break;
        }
    }

    private TableMarkerResult processTableWithTableMarker(TableMarker tableMarker, SqlResponse table) {
        TableMarkerResult tableMarkerResult = new TableMarkerResult();
        if (tableMarker.getExpectedResultForQuery() != null) {
            final List<List<String>> processedData = new ArrayList<>();
            List<String> expectedResults = new ArrayList<>();
            List<String> actualResults = new ArrayList<>();
            List<String> processedColumns = new ArrayList<>();
            processedColumns.add(" ");
            expectedResults.add("ER");
            actualResults.add("AR");
            for (String column : table.getData().getColumns()) {
                String columnValue;
                if (table.getRecords() > 0) {
                    columnValue = table.getData().getData().get(0)
                            .get(table.getData().getColumns().indexOf(column));
                } else {
                    columnValue = "Not Found";
                }
                for (Map.Entry<String, String> entry : tableMarker.getExpectedResultForQuery().entrySet()) {
                    if (column.equalsIgnoreCase(entry.getKey())) {
                        try {
                            if (!Utils.checkCondition(entry.getValue(), columnValue)) {
                                tableMarkerResult.addColumnStatus(column, Statuses.FAIL, columnValue, entry.getValue());
                            } else {
                                tableMarkerResult.addColumnStatus(column, Statuses.SUCCESS, columnValue,
                                        entry.getValue());
                            }
                            expectedResults.add(entry.getValue());
                            processedColumns.add(column);
                        } catch (PatternSyntaxException pse) {
                            throw new PatternCompileException("Problem in evaluating Validation Table "
                                    + "Marker. Exception : " + pse.getMessage());
                        }
                    }
                }
                if (!processedColumns.contains(column)) {
                    expectedResults.add("---");
                    processedColumns.add(column);
                }
                actualResults.add(columnValue);
            }
            processedData.add(expectedResults);
            processedData.add(actualResults);
            table.getData().setColumns(processedColumns);
            table.getData().setData(processedData);
            sqlService.saveSqlTableToFile(Collections.singletonList(table));
        }
        if (tableMarker.getTableRowCount() != null) {
            if (!Utils.checkCondition(tableMarker.getTableRowCount(), String.valueOf(table.getRecords()))) {
                tableMarkerResult.setTableRowCount(tableMarker.getTableRowCount(),
                        String.valueOf(table.getRecords()), Statuses.FAIL);
            } else {
                tableMarkerResult.setTableRowCount(tableMarker.getTableRowCount(),
                        String.valueOf(table.getRecords()), Statuses.SUCCESS);
            }
        }
        return tableMarkerResult;
    }

    /**
     * Save first data value from first "select" query if name of prerequisite is defined.
     *
     * @param prerequisite prerequisite
     * @param responses    responses
     */
    private void saveSqlPrerequisiteResponseToFlowData(Prerequisite prerequisite, List<CommandResponse> responses) {
        if (prerequisite.getName() != null) {
            Optional<CommandResponse> prerequisiteQuery = responses.stream().filter(this::filterQueryType).findFirst();
            if (prerequisiteQuery.isPresent()) {
                final SqlResponse sqlResponse = prerequisiteQuery.get().getSqlResponse();
                if (sqlResponse.getRecords() > 0) {
                    final String value = sqlResponse.getData().getData().get(0).get(0);
                    miaContext.getFlowData().addParameter(prerequisite.getName(), value);
                    log.info("Parameter {} saved as {} from query: {}", prerequisite.getName(), value,
                            sqlResponse.getQuery());
                } else if (sqlResponse.getDescription() != null) {
                    miaContext.getFlowData().addParameter(prerequisite.getName(), sqlResponse.getDescription());
                    log.info("Parameter {}  from query: {}", prerequisite.getName(),
                            sqlResponse.getQuery());
                } else {
                    throw new PrerequisiteNoRecordsAddedException(sqlResponse.getQuery());
                }
            } else {
                throw new PrerequisiteQueryNotValidException(Constants.sqlOperators);
            }
        }
    }

    /**
     * Save parameters in flowData if find it in log by regex.
     *
     * @param commandResponse commandResponse
     * @param variables       variables
     */
    private void saveVariableFromLog(CommandResponse commandResponse, HashMap<String, String> variables) {
        CommandOutput commandOutput = commandResponse.getCommandOutputs().get(0);
        try {
            commandOutput.contentFromFile().forEach(string -> variables.forEach((variableName, regex) -> {
                String value = Utils.getFirstGroupFromStringByRegexp(string, regex);
                if (value != null) {
                    miaContext.getFlowData().addParameter(variableName, value);
                    log.info("Parameter '{}' saved as '{}' from string: {}", value, variableName, string);
                }
            }));
        } catch (PatternSyntaxException pse) {
            throw new PatternCompileException("Problem in variableToExtractFromLog value. " + pse.getMessage());
        }
    }

    /**
     * Check if need to skip the prerequisite.
     *
     * @param prerequisite prerequisite
     * @param command      command
     * @return {@code true} if prerequisite should be skipped, {@code false} otherwise
     */
    private boolean skipPrerequisite(Prerequisite prerequisite, Command command) {
        final List<String> commandValuesToRefer = prerequisite.getReferToCommandValue();
        final List<String> inputNamesToRefer = prerequisite.getReferToInputName();
        boolean toSkip = false;
        if (commandValuesToRefer != null) {
            if (commandValuesToRefer.stream().anyMatch(commandValue ->
                    commandValue.equalsIgnoreCase(command.getToExecute()))) {
                if (inputNamesToRefer != null) {
                    toSkip = checkInput(inputNamesToRefer);
                }
            } else {
                toSkip = true;
            }
        } else if (inputNamesToRefer != null) {
            toSkip = checkInput(inputNamesToRefer);
        }
        return toSkip;
    }

    private void validateProcessStatus(ExecutionResponse executionResponse) {
        ProcessStatus processStatusFromMarkerResult = new ProcessStatus();
        try {
            processStatusFromMarkerResult.setStatus(executionResponse.getCommandResponse().getSqlResponse()
                    .getTableMarkerResult().getTableRowCount().getStatus());
            executionResponse.setProcessStatus(processStatusFromMarkerResult);
        } catch (NullPointerException e) {
            log.debug("cannot set process status from markerResult (NPE1), because [{}]", e.getMessage());
        }
        try {
            if (executionResponse.getCommandResponse().getErrors().size() > 0) {
                processStatusFromMarkerResult.setStatus(Statuses.FAIL);
                executionResponse.setProcessStatus(processStatusFromMarkerResult);
            }
        } catch (NullPointerException e) {
            log.debug("cannot set process status from markerResult (NPE2), because [{}]", e.getMessage());
        }
        try {
            if (executionResponse.getCommandResponse().getPostScriptExecutionReport().contains(Constants.ERROR)) {
                processStatusFromMarkerResult.setStatus(Statuses.FAIL);
                executionResponse.setProcessStatus(processStatusFromMarkerResult);
            }
        } catch (NullPointerException e) {
            log.debug("cannot set process status from markerResult (NPE3), because [{}]", e.getMessage());
        }
    }
}
