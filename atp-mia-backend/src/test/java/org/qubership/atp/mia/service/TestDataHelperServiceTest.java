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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.macros.MacrosRegistrator;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.testdata.TestDataWorkbook;
import org.qubership.atp.mia.model.impl.testdata.parsed.ValidatedParameters;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.repo.gridfs.GridFsRepository;
import org.qubership.atp.mia.repo.impl.ShellRepository;
import org.qubership.atp.mia.repo.impl.TestDataRepository;
import org.qubership.atp.mia.repo.impl.pool.ssh.SshSessionPool;
import org.qubership.atp.mia.service.execution.RestExecutionHelperService;
import org.qubership.atp.mia.service.execution.SoapExecutionHelperService;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.qubership.atp.mia.service.execution.TestDataService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.utils.ExcelParserHelper;
import org.qubership.atp.mia.utils.Utils;

@ExtendWith(SkipTestInJenkins.class)
public class TestDataHelperServiceTest extends ConfigTestBean {

    private static final String fileName = "Rating_Matrix.xlsx";
    private static final String origFilePath = "src/test/resources/testData/" + fileName;
    private static final String accountNumber = "12345";
    private ThreadLocal<GridFsRepository> gridFsRepository = new ThreadLocal<>();
    private ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();
    private ThreadLocal<SqlExecutionHelperService> sqlService = new ThreadLocal<>();
    private ThreadLocal<SoapExecutionHelperService> soapService = new ThreadLocal<>();
    private ThreadLocal<RestExecutionHelperService> restService = new ThreadLocal<>();
    private ThreadLocal<ShellRepository> shellRepository = new ThreadLocal<>();
    private ThreadLocal<TestDataRepository> testDataRepository = new ThreadLocal<>();
    private ThreadLocal<TestDataService> testDataService = new ThreadLocal<>();

    @BeforeEach
    public void before() throws IOException {
        gridFsRepository.set(mock(GridFsRepository.class));
        gridFsService.set(new GridFsService(gridFsRepository.get(), miaContext.get()));
        sqlService.set(mock(SqlExecutionHelperService.class));
        soapService.set(mock(SoapExecutionHelperService.class));
        restService.set(mock(RestExecutionHelperService.class));
        shellRepository.set(spy(new ShellRepository(miaContext.get(), new SshSessionPool("300", "30000",
                miaContext.get()), metricsService)));
        miaFileService.set(spy(new MiaFileService(gridFsService.get(), miaContext.get(), projectConfigurationService.get())));
        testDataRepository.set(new TestDataRepository(contextRepository,
                sqlService.get(),
                soapService.get(),
                restService.get(),
                shellRepository.get(),
                miaContext.get(),
                miaFileService.get()));
        testDataService.set(new TestDataService(shellRepository.get(), testDataRepository.get(), miaContext.get(),
                sqlService.get(), miaFileService.get()));
        miaContext.get().getFlowData().addParameter("accountNumber", accountNumber);
        miaContext.get().getFlowData().addParameter("EVENT_DTM", "2019/12/11-07-49-57.00");
        Path projectDir = miaContext.get().getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_PROJECT, null);
        File dest = miaContext.get().getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_UPLOAD,
                miaContext.get().getFlowData().getSessionId()).resolve(fileName).toFile();
        try {
            FileUtils.copyFile(Paths.get(origFilePath).toFile(), dest);
            FileUtils.copyFile(Paths.get("src/main/config/project/default/etalon_files/event_testData.dat").toFile(),
                    projectDir.resolve("event_testData.dat").toFile());
            FileUtils.copyFile(Paths.get("src/main/config/project/default/etalon_files/control_templ.dat").toFile(),
                    projectDir.resolve("control_templ.dat").toFile());
        } catch (IOException e) {
            //copied by other thread
        }
        TestDataWorkbook testDataWorkbook = new TestDataWorkbook(dest.toString());
        testDataWorkbook.setScenariosToExecute(Utils.listToSet(Arrays.asList("Kirov non-National", "Kirov National")));
        testDataWorkbook.setExecuteSql(true);
        miaContext.get().getFlowData().setTestDataWorkbook(testDataWorkbook);
    }

    @Test
    public void event() {
        new MacrosRegistrator().register();
        final ProcessSettings process = DeserializerConfigBaseTest.getEventTestData().getProcessSettings();
        CommandResponse commandResponse = new CommandResponse(new CommandOutput("", "", false, miaContext.get()));
        Mockito.doNothing().when(shellRepository.get()).uploadFileOnServer(any(Server.class), anyString(), any(File.class));
        Mockito.doReturn(commandResponse).when(shellRepository.get()).executeAndGetLog(any());
        CommandResponse response = testDataService.get().event(process.getCommand());
        assertEquals(4, response.getCommandOutputs().size());
        assertEquals("Event: \"78332111111\",\"1\",\"2019/12/11-07-49-57.00\",,\"0\",\"321\"",
                response.getCommandOutputs().get(1).contentFromFile().get(10));
        System.err.println(response.getCommandOutputs().get(response.getCommandOutputs().size() - 2).contentFromFile().get(11));
        assertThat(response.getCommandOutputs().get(response.getCommandOutputs().size() - 2).contentFromFile().get(11),
                MatchesPattern.matchesPattern("File: \"(?!centrex\\-)event_file_sub_[\\d_]+.dat\",\"event_file\",\"event_file_sub_type\",\"1\",,"
                        + ",,,"));
    }

    public DbTable prepareTable(int size, int errSize) {
        if (errSize > size) {
            errSize = size;
        }
        List<String> columns = Arrays.asList("SCENARIO", "PASSED", "FAILED");
        List<List<String>> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i < errSize) {
                data.add(Arrays.asList("Testname_error" + i, "0", "20"));
            } else {
                data.add(Arrays.asList("Testname_success" + i, "10", "0"));
            }
        }
        return new DbTable(columns, data);
    }

    @Test
    public void restFromTestData() {
        new MacrosRegistrator().register();
        final ProcessSettings process = DeserializerConfigBaseTest.getRestTestData().getProcessSettings();
        CommandResponse commandResponse = new CommandResponse(
                new CommandOutput("src/test/resources/testData/restResponse.log", "", false, miaContext.get()));
        Mockito.doReturn(commandResponse).when(restService.get()).sendRestRequest(any());
        CommandResponse response = testDataService.get().rest(process.getCommand());
        TestDataWorkbook testDataWorkbook = new TestDataWorkbook(response.getCommandOutputs().getFirst().getInternalPathToFile());
        testDataRepository.get().parseWorkbook(testDataWorkbook, false);
        assertEquals("{ \"This is rest response\"}",
                testDataWorkbook.getTestDataSheet().getScenarios().getFirst().getDescriptions().getFirst()
                        .getOtherParams().get("RESTEXECUTION"));
    }

    @Test
    public void runEvent_whenExcelVariableSet_fileShouldHaveVariableName() {
        new MacrosRegistrator().register();
        final String expectedFilename = "centrex";
        final ProcessSettings process = DeserializerConfigBaseTest.getEventTestData().getProcessSettings();
        process.getCommand().setNamesOfFilesForGeneration(
                Collections.singletonList(":EVENT_ATTR_8-event_file_sub_${Timestamp(YYYYMMdd_hhmmss_S)}"));
        CommandResponse commandResponse = new CommandResponse(new CommandOutput("", "", false, miaContext.get()));
        Mockito.doNothing().when(shellRepository.get()).uploadFileOnServer(any(Server.class), anyString(), any(File.class));
        Mockito.doReturn(commandResponse).when(shellRepository.get()).executeAndGetLog(any());
        CommandResponse response = testDataService.get().event(process.getCommand());
        assertTrue(response.getCommandOutputs().get(1).getLink().getName().contains(expectedFilename));
    }

    @Test
    public void soapFromTestData() {
        new MacrosRegistrator().register();
        final ProcessSettings process = DeserializerConfigBaseTest.getSoapTestData().getProcessSettings();
        CommandResponse commandResponse = new CommandResponse(
                new CommandOutput("src/test/resources/testData/soapResponse.log", "", false, miaContext.get()));
        Mockito.doReturn(commandResponse).when(soapService.get()).sendSoapRequest(any());
        CommandResponse response = testDataService.get().soap(process.getCommand());
        TestDataWorkbook testDataWorkbook =
                new TestDataWorkbook(response.getCommandOutputs().getFirst().getInternalPathToFile());
        testDataRepository.get().parseWorkbook(testDataWorkbook, false);
        assertEquals("<soapResponse>\n    SOAP RESPONSE\n</soapResponse>",
                testDataWorkbook.getTestDataSheet().getScenarios().getFirst().getDescriptions().getFirst()
                        .getOtherParams().get("SOAPEXECUTION"));
    }

    @Test
    public void sshFromTestData() {
        new MacrosRegistrator().register();
        final ProcessSettings process = DeserializerConfigBaseTest.getSshTestData().getProcessSettings();
        CommandResponse commandResponse = new CommandResponse(new CommandOutput("", "", false, miaContext.get()));
        commandResponse.getCommandOutputs().get(0).setContent(
                new LinkedList<>(Arrays.asList("text1", "text2", "text3")));
        final Command newCommand = new Command(process.getCommand());
        newCommand.setToExecute("{ echo text1; echo text2; echo text3; }");
        // Job failed when use executeAndGetLog(eq(newCommand), any(FlowData.class)) but locally fine
        Mockito.doReturn(commandResponse).when(shellRepository.get()).executeAndGetLog(any());
        CommandResponse response = testDataService.get().ssh(process.getCommand());
        TestDataWorkbook testDataWorkbook =
                new TestDataWorkbook(response.getCommandOutputs().getFirst().getInternalPathToFile());
        testDataRepository.get().parseWorkbook(testDataWorkbook, false);
        assertEquals("text1\ntext2\ntext3",
                testDataWorkbook.getTestDataSheet().getScenarios().getFirst().getDescriptions().getFirst()
                        .getOtherParams().get("SSHEXECUTION"));
        assertEquals("1\ntext2\ntext",
                testDataWorkbook.getTestDataSheet().getScenarios().getFirst().getDescriptions().getFirst()
                        .getOtherParams().get("COLUMN_EXTRACTED_FROM_LOG_1"));
        assertEquals("text1\ntext2",
                testDataWorkbook.getTestDataSheet().getScenarios().getFirst().getDescriptions().getFirst()
                        .getOtherParams().get("COLUMN_EXTRACTED_FROM_LOG_2"));
    }

    @Test
    public void validate() throws IOException {
        final DbTable table1 = new DbTable(Arrays.asList("VALIDATED_Usage_Total_Cost", "VALIDATED_source",
                    "VALIDATED_type_id", "VALIDATED_dtm", "VALIDATED_attr_1", "VALIDATED_attr_2", "VALIDATED_attr_3",
                    "VALIDATED_attr_4", "VALIDATED_attr_5", "VALIDATED_attr_6", "VALIDATED_Attr_7", "VALIDATED_attr_8",
                    "VALIDATED_attr_9", "VALIDATED_attr_10", "VALIDATED_attr_11"),
                    Arrays.asList(Arrays.asList(ExcelParserHelper.decimalFormat(1.22), "78332111111", "1",
                            "2019/12/11-07-49-57.00", "78332865512", "78332111111", "2019/12/11-07-49-57.00", "2",
                            "1", "5839139948", "7", "centrex", "OPTS-21", "ERROR: NO COLUMN RESULT", "")));
        final SqlResponse sqlResponse = new SqlResponse();
            sqlResponse.setData(table1);
            List<CommandResponse> responses = Arrays.asList(new CommandResponse(sqlResponse));
        when(sqlService.get().executeCommand(anyString(), eq(testSystem.get().getName()), any(HashMap.class),
                anyBoolean()))
                .thenReturn(responses);
        final Command command = new Command("name", "type", testSystem.get().getName(), listToSet(""));
        CommandResponse response = testDataService.get().validate(command);
        Mockito.verify(sqlService.get(), times(1)).executeCommand(eq("select event_cost_mny/100000 as "
                            + "VALIDATED_Usage_Total_Cost, event_source as VALIDATED_source, event_type_id as "
                            + "VALIDATED_type_id, to_char(event_dtm, 'YYYY/MM/DD-HH24-MI-SS') as VALIDATED_dtm, "
                            + "event_attr_1 as VALIDATED_attr_1, event_attr_2 as VALIDATED_attr_2, event_attr_3 as "
                            + "VALIDATED_attr_3, event_attr_4 as VALIDATED_attr_4, event_attr_5 as VALIDATED_attr_5, "
                            + "event_attr_6 as VALIDATED_attr_6, event_attr_7 as VALIDATED_attr_7, event_attr_8 as "
                            + "VALIDATED_attr_8, event_attr_9 as VALIDATED_attr_9, event_attr_10 as "
                            + "VALIDATED_attr_10, event_attr_11 as VALIDATED_attr_11 "
                            + "from costedevent where event_source"
                            + " =78332111111 and event_dtm = to_date('2019/12/11-07-49-57.00',"
                        + "'YYYY/MM/DD-HH24-MI-SS') and event_attr_1='78332865512'"), eq(testSystem.get().getName()),
                any(HashMap.class), anyBoolean());
            assertEquals(2, response.getSqlResponse().getRecords());
            assertEquals("1", response.getSqlResponse().getData().getData().get(0).get(1));
            assertEquals("17", response.getSqlResponse().getData().getData().get(0).get(2));
            assertEquals("0", response.getSqlResponse().getData().getData().get(1).get(1));
            assertEquals("13", response.getSqlResponse().getData().getData().get(1).get(2));
        LinkedList<ValidatedParameters> validateValue = testDataRepository.get().getTestDataWorkbook().getMainSheet()
                    .getScenarios().get(0).getDescriptions().get(0).getValidatedParams();
            ValidatedParameters vp = validateValue.stream().filter(v -> v.getKey().equalsIgnoreCase(
                    "VALIDATED_attr_10")).findFirst().get();
            assertEquals("ERROR: NO COLUMN RESULT", vp.getValue());
            assertEquals(ValidatedParameters.State.PASSED, vp.getState());
        validateValue = testDataRepository.get().getTestDataWorkbook().getMainSheet().getScenarios().get(0)
                    .getDescriptions().get(1).getValidatedParams();
            vp = validateValue.stream().filter(v -> v.getKey().equalsIgnoreCase("VALIDATED_attr_9")).findFirst().get();
            assertEquals("OPTS-21", vp.getValue());
            assertEquals(ValidatedParameters.State.SKIPPED, vp.getState());
            vp = validateValue.stream().filter(v -> v.getKey().equalsIgnoreCase("VALIDATED_attr_10")).findFirst().get();
            assertEquals("SKIPPED", vp.getValue());
            assertEquals(ValidatedParameters.State.SKIPPED, vp.getState());
            vp = validateValue.stream().filter(v -> v.getKey().equalsIgnoreCase("VALIDATED_attr_1")).findFirst().get();
            assertEquals("[AR : 78332865512]\n[ER : 78332817695]", vp.getValue());
            assertEquals(ValidatedParameters.State.FAILED, vp.getState());
            vp = validateValue.stream().filter(v -> v.getKey().equalsIgnoreCase("VALIDATED_attr_11")).findFirst().get();
            assertEquals("", vp.getValue());
            assertEquals(ValidatedParameters.State.PASSED, vp.getState());
    }

    @Test
    public void validate_whenFailNotAtFirstLine_expectWarningStatus() {
        DbTable dbTable = prepareTable(5, 0);
        dbTable.getData().add(Arrays.asList("ERRCASE1", "23", "244"));
        Statuses result = TestDataService.setValidationStatus(dbTable, 5);
        assertNotNull(result);
        assertEquals(Statuses.WARNING, result);
    }

    @Test
    public void validate_whenScenariosFail_expectFailStatus() {
        DbTable dbTable = prepareTable(5, 5);
        Statuses result = TestDataService.setValidationStatus(dbTable, 5);
        assertNotNull(result);
        assertEquals(Statuses.FAIL, result);
    }

    @Test
    public void validate_whenScenariosSuccessAndFail_expectWarningStatus() {
        DbTable dbTable = prepareTable(5, 2);
        Statuses result = TestDataService.setValidationStatus(dbTable, 5);
        assertNotNull(result);
        assertEquals(Statuses.WARNING, result);
    }

    @Test
    public void validate_whenScenariosSuccess_expectSuccessStatus() {
        DbTable dbTable = prepareTable(5, 0);
        Statuses result = TestDataService.setValidationStatus(dbTable, 5);
        assertNotNull(result);
        assertEquals(Statuses.SUCCESS, result);
    }
}
