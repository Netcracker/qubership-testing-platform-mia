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

import static org.qubership.atp.mia.model.impl.testdata.MainSheet.MAIN_SHEET_NAME;
import static org.qubership.atp.mia.model.impl.testdata.QueriesSheet.QUERIES_SHEET_NAME;
import static org.qubership.atp.mia.model.impl.testdata.TestDataSheet.TEST_DATA_SHEET_NAME;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.xml.ws.Holder;

import org.qubership.atp.mia.exceptions.testdata.MatrixExcelParseFailException;
import org.qubership.atp.mia.exceptions.testdata.MatrixQuerySheetMissedDbTypeException;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.model.impl.executable.Soap;
import org.qubership.atp.mia.model.impl.generation.Template;
import org.qubership.atp.mia.model.impl.testdata.MainSheet;
import org.qubership.atp.mia.model.impl.testdata.MainSheetRecord;
import org.qubership.atp.mia.model.impl.testdata.QueriesSheet;
import org.qubership.atp.mia.model.impl.testdata.QueriesSheetRecord;
import org.qubership.atp.mia.model.impl.testdata.TestDataSheet;
import org.qubership.atp.mia.model.impl.testdata.TestDataWorkbook;
import org.qubership.atp.mia.model.impl.testdata.parsed.Description;
import org.qubership.atp.mia.model.impl.testdata.parsed.Query;
import org.qubership.atp.mia.model.impl.testdata.parsed.Scenario;
import org.qubership.atp.mia.model.impl.testdata.parsed.ValidateValue;
import org.qubership.atp.mia.model.impl.testdata.parsed.ValidatedParameters;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.execution.RestExecutionHelperService;
import org.qubership.atp.mia.service.execution.SoapExecutionHelperService;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.stereotype.Repository;

import com.poiji.bind.Poiji;
import com.poiji.option.PoijiOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TestDataRepository {

    private final ContextRepository contextRepository;
    private final SqlExecutionHelperService sqlService;
    private final SoapExecutionHelperService soapService;
    private final RestExecutionHelperService restService;
    private final ShellRepository shellRepo;
    private final MiaContext miaContext;
    private final MiaFileService miaFileService;

    /**
     * Get TestDataWorkbook.
     *
     * @return {@link TestDataWorkbook}
     */
    public TestDataWorkbook getTestDataWorkbook() {
        TestDataWorkbook testDataWorkbook = contextRepository.getContext().getTestDataWorkbook();
        if (testDataWorkbook != null && testDataWorkbook.getExcelFile() != null && !testDataWorkbook.isLoaded()) {
            parseWorkbook(testDataWorkbook, false);
        }
        return testDataWorkbook;
    }

    /**
     * Generate event for description.
     *
     * @param sceMain scenario on main sheet
     * @param descrM  description on main sheet
     * @param descrS  description on test data sheet
     */
    public void generateEventDescription(Command command, LinkedList<Template> templates, Scenario sceMain,
                                         Description descrM, Description descrS) {
        log.info("GENERATE EVENT: [scenario:{}],[description:{}]", sceMain.getName(), descrM.getName());
        String fileName = null;
        final Map<String, String> params = getParametersFromDescriptions(descrM, descrS);
        params.putAll(contextRepository.getContext().getParameters());
        final CommonConfiguration cConf = miaContext.getConfig().getCommonConfiguration();
        if (command.getTestDataParams().isEventFileForEachDescription() || templates.size() == 0) {
            final String ethalonFile = miaContext.evaluate(command.getTestDataParams().getEventFileTemplate(), params);
            final String toGeneration = miaContext.evaluate(command.getNamesOfFilesForGeneration().get(0), params);
            fileName = toGeneration;
            templates.add(new Template(miaContext, miaFileService, ethalonFile, toGeneration,
                    command.getFileExtension(), command.definedCharsetForGeneratedFile()));
        }
        descrM.addOtherParam(command.getTestDataParams().getColumnNameForGeneratedFileName(), fileName,
                getTestDataWorkbook());
        descrM.addOtherParam(command.getTestDataParams().getColumnNameForGeneratedFileExtension(),
                command.getFileExtension(), getTestDataWorkbook());
        templates.getLast().evaluateTemplate(command, cConf, params);
    }

    /**
     * Parse excel sheet.
     *
     * @param excelFile   excel file
     * @param forValidate limit 10 rows in case for validation
     * @return MainSheet
     */
    public MainSheet parseMainSheet(File excelFile, boolean forValidate) {
        PoijiOptions options = PoijiOptions.PoijiOptionsBuilder.settings()
                .sheetName(MAIN_SHEET_NAME)
                .preferNullOverDefault(true)
                .headerStart(13)
                .build();
        final MainSheet mainSheet = new MainSheet();
        mainSheet.setRecords(Poiji.fromExcel(excelFile, MainSheetRecord.class, options));
        final Holder<Scenario> scenario = new Holder<>();
        if (mainSheet.getRecords() != null) {
            final Set<String> allKeysForOtherKeysMain = new LinkedHashSet<>();
            mainSheet.getRecords().forEach(r -> {
                if (r.getOtherCells() != null) {
                    allKeysForOtherKeysMain.addAll(r.getOtherCells().keySet());
                }
            });
            mainSheet.getRecords().forEach(r -> {
                if (r.getScenario() != null) {
                    if (scenario.value != null) {
                        if (!scenario.value.getName().equals(r.getScenario())) {
                            mainSheet.addScenario(scenario.value);
                            scenario.value = new Scenario(r.getScenario());
                        }
                    } else {
                        scenario.value = new Scenario(r.getScenario());
                    }
                }
                if (scenario.value != null && !forValidate && r.getDescription() != null) {
                    scenario.value.addDescription(
                            new Description(r.getDescription(), r.getOtherCells(allKeysForOtherKeysMain), miaContext));
                }
            });
            if (scenario.value != null) {
                mainSheet.addScenario(scenario.value);
                scenario.value = null;
            }
            mainSheet.setRecords(null);
        }
        return mainSheet;
    }

    /**
     * Parse excel sheet.
     *
     * @param excelFile excel file
     * @return MainSheet
     */
    public QueriesSheet parseQueriesSheet(File excelFile) {
        PoijiOptions options = PoijiOptions.PoijiOptionsBuilder.settings()
                .sheetName(QUERIES_SHEET_NAME)
                .preferNullOverDefault(true)
                .build();
        final QueriesSheet queriesSheet = new QueriesSheet();
        queriesSheet.setRecords(Poiji.fromExcel(excelFile, QueriesSheetRecord.class, options));
        final Holder<Query> query = new Holder<>();
        if (queriesSheet.getRecords() != null) {
            queriesSheet.getRecords().forEach(r -> {
                if (r.getQuery() != null) {
                    final String queryName = miaContext.evaluate(r.getQuery());
                    if (r.getType() == null) {
                        throw new MatrixQuerySheetMissedDbTypeException(r.getQuery());
                    }
                    if (query.value != null) {
                        if (!query.value.getQuery().equals(r.getQuery())) {
                            queriesSheet.addQuery(query.value);
                            query.value = new Query(queryName, r.getType());
                        }
                    } else {
                        query.value = new Query(queryName, r.getType());
                    }
                }
                if (query.value != null && r.getToValidate() != null && r.getValidated() != null) {
                    query.value.addValidateValue(
                            miaContext.evaluate(r.getToValidate().trim()),
                            miaContext.evaluate(r.getValidated().trim()));
                }
            });
            if (query.value != null) {
                queriesSheet.addQuery(query.value);
                query.value = null;
            }
            queriesSheet.setRecords(null);
        }
        return queriesSheet;
    }

    /**
     * Parse excel sheet.
     *
     * @param excelFile excel file
     * @return MainSheet
     */
    public TestDataSheet parseTestDataSheet(File excelFile) {
        PoijiOptions options = PoijiOptions.PoijiOptionsBuilder.settings()
                .sheetName(TEST_DATA_SHEET_NAME)
                .preferNullOverDefault(true)
                .trimCellValue(true)
                .build();
        final TestDataSheet testDataSheet = new TestDataSheet();
        testDataSheet.setRecords(Poiji.fromExcel(excelFile, MainSheetRecord.class, options));
        final Holder<Scenario> scenario = new Holder<>();
        if (testDataSheet.getRecords() != null) {
            final Set<String> allKeysForOtherKeysSec = new LinkedHashSet<>();
            testDataSheet.getRecords().forEach(r -> {
                if (r.getOtherCells() != null) {
                    allKeysForOtherKeysSec.addAll(r.getOtherCells().keySet());
                }
            });
            testDataSheet.getRecords().forEach(r -> {
                if (r.getScenario() != null) {
                    if (scenario.value != null) {
                        if (!scenario.value.getName().equals(r.getScenario())) {
                            testDataSheet.addScenario(scenario.value);
                            scenario.value = new Scenario(r.getScenario());
                        }
                    } else {
                        scenario.value = new Scenario(r.getScenario());
                    }
                }
                if (scenario.value != null && r.getDescription() != null) {
                    scenario.value.addDescription(
                            new Description(r.getDescription(), false, r.getOtherCells(allKeysForOtherKeysSec),
                                    miaContext));
                }
            });
            if (scenario.value != null) {
                testDataSheet.addScenario(scenario.value);
                scenario.value = null;
            }
            testDataSheet.setRecords(null);
        }
        return testDataSheet;
    }

    /**
     * Generate event scenario.
     *
     * @param sceMain scenario on main sheet
     * @param sceSec  scenario on test data sheet
     */
    public void generateEventScenario(Command command, LinkedList<Template> templates, Scenario sceMain,
                                      Scenario sceSec) {
        sceMain.getDescriptions().forEach(descriptionMain -> {
            final Description descriptionSecond = sceSec == null
                    ? null
                    : sceSec.getDescriptions().stream()
                    .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst()
                    .orElse(null);
            generateEventDescription(command, templates, sceMain, descriptionMain, descriptionSecond);
        });
    }

    /**
     * Parse workbook. Parse only first sheet in case validation.
     *
     * @param forValidate is for validation
     */
    public void parseWorkbook(TestDataWorkbook testDataWorkbook, boolean forValidate) {
        File excelFile = miaFileService.getFile(testDataWorkbook.getExcelFile());
        try {
            testDataWorkbook.setMainSheet(parseMainSheet(excelFile, forValidate));
            if (!forValidate) {
                testDataWorkbook.setTestDataSheet(parseTestDataSheet(excelFile));
                testDataWorkbook.setQueriesSheet(parseQueriesSheet(excelFile));
            }
        } catch (Exception e) {
            throw new MatrixExcelParseFailException(excelFile.getName(), MAIN_SHEET_NAME, e);
        }
        testDataWorkbook.setExcelFile(excelFile.toString());
        testDataWorkbook.setLoaded(true);
    }

    /**
     * Validate whole scenario.
     *
     * @param sceMain scenario on main sheet
     * @param sceSec  scenario on test data sheet
     * @param system  system of environment
     */
    public void validateScenario(Scenario sceMain, Scenario sceSec, String system) {
        sceMain.getDescriptions().forEach(descriptionMain -> {
            final Description descriptionSecond = sceSec == null
                    ? null
                    : sceSec.getDescriptions().stream()
                    .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst()
                    .orElse(null);
            validateDescription(sceMain, descriptionMain, descriptionSecond, system);
        });
    }

    /**
     * Validate description.
     *
     * @param sceMain scenario on main sheet
     * @param descrM  description on main sheet
     * @param descrS  description on test data sheet
     * @param system  system of environment
     */
    public void validateDescription(Scenario sceMain,
                                    Description descrM,
                                    Description descrS,
                                    String system) {
        log.info("VALIDATE MATRIX: [scenario:{}],[description:{}],[columns:{}]", sceMain.getName(), descrM.getName(),
                descrM.getValidationParams().keySet());
        final Map<String, String> params = getParametersFromDescriptions(descrM, descrS);
        descrM.getValidationParams().forEach((validK, validV) -> {
            Optional<Query> queryOptional = getTestDataWorkbook().getQueriesSheet().getQueries()
                    .stream().filter(q -> q.getValidateValue()
                            .stream().anyMatch(v -> v.getValidateName().equals(validK)))
                    .findAny();
            if (!queryOptional.isPresent()) {
                descrM.addValidatedParam(validK, "ERROR: QUERY TO EXECUTE NOT FOUND");
                return;
            }
            Query query = queryOptional.get();
            ValidateValue validateValue =
                    query.getValidateValue().stream().filter(v -> v.getValidateName().equals(validK)).findFirst().get();
            //If query already executed then just get the value
            if (validateValue.getValue() != null) {
                log.debug("VALIDATE MATRIX: [scenario:{}],[description:{}] value for '{}' present: {}",
                        sceMain.getName(), descrM.getName(), validK, validateValue.getValue());
                validate(descrM, params, validV, validateValue);
                return;
            }
            //Execute query
            String queryToExecute = miaContext.evaluate(query.getQuery(), params);
            log.info("VALIDATE MATRIX:[scenario:{}],[description:{},[query:{}]", sceMain.getName(),
                    descrM.getName(), queryToExecute);
            try {
                query.updateResultsFromListCommandResponses(
                        sqlService.executeCommand(queryToExecute, system, params, false));
            } catch (Exception e) {
                query.updateResultsWithString(Utils.maxExcelString(e.getMessage()));
            }
            //Update description
            validateValue =
                    query.getValidateValue().stream().filter(v -> v.getValidateName().equals(validK)).findFirst().get();
            validate(descrM, params, validV, validateValue);
        });
        getTestDataWorkbook().getQueriesSheet().getQueries()
                .forEach(q -> q.getValidateValue().forEach(v -> v.setValue(null)));
    }

    private void validate(Description descrM, Map<String, String> params, String validV, ValidateValue validateValue) {
        if (validV.length() > 0) {
            validV = miaContext.evaluate(validV, params);
            descrM.updateValidatedParam(validateValue.getValidatedName(), validateValue.getValue(),
                    validateValue.getValue().equals(validV)
                            ? ValidatedParameters.State.PASSED
                            : ValidatedParameters.State.FAILED, validV);
        } else {
            if (validateValue.getValue().equals("ERROR: NO COLUMN RESULT")) {
                validateValue.setValue("SKIPPED");
            }
            descrM.updateValidatedParam(validateValue.getValidatedName(), validateValue.getValue(),
                    validateValue.getValue().length() > 0
                            ? ValidatedParameters.State.SKIPPED
                            : ValidatedParameters.State.PASSED, validV);
        }
    }

    /**
     * SQL in whole scenario.
     *
     * @param sceMain scenario on main sheet
     * @param sceSec  scenario on test data sheet
     * @param command command
     */
    public void sqlScenario(Scenario sceMain, Scenario sceSec, Command command) {
        //Execute SQL from Main sheet
        sceMain.getDescriptions().forEach(descriptionMain -> {
            final Description descriptionSecond = sceSec == null
                    ? null
                    : sceSec.getDescriptions().stream()
                    .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst()
                    .orElse(null);
            sqlDescriptionToExecute(sceMain.getName(), descriptionMain, descriptionSecond, command);
        });
        if (sceSec != null) {
            //Execute SQL from Test Data sheet
            sceSec.getDescriptions().forEach(descriptionMain -> {
                final Description descriptionSecond = sceMain.getDescriptions().stream()
                        .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst().orElse(null);
                sqlDescriptionToExecute(sceSec.getName(), descriptionMain, descriptionSecond, command);
            });
        }
    }

    /**
     * SQL description to execute.
     *
     * @param scenarioName Scenario name
     * @param descrM       description on main sheet
     * @param descrS       description on test data sheet
     * @param command      command
     */
    private void sqlDescriptionToExecute(String scenarioName,
                                         Description descrM,
                                         Description descrS,
                                         Command command) {
        parseListToExecuteWithConsumer(command, (columnToExecute) -> {
            log.info("SQL FROM MATRIX: [scenario:{}],[description:{}],[column:{}]",
                    scenarioName, descrM.getName(), command.getToExecute());
            if (descrM.getOtherParams() != null && descrM.getOtherParams().containsKey(columnToExecute)) {
                descrM.getOtherParams().put(columnToExecute,
                        executeSqlFromCell(descrM.getOtherParams().get(columnToExecute), command.getSystem(), descrM,
                                descrS));
            } else if (descrM.getEventParams() != null && descrM.getEventParams().containsKey(columnToExecute)) {
                descrM.getEventParams().put(columnToExecute,
                        executeSqlFromCell(descrM.getEventParams().get(columnToExecute), command.getSystem(), descrM,
                                descrS));
            } else if (descrM.getValidationParams() != null && descrM.getValidationParams()
                    .containsKey(columnToExecute)) {
                descrM.getValidationParams().put(columnToExecute,
                        executeSqlFromCell(descrM.getValidationParams().get(columnToExecute), command.getSystem(),
                                descrM, descrS));
            } else {
                log.debug("SQL FROM MATRIX: [scenario:{}],[description:{}] - column '{}' not found",
                        scenarioName, descrM.getName(), command.getToExecute());
            }
        });
    }

    /**
     * SSH in whole scenario.
     *
     * @param sceMain scenario on main sheet
     * @param sceSec  scenario on test data sheet
     * @param command command
     */
    public void sshScenario(Scenario sceMain, Scenario sceSec, Command command) {
        //Execute SSH from Main sheet
        sceMain.getDescriptions().forEach(descriptionMain -> {
            final Description descriptionSecond = sceSec == null
                    ? null
                    : sceSec.getDescriptions().stream()
                    .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst()
                    .orElse(null);
            sshDescriptionToExecute(sceMain.getName(), descriptionMain, descriptionSecond, command);
        });
        if (sceSec != null) {
            //Execute SSH from Test Data sheet
            sceSec.getDescriptions().forEach(descriptionMain -> {
                final Description descriptionSecond = sceMain.getDescriptions().stream()
                        .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst().orElse(null);
                sshDescriptionToExecute(sceSec.getName(), descriptionMain, descriptionSecond, command);
            });
        }
    }

    /**
     * SQL description to execute.
     *
     * @param scenarioName Scenario name
     * @param descrM       description on main sheet
     * @param descrS       description on test data sheet
     * @param command      command
     */
    private void sshDescriptionToExecute(String scenarioName,
                                         Description descrM,
                                         Description descrS,
                                         Command command) {
        parseListToExecuteWithConsumer(command, (columnToExecute) -> {
            final Command currCommand = new Command(command);
            log.info("SSH FROM MATRIX: [scenario:{}],[description:{}],[column:{}]",
                    scenarioName, descrM.getName(), command.getToExecute());
            if (descrM.getOtherParams() != null && descrM.getOtherParams().containsKey(columnToExecute)) {
                currCommand.setToExecute(descrM.getOtherParams().get(columnToExecute));
                descrM.getOtherParams().put(columnToExecute, executeSsh(currCommand, descrM, descrS));
            } else if (descrM.getEventParams() != null && descrM.getEventParams().containsKey(columnToExecute)) {
                currCommand.setToExecute(descrM.getEventParams().get(columnToExecute));
                descrM.getEventParams().put(columnToExecute, executeSsh(currCommand, descrM, descrS));
            } else if (descrM.getValidationParams() != null
                    && descrM.getValidationParams().containsKey(columnToExecute)) {
                currCommand.setToExecute(descrM.getValidationParams().get(columnToExecute));
                descrM.getValidationParams().put(columnToExecute, executeSsh(currCommand, descrM, descrS));
            } else {
                log.debug("SSH FROM MATRIX: [scenario:{}],[description:{}] - column '{}' not found",
                        scenarioName, descrM.getName(), command.getToExecute());
            }
        });
    }

    /**
     * Execute SSH from description.
     *
     * @param command command
     * @param descrM  description on main sheet
     * @param descrS  description on test data sheet
     * @return result of ssh execution
     */
    private String executeSsh(Command command, Description descrM, Description descrS) {
        return executeWithFunction("SSH", command, descrM, descrS, () -> shellRepo.executeAndGetLog(command));
    }

    /**
     * SOAP in whole scenario.
     *
     * @param sceMain scenario on main sheet
     * @param sceSec  scenario on test data sheet
     * @param command command
     */
    public void soapScenario(Scenario sceMain, Scenario sceSec, Command command) {
        //Execute SOAP from Main sheet
        sceMain.getDescriptions().forEach(descriptionMain -> {
            final Description descriptionSecond = sceSec == null
                    ? null
                    : sceSec.getDescriptions().stream()
                    .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst()
                    .orElse(null);
            soapDescriptionToExecute(sceMain.getName(), descriptionMain, descriptionSecond, command);
        });
        if (sceSec != null) {
            //Execute SOAP from Test Data sheet
            sceSec.getDescriptions().forEach(descriptionMain -> {
                final Description descriptionSecond = sceMain.getDescriptions().stream()
                        .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst().orElse(null);
                soapDescriptionToExecute(sceSec.getName(), descriptionMain, descriptionSecond, command);
            });
        }
    }

    /**
     * SQL description to execute.
     *
     * @param scenarioName Scenario name
     * @param descrM       description on main sheet
     * @param descrS       description on test data sheet
     * @param command      command
     */
    private void soapDescriptionToExecute(String scenarioName,
                                          Description descrM,
                                          Description descrS,
                                          Command command) {
        parseListToExecuteWithConsumer(command, (columnToExecute) -> {
            log.info("SOAP FROM MATRIX: [scenario:{}],[description:{}],[column:{}]",
                    scenarioName, descrM.getName(), command.getToExecute());
            final Command currCommand = new Command(command);
            currCommand.setSoap(new Soap(command.getSoap().getEndpoint()));
            if (descrM.getOtherParams() != null && descrM.getOtherParams().containsKey(columnToExecute)) {
                currCommand.getSoap().setRequestFile(descrM.getOtherParams().get(columnToExecute));
                descrM.getOtherParams().put(
                        columnToExecute, executeSoap(currCommand, descrM, descrS));
            } else if (descrM.getEventParams() != null && descrM.getEventParams().containsKey(columnToExecute)) {
                currCommand.getSoap().setRequestFile(descrM.getEventParams().get(columnToExecute));
                descrM.getEventParams().put(
                        columnToExecute, executeSoap(currCommand, descrM, descrS));
            } else if (descrM.getValidationParams() != null && descrM.getValidationParams()
                    .containsKey(columnToExecute)) {
                currCommand.getSoap().setRequestFile(descrM.getValidationParams().get(columnToExecute));
                descrM.getValidationParams().put(
                        columnToExecute, executeSoap(currCommand, descrM, descrS));
            } else {
                log.debug("SOAP FROM MATRIX: [scenario:{}],[description:{}] - column '{}' not found",
                        scenarioName, descrM.getName(), command.getToExecute());
            }
        });
    }

    /**
     * Execute SOAP from description.
     *
     * @param command command instance
     * @param descrM  description on main sheet
     * @param descrS  description on test data sheet
     * @return result of SOAP execution
     */
    private String executeSoap(Command command, Description descrM, Description descrS) {
        return executeWithFunction("SOAP", command, descrM, descrS, () -> soapService.sendSoapRequest(command));
    }

    /**
     * REST in whole scenario.
     *
     * @param sceMain scenario on main sheet
     * @param sceSec  scenario on test data sheet
     * @param command command
     */
    public void restScenario(Scenario sceMain, Scenario sceSec, Command command) {
        //Execute REST from Main sheet
        sceMain.getDescriptions().forEach(descriptionMain -> {
            final Description descriptionSecond = sceSec == null
                    ? null
                    : sceSec.getDescriptions().stream()
                    .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst()
                    .orElse(null);
            restDescriptionToExecute(sceMain.getName(), descriptionMain, descriptionSecond, command);
        });
        if (sceSec != null) {
            //Execute REST from Test Data sheet
            sceSec.getDescriptions().forEach(descriptionMain -> {
                final Description descriptionSecond = sceMain.getDescriptions().stream()
                        .filter(descr -> descr.getName().equals(descriptionMain.getName())).findFirst().orElse(null);
                restDescriptionToExecute(sceSec.getName(), descriptionMain, descriptionSecond, command);
            });
        }
    }

    /**
     * SQL description to execute.
     *
     * @param scenarioName Scenario name
     * @param descrM       description on main sheet
     * @param descrS       description on test data sheet
     * @param command      command
     */
    private void restDescriptionToExecute(String scenarioName,
                                          Description descrM,
                                          Description descrS,
                                          Command command) {
        parseListToExecuteWithConsumer(command, (columnToExecute) -> {
            log.info("REST FROM MATRIX: [scenario:{}],[description:{}],[column:{}]",
                    scenarioName, descrM.getName(), columnToExecute);
            final Command currCommand = new Command(command);
            currCommand.setRest(Rest.builder().build());
            if (descrM.getOtherParams() != null && descrM.getOtherParams().containsKey(columnToExecute)) {
                currCommand.getRest().setRestFile(descrM.getOtherParams().get(columnToExecute));
                descrM.getOtherParams().put(columnToExecute, executeRest(currCommand, descrM, descrS));
            } else if (descrM.getEventParams() != null && descrM.getEventParams().containsKey(columnToExecute)) {
                currCommand.getRest().setRestFile(descrM.getEventParams().get(columnToExecute));
                descrM.getEventParams().put(columnToExecute, executeRest(currCommand, descrM, descrS));
            } else if (descrM.getValidationParams() != null
                    && descrM.getValidationParams().containsKey(columnToExecute)) {
                currCommand.getRest().setRestFile(descrM.getValidationParams().get(columnToExecute));
                descrM.getValidationParams().put(columnToExecute, executeRest(currCommand, descrM, descrS));
            } else {
                log.debug("REST FROM MATRIX: [scenario:{}],[description:{}] - column '{}' not found",
                        scenarioName, descrM.getName(), columnToExecute);
            }
        });
    }

    /**
     * Execute sql from description.
     *
     * @param command command instance
     * @param descrM  description on main sheet
     * @param descrS  description on test data sheet
     * @return result of sql execution
     */
    private String executeRest(Command command, Description descrM, Description descrS) {
        return executeWithFunction("REST", command, descrM, descrS, () -> restService.sendRestRequest(command));
    }

    /**
     * Get parameters from descriptions.
     *
     * @param descrM description on main sheet
     * @param descrS description on test data sheet
     * @return Map of parameters for description
     */
    public Map<String, String> getParametersFromDescriptions(Description descrM, Description descrS) {
        Map<String, String> allParams = descrM.getAllParams();
        if (descrS != null) {
            allParams.putAll(descrS.getAllParams());
        }
        return allParams;
    }

    /**
     * Execute SQL from cell if it is needed.
     *
     * @param value  sql query
     * @param system system name
     * @param descrM description from main page
     * @param descrS description from second page
     * @return value of evaluated by SQL
     */
    private String executeSqlFromCell(String value, String system, Description descrM,
                                      Description descrS) {
        return executeSqlFromCell(value, system, getParametersFromDescriptions(descrM, descrS));
    }

    /**
     * Execute SQL from cell if it is needed.
     *
     * @param value  sql query
     * @param system system name
     * @param params all parameters
     * @return value of evaluated by SQL
     */
    private String executeSqlFromCell(String value, String system, Map<String, String> params) {
        if (value != null && value.toLowerCase().endsWith(".sql")) {
            try {
                final List<CommandResponse> result = sqlService.executeCommand(value, system, params, false);
                if (result.size() > 0 && result.get(0).getSqlResponse() != null
                        && result.get(0).getSqlResponse().getData() != null
                        && result.get(0).getSqlResponse().getData().getData() != null
                        && result.get(0).getSqlResponse().getData().getData().size() > 0
                        && result.get(0).getSqlResponse().getData().getData().get(0) != null
                        && result.get(0).getSqlResponse().getData().getData().get(0).size() > 0) {
                    value = result.get(0).getSqlResponse().getData().getData().get(0).get(0);
                } else {
                    value = "SQL ERROR: can't parse result of execution";
                }
            } catch (Exception e) {
                value = Utils.maxExcelString("SQL ERROR: " + e.getMessage());
            }
        }
        return value;
    }

    /**
     * Parse response of execution and add new column if need parse result.
     *
     * @param command     command
     * @param response    response of execution
     * @param description description on which add column
     * @return result of execution as string
     */
    private String addColumnWithParsingFromLog(Command command, CommandResponse response, Description description) {
        final String stringResponse = String.join("\n", response.getCommandOutputs().get(0).contentFromFile());
        if (stringResponse != null && command.getVariablesToExtractFromLog() != null) {
            command.getVariablesToExtractFromLog().forEach((columnName, regex) -> {
                String value = Utils.getFirstGroupFromStringByRegexp(stringResponse, regex);
                if (value != null) {
                    description.addOtherParam(columnName, value, getTestDataWorkbook());
                }
            });
        }
        return stringResponse;
    }

    /**
     * Execute REST, SOAP, SSH: take parameters from description and execute.
     *
     * @param type      type of command
     * @param command   command
     * @param descrM    main description
     * @param descrS    second description
     * @param execution what should be execute
     * @return result of execution
     */
    private String executeWithFunction(String type, Command command, Description descrM, Description descrS,
                                       Supplier<CommandResponse> execution) {
        contextRepository.getContext().addParameters(getParametersFromDescriptions(descrM, descrS));
        try {
            return addColumnWithParsingFromLog(command, execution.get(), descrM);
        } catch (Exception e) {
            return type + " ERROR: " + e.getMessage();
        }
    }

    /**
     * Parse list to execute and accept execution for each member.
     *
     * @param command           command
     * @param executionConsumer consumer to execute
     */
    private void parseListToExecuteWithConsumer(Command command, Consumer<String> executionConsumer) {
        String[] columns = command.getToExecute().toUpperCase().split(",");
        if (command.getToExecute().matches("\\[[\\w, ]+\\]")) {
            columns = command.getToExecute().toUpperCase().substring(1, command.getToExecute().length() - 1)
                    .split(", ");
        } else {
            columns[0] = command.getToExecute().toUpperCase();
        }
        for (String columnToExecute : columns) {
            executionConsumer.accept(columnToExecute.trim());
        }
    }
}
