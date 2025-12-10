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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.mia.exceptions.fileservice.UploadTestDataFailException;
import org.qubership.atp.mia.exceptions.testdata.MatrixCommandIncorrectParameterException;
import org.qubership.atp.mia.exceptions.testdata.MatrixExcelCloseFailException;
import org.qubership.atp.mia.exceptions.testdata.MatrixExcelWriteFailException;
import org.qubership.atp.mia.exceptions.testdata.MatrixIncorrectParameterException;
import org.qubership.atp.mia.exceptions.testdata.MatrixScenarioNotFoundException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.VariableFormat;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.generation.Template;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.testdata.TestDataWorkbook;
import org.qubership.atp.mia.model.impl.testdata.TestDataWorkbookStyles;
import org.qubership.atp.mia.model.impl.testdata.parsed.Scenario;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;
import org.qubership.atp.mia.repo.impl.ShellRepository;
import org.qubership.atp.mia.repo.impl.TestDataRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.utils.FileUtils;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestDataService {

    private final ShellRepository sshRepo;
    private final TestDataRepository testDataRepository;
    private final MiaContext miaContext;
    private final SqlExecutionHelperService sqlService;
    private final MiaFileService miaFileService;

    /**
     * Check rows of a DbTable after execution and set Status.
     *
     * @param table                  result table after validation
     * @param scenariosToExecuteSize amount of executed scenarios
     * @return status
     */
    public static Statuses setValidationStatus(DbTable table, int scenariosToExecuteSize) {
        boolean isThereSuccessTest = table.getData().stream()
                .anyMatch(row -> Integer.parseInt(row.get(1)) > 0);
        boolean isThereFailedTest = table.getData().stream()
                .anyMatch(row -> Integer.parseInt(row.get(2)) > 0);
        return scenariosToExecuteSize == table.getData().size()
                && isThereSuccessTest && !isThereFailedTest ? Statuses.SUCCESS
                : isThereSuccessTest && isThereFailedTest ? Statuses.WARNING
                : Statuses.FAIL;
    }

    /**
     * Upload file provided by FE to BE and validate it.
     *
     * @param file file
     * @return TestDataFile instance with file name and scenarios.
     */
    public TestDataWorkbook uploadTestDataFileAndValidate(MultipartFile file) {
        String excelFile = miaFileService.uploadFileOnBe(file, false);
        final TestDataWorkbook testDataWorkbook = new TestDataWorkbook(excelFile);
        testDataRepository.parseWorkbook(testDataWorkbook, true);
        if (testDataWorkbook.getMainSheet().getScenarios() == null
                || testDataWorkbook.getMainSheet().getScenarios().isEmpty()) {
            throw new UploadTestDataFailException();
        }
        return testDataWorkbook;
    }

    /**
     * Write workbook into file.
     *
     * @param newExcelFileName full path to new file
     */
    public void writeTestDataWorkbookToFile(String newExcelFileName) {
        log.info("Preparing workbook to save into {}", newExcelFileName);
        TestDataWorkbook testDataWorkbook = testDataRepository.getTestDataWorkbook();
        try (TestDataWorkbookStyles wb = new TestDataWorkbookStyles(new SXSSFWorkbook(100))) {
            testDataWorkbook.getMainSheet().write(wb);
            testDataWorkbook.getTestDataSheet().write(wb);
            testDataWorkbook.getQueriesSheet().write(wb);
            log.info("Saving Test Data workbook into {}", newExcelFileName);
            try (FileOutputStream out = new FileOutputStream(newExcelFileName)) {
                wb.getWorkbook().write(out);
            } catch (IOException e) {
                throw new MatrixExcelWriteFailException(newExcelFileName, e);
            }
        } catch (Exception e) {
            throw new MatrixExcelCloseFailException(newExcelFileName, e);
        }
    }

    /**
     * Event parameters in test data file.
     * (Event generation for Billing System Rate matrix from excel file)
     *
     * @param command command
     * @return commandResponse
     */
    @AtpJaegerLog()
    public CommandResponse event(Command command) {
        // Here we parse main and test sheet for the first time
        checkBeforeExecute();
        checkBeforeExecuteEvent(command);
        final LinkedList<CommandOutput> commandOutputs = new LinkedList<>();
        final Server server = sshRepo.getServer(command);
        FlowData flowData = miaContext.getFlowData();
        flowData.addParameters(server.getProperties());
        LinkedList<Template> eventFiles = new LinkedList<>();
        //Generate event files
        TestDataWorkbook testDataWorkbook = testDataRepository.getTestDataWorkbook();
        testDataWorkbook.getScenariosToExecute().forEach(sce -> {
            final Scenario scenarioMain = findScenarioByName(
                    testDataWorkbook.getMainSheet().getScenarios(),
                    sce, "Scenario '" + sce + "' not found on Main page");
            final Scenario scenarioSec =
                    findScenarioByName(testDataWorkbook.getTestDataSheet().getScenarios(), sce, null);
            if (command.getTestDataParams().isEventFileForEachScenario()) {
                final String ethalonFile = miaContext.evaluate(command.getTestDataParams().getEventFileTemplate());
                final String toGeneration = miaContext.evaluate(command.getNamesOfFilesForGeneration().get(0));
                eventFiles.add(new Template(miaContext, miaFileService, ethalonFile, toGeneration,
                        command.getFileExtension(), command.definedCharsetForGeneratedFile()));
            }
            testDataRepository.generateEventScenario(command, eventFiles, scenarioMain, scenarioSec);
        });
        //Remove last EVENT_LINE and upload event files.
        final String pathForUpload = miaContext.evaluate(command.getPathForUpload());
        eventFiles.forEach(template -> {
            final String paramInTemplate = command.getTestDataParams().getEventParameterInTemplate();
            final VariableFormat varFormat =
                    new VariableFormat(miaContext.getConfig().getCommonConfiguration().getVariableFormat());
            final String neededParamInTemplate = varFormat.getVariableAccordingFormat(paramInTemplate);
            template.replaceContent(neededParamInTemplate, "");
            template.writeContent();
            sshRepo.uploadFileOnServer(server, pathForUpload, template.getFile());
            commandOutputs.add(new CommandOutput(template.getFile().getPath(), null, false, miaContext));
        });
        //Prepare and upload control file
        final String controlFile = command.getTestDataParams().getControlFileTemplate();
        if (!Strings.isNullOrEmpty(controlFile) && !Strings.isNullOrEmpty(command.getSaveGeneratedFilesToParameter())) {
            flowData.addParameter(command.getSaveGeneratedFilesToParameter(),
                    "[" + eventFiles.stream().map(Template::getFileName).collect(Collectors.joining(", ")) + "]");
            final String toGeneration = command.getNamesOfFilesForGeneration().size() > 1
                    ? miaContext.evaluate(command.getNamesOfFilesForGeneration().get(1))
                    : Utils.getFileNameWithoutExtension(controlFile);
            final Template controlTemplate = new Template(miaContext, miaFileService, controlFile, toGeneration,
                    command.getFileExtension(), command.definedCharsetForGeneratedFile());
            controlTemplate.evaluateFile();
            flowData.removeParameter(command.getSaveGeneratedFilesToParameter());
            sshRepo.uploadFileOnServer(server, pathForUpload, controlTemplate.getFile());
            commandOutputs.add(new CommandOutput(controlTemplate.getFile().getPath(), null, false, miaContext));
        }
        writeTestDataWorkbookToFile(testDataWorkbook.getExcelFile());
        final String excelFilePath = testDataWorkbook.getExcelFile();
        commandOutputs.add(new CommandOutput(excelFilePath, null, false, miaContext));
        final CommandResponse commandResponse = sshRepo.executeAndGetLog(command);
        commandResponse.addCommandOutputs(commandOutputs);
        return commandResponse;
    }

    /**
     * Validate parameters in test data file.
     * (Rate matrix validation using validation data from excel file (which was used for event generation))
     *
     * @param command command
     * @return commandResponse
     */
    @AtpJaegerLog()
    public CommandResponse validate(Command command) {
        TestDataWorkbook testDataWorkbook = testDataRepository.getTestDataWorkbook();
        final CommandResponse commandResponse = new CommandResponse();
        checkBeforeExecute();
        final LinkedHashSet<String> scenariosToExecute = testDataWorkbook.getScenariosToExecute();
        final DbTable table = new DbTable(Arrays.asList("SCENARIO", "PASSED", "FAILED"), new ArrayList<>());
        SqlResponse sqlResponse = new SqlResponse();
        sqlResponse.setDescription("Result for validations from Test Data file");
        sqlResponse.setRecords(scenariosToExecute.size());
        sqlResponse.setTableName("");
        sqlResponse.setQuery("");
        final LinkedList<String> generatedFiles = new LinkedList<>();
        try {
            scenariosToExecute.forEach(sce -> {
                final Scenario scenarioMain = findScenarioByName(testDataWorkbook.getMainSheet().getScenarios(), sce,
                        "Scenario '" + sce + "' not found on Main page");
                final Scenario scenarioSec = findScenarioByName(testDataWorkbook.getTestDataSheet().getScenarios(),
                        sce, null);
                testDataRepository.validateScenario(scenarioMain, scenarioSec, command.getSystem());
                table.addData(Arrays.asList(sce,
                        String.valueOf(scenarioMain.getPassedValidatedNumber()),
                        String.valueOf(scenarioMain.getFailedValidatedNumber())));
                final String file = Utils.getFileNameWithTimestamp(testDataWorkbook.getExcelFile());
                writeTestDataWorkbookToFile(file);
                generatedFiles.add(file);
            });
        } catch (Exception e) {
            log.error(ErrorCodes.MIA_1700_MATRIX_VALIDATION_ERROR.getMessage(e.getMessage()));
            commandResponse.addError(e);
        } finally {
            getLastFileAndRemoveTemporaryFiles(generatedFiles, commandResponse, testDataWorkbook.getExcelFile());
            sqlResponse.setData(table);
            final TableMarkerResult tableMarkerResult = new TableMarkerResult();
            final Statuses rowsStatus = setValidationStatus(table, scenariosToExecute.size());
            tableMarkerResult.setTableRowCount(String.valueOf(scenariosToExecute.size()),
                    String.valueOf(table.getData().size()), rowsStatus);
            sqlResponse.setTableMarkerResult(tableMarkerResult);
            commandResponse.setSqlResponse(sqlResponse);
            sqlService.saveSqlTableToFile(Collections.singletonList(sqlResponse));
        }
        return commandResponse;
    }

    /**
     * Execute SQL in test data file.
     *
     * @param command command
     * @return commandResponse
     */
    @AtpJaegerLog()
    public CommandResponse sql(Command command) {
        return executeCommandBiConsumer((scenarioMain, scenarioSec) ->
                testDataRepository.sqlScenario(scenarioMain, scenarioSec, command));
    }

    /**
     * Execute SOAP in test data file.
     *
     * @param command command
     * @return commandResponse
     */
    @AtpJaegerLog()
    public CommandResponse ssh(Command command) {
        return executeCommandBiConsumer((scenarioMain, scenarioSec) ->
                testDataRepository.sshScenario(scenarioMain, scenarioSec, command));
    }

    /**
     * Execute SOAP in test data file.
     *
     * @param command command
     * @return commandResponse
     */
    @AtpJaegerLog()
    public CommandResponse soap(Command command) {
        return executeCommandBiConsumer((scenarioMain, scenarioSec) ->
                testDataRepository.soapScenario(scenarioMain, scenarioSec, command));
    }

    /**
     * Execute REST in test data file.
     *
     * @param command command
     * @return commandResponse
     */
    @AtpJaegerLog()
    public CommandResponse rest(Command command) {
        return executeCommandBiConsumer((scenarioMain, scenarioSec) ->
                testDataRepository.restScenario(scenarioMain, scenarioSec, command));
    }

    /**
     * Execute command (SSH, REST, SOAP) and return CommandResponse.
     *
     * @param executeCommand command which sholud be execute
     * @return CommandResponse
     */
    private CommandResponse executeCommandBiConsumer(BiConsumer<Scenario, Scenario> executeCommand) {
        final TestDataWorkbook testDataWorkbook = testDataRepository.getTestDataWorkbook();
        final CommandResponse commandResponse = CommandResponse.getCommandResponseWithFilledRequestInfo();
        checkBeforeExecute();
        final LinkedHashSet<String> scenariosToExecute = testDataWorkbook.getScenariosToExecute();
        final LinkedList<String> generatedFiles = new LinkedList<>();
        try {
            scenariosToExecute.forEach(sce -> {
                final Scenario scenarioMain = findScenarioByName(testDataWorkbook.getMainSheet().getScenarios(), sce,
                        "Scenario '" + sce + "' not found on Main page");
                final Scenario scenarioSec = findScenarioByName(testDataWorkbook.getTestDataSheet().getScenarios(),
                        sce, null);
                executeCommand.accept(scenarioMain, scenarioSec);
                final String file = Utils.getFileNameWithTimestamp(testDataWorkbook.getExcelFile());
                writeTestDataWorkbookToFile(file);
                generatedFiles.add(file);
            });
        } catch (Exception e) {
            log.error("Exception during command execution: {}", e.getMessage());
            commandResponse.addError(e);
        } finally {
            commandResponse.getConnectionInfo().put("timestampResponse", Utils.getTimestamp());
            getLastFileAndRemoveTemporaryFiles(generatedFiles, commandResponse, testDataWorkbook.getExcelFile());
        }
        return commandResponse;
    }

    /**
     * Get last file from generated files, copy it to initial file, remove all other temporary files.
     *
     * @param generatedFiles  list of all temporary files
     * @param commandResponse commandResponse to add last file as link.
     * @param initialPath     path to initial file
     */
    private void getLastFileAndRemoveTemporaryFiles(LinkedList<String> generatedFiles,
                                                    CommandResponse commandResponse,
                                                    String initialPath) {
        if (!generatedFiles.isEmpty()) {
            final String lastFile = generatedFiles.getLast();
            commandResponse.addCommandOutput(new CommandOutput(lastFile, null, false, miaContext));
            generatedFiles.removeLast();
            try {
                FileUtils.copyFile(Paths.get(lastFile), Paths.get(initialPath));
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            for (String fileToRemove : generatedFiles) {
                FileUtils.removeFile(fileToRemove);
            }
        }
    }

    /**
     * Check parameters before execute.
     */
    private void checkBeforeExecute() {
        TestDataWorkbook testDataWorkbook = testDataRepository.getTestDataWorkbook();
        if (testDataWorkbook == null) {
            throw new MatrixIncorrectParameterException("Test data");
        }
        if (testDataWorkbook.getMainSheet().getScenarios() == null) {
            throw new MatrixIncorrectParameterException("Scenarios");
        }
        if (testDataWorkbook.getScenariosToExecute() == null) {
            throw new MatrixIncorrectParameterException("Scenarios to validate");
        }
    }

    /**
     * Check parameters before execute generating events.
     *
     * @param command command
     */
    private void checkBeforeExecuteEvent(Command command) {
        if (command.getTestDataParams() == null) {
            throw new MatrixCommandIncorrectParameterException("Scenarios to validate");
        }
        if (Strings.isNullOrEmpty(command.getPathForUpload())) {
            throw new MatrixCommandIncorrectParameterException("pathForUpload");
        }
        if (command.getNamesOfFilesForGeneration() == null || command.getNamesOfFilesForGeneration().isEmpty()) {
            throw new MatrixCommandIncorrectParameterException("namesOfFilesForGeneration");
        }
        if (Strings.isNullOrEmpty(command.getTestDataParams().getEventFileTemplate())) {
            throw new MatrixCommandIncorrectParameterException("eventFileTemplate");
        }
        if (Strings.isNullOrEmpty(command.getTestDataParams().getEventTemplate())) {
            throw new MatrixCommandIncorrectParameterException("eventTemplate");
        }
        if (Strings.isNullOrEmpty(command.getTestDataParams().getEventParameterInTemplate())) {
            throw new MatrixCommandIncorrectParameterException("eventParameterInTemplate");
        }
    }

    /**
     * Finds scenario by name.
     *
     * @param scenarios    scenarios
     * @param scenarioName scenarioName
     * @return Scenario, NULL if scenario is not found and error not defined, otherwise throw error.
     */
    private Scenario findScenarioByName(LinkedList<Scenario> scenarios, String scenarioName, String error) {
        if (scenarios != null) {
            Optional<Scenario> scenario = scenarios.stream().filter(s -> s.getName().equals(scenarioName)).findFirst();
            if (!scenario.isPresent()) {
                if (Strings.isNullOrEmpty(error)) {
                    return null;
                } else {
                    throw new MatrixScenarioNotFoundException(error);
                }
            }
            return scenario.get();
        }
        return null;
    }
}
