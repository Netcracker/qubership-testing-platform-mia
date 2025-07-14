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

package org.qubership.atp.mia.integration.ssh;

import static org.mockito.ArgumentMatchers.eq;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_NAME;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.mia.controllers.MiaConfigurationController;
import org.qubership.atp.mia.controllers.MiaExecutionController;
import org.qubership.atp.mia.controllers.api.dto.CommandDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessSettingsDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessShortDto;
import org.qubership.atp.mia.controllers.api.dto.TableMarkerDto;
import org.qubership.atp.mia.controllers.api.dto.ValidationDto;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.integration.utils.TestUtils;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

//@Disabled("Temporarily disabled for refactoring")
public class ExecuteSshTest extends BaseIntegrationTestConfiguration {

    @Autowired
    MiaExecutionController miaExecutionController;

    @Autowired
    MiaConfigurationController miaConfigurationController;

    public ThreadLocal<String> tempTableName = new ThreadLocal<>();

    @BeforeEach
    public void beforeExecuteSshTest() {
        int randomId = (int) (Math.random() * 1000);
        UUID envId = UUID.randomUUID();
        UUID systemId = UUID.randomUUID();
        System testSystem2 = System.builder()
                .id(systemId)
                .name(TEST_SYSTEM_NAME + randomId)
                .environmentId(envId)
                .connections(Arrays.asList())
                .build();
        Environment testEnvironment2 = Environment.builder()
                .projectId(projectId.get())
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + randomId)
                .systems(Arrays.asList(testSystem2))
                .build();
        Project testProject2 = Project.builder()
                .id(projectId.get())
                .name(DEFAULT_PROJECT_NAME + randomId)
                .environments(Arrays.asList(testEnvironment2.getId()))
                .build();
        Mockito.when(environmentsService.getEnvironmentsByProject(eq(projectId.get())))
                .thenReturn(Arrays.asList(testEnvironment2));
        Mockito.when(environmentsService.getEnvironmentsFull(eq(envId), eq(projectId.get()))).thenReturn(testEnvironment2);
        Mockito.when(environmentsService.getProjects()).thenReturn(Arrays.asList(testProject2));
        Mockito.when(environmentsService.getProject(eq(projectId.get()))).thenReturn(testProject2);
        miaContext.setContext(projectId.get(), null);
    }

    @BeforeEach
    public void createTempTable() {
        int randomNumber = (int) (Math.random() * 1000);
        tempTableName.set("TempTable_" + randomNumber);
        Server dbServer = TestUtils.preparePostgresServer(postgresJdbcUrl);
        String createTable = "DROP TABLE IF EXISTS " + tempTableName.get() + ";"
                + " CREATE table IF NOT EXISTS " + tempTableName.get() + "(id serial, name varchar)",
                insertTable = "INSERT INTO " + tempTableName.get() + " (name) VALUES ('some_name');";
        Assert.assertEquals(0, postgreSqlDriver.get().executeUpdate(dbServer, createTable));
        Assert.assertEquals(1, postgreSqlDriver.get().executeUpdate(dbServer, insertTable));
    }

    @AfterEach
    public void removeTempTable() {
        Server dbServer = TestUtils.preparePostgresServer(postgresJdbcUrl);
        String dropTable = "DROP TABLE IF EXISTS " + tempTableName.get() + ";";
        Assert.assertEquals(0, postgreSqlDriver.get().executeUpdate(dbServer, dropTable));
    }

    @AfterEach
    public void afterExecuteSshTest() {
        if(projectId != null) {
            File file = new File("PROJECT_FOLDER/"+projectId.get());
            FileUtils.deleteFolder(file, true);
        }
    }

    @Test
    public void executeSsh_commandSystemEmpty_AutoSetToDefaultSystem() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String currentStatement = "select * from " + tempTableName.get() + " where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), currentStatement, null);
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
    }

    @Test
    public void executeSsh_globalVariablesTest() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), null, null);
        processDto.getProcessSettings().getGlobalVariables().put("globalInput", ":defaultSystem");
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
        Assert.assertEquals(1, executionResponse.getGlobalVariables().size());
        Assert.assertEquals("Billing System", executionResponse.getGlobalVariables().get("globalInput"));
    }

    @Test
    public void executeSsh_ValidationTableMarker_checkSuccess() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String validation = "select * from " + tempTableName.get() + " where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), null, validation);
        TableMarkerDto tableMarkerDto = new TableMarkerDto();
        tableMarkerDto.setExpectedResultForQuery(new HashMap<String, String>() {{
            put("id", "1");
        }});
        processDto.getProcessSettings().getValidations().get(0).setTableMarker(tableMarkerDto);
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
        Assert.assertEquals(1, executionResponse.getValidations().size());
        Assert.assertEquals(Statuses.SUCCESS,
                executionResponse.getValidations().get(0).getTableMarkerResult().getColumnStatuses().get(0).getStatus());
    }

    @Test
    public void executeSsh_ValidationTableMarker_checkFailure() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String validation = "select * from " + tempTableName.get() + " where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), null, validation);
        TableMarkerDto tableMarkerDto = new TableMarkerDto();
        tableMarkerDto.setExpectedResultForQuery(new HashMap<String, String>() {{
            put("id", "2");
        }});
        processDto.getProcessSettings().getValidations().get(0).setTableMarker(tableMarkerDto);
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
        Assert.assertEquals(1, executionResponse.getValidations().size());
        Assert.assertEquals(Statuses.FAIL,
                executionResponse.getValidations().get(0).getTableMarkerResult().getColumnStatuses().get(0).getStatus());
    }

    @Test
    public void executeSsh_ValidationTableMarker_checkRowCountForSuccess() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String validation = "select * from " + tempTableName.get() + " where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), null, validation);
        TableMarkerDto tableMarkerDto = new TableMarkerDto();
        tableMarkerDto.setTableRowCount("1");
        processDto.getProcessSettings().getValidations().get(0).setTableMarker(tableMarkerDto);
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
        Assert.assertEquals(1, executionResponse.getValidations().size());
        Assert.assertEquals(Statuses.SUCCESS,
                executionResponse.getValidations().get(0).getTableMarkerResult().getTableRowCount().getStatus());
    }

    @Test
    public void executeSsh_ValidationTableMarker_checkRowCountForFailure() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String validation = "select * from " + tempTableName.get() + " where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), null, validation);
        TableMarkerDto tableMarkerDto = new TableMarkerDto();
        tableMarkerDto.setTableRowCount(">1");
        processDto.getProcessSettings().getValidations().get(0).setTableMarker(tableMarkerDto);
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
        Assert.assertEquals(1, executionResponse.getValidations().size());
        Assert.assertEquals(Statuses.FAIL,
                executionResponse.getValidations().get(0).getTableMarkerResult().getTableRowCount().getStatus());
    }

    @Test
    public void executeSsh_Validation_ExportVariables() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String validation = "select * from " + tempTableName.get() + " where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), null, validation);
        processDto.getProcessSettings().getValidations().get(0).setExportVariables(new HashMap<String, String>() {{
            put("var1", "name");
        }});
        processDto.getProcessSettings().getGlobalVariables().put("globalInput", ":var1");
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
        //Request Creation
        ExecutionRequest request = new ExecutionRequest();
        request.setSessionId(UUID.randomUUID());
        request.setProcess("SSH_PWD");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request.setFlowData(flowData);
        // Execution & Validation
        ExecutionResponse executionResponse =
                miaExecutionController.executeProcess(projectId.get(), "Test", request).getBody();
        Assert.assertNotNull(executionResponse);
        Assert.assertEquals(1, executionResponse.getValidations().size());
        Assert.assertEquals("some_name", executionResponse.getGlobalVariables().get("globalInput"));
    }

    private ProcessDto getProcess(UUID processId, String processName, List<UUID> inSections, String currentStatement,
                                  String validation) {
        ProcessSettingsDto processSettingsDto = new ProcessSettingsDto();
        if (currentStatement != null) {
            ValidationDto validationDto = new ValidationDto();
            validationDto.setName("mia_table");
            validationDto.setSystem("Billing System");
            validationDto.setValue(currentStatement);
            validationDto.setType("SQL");
            List<ValidationDto> currentStatements = new ArrayList<>();
            currentStatements.add(validationDto);
            processSettingsDto.setCurrentStatement(currentStatements);
        }
        Map<String, String> atpValues = new HashMap<>();
        atpValues.put("pwd", "pwd");
        CommandDto commandDto = new CommandDto();
        commandDto.setType("SSH");
        commandDto.setAtpValues(atpValues);
        processSettingsDto.setCommand(commandDto);
        if (validation != null) {
            ValidationDto validationDto = new ValidationDto();
            validationDto.setName("mia_table");
            validationDto.setSystem("Billing System");
            validationDto.setValue(validation);
            validationDto.setType("SQL");
            List<ValidationDto> validationDtos = new ArrayList<>();
            validationDtos.add(validationDto);
            processSettingsDto.setValidations(validationDtos);
        }
        ProcessDto processDto = new ProcessDto();
        processDto.setId(processId);
        processDto.setName(processName);
        processDto.setInSections(inSections);
        processDto.setProcessSettings(processSettingsDto);
        return processDto;
    }
}
