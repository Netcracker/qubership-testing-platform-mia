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

package org.qubership.atp.mia.integration.sql;

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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.mia.controllers.MiaConfigurationController;
import org.qubership.atp.mia.controllers.MiaExecutionController;
import org.qubership.atp.mia.controllers.api.dto.CommandDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessSettingsDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessShortDto;
import org.qubership.atp.mia.controllers.api.dto.ValidationDto;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlExecuteFailException;
import org.qubership.atp.mia.exceptions.configuration.CurrentStatementListIsEmptyException;
import org.qubership.atp.mia.exceptions.configuration.ProcessOrCompoundNotFoundException;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ExecuteCurrentStatement extends BaseIntegrationTestConfiguration {

    @Autowired
    MiaExecutionController miaExecutionController;

    @Autowired
    MiaConfigurationController miaConfigurationController;    

    @BeforeEach
    public void beforeExecuteCurrentStatement() {
        int randomId = (int) (Math.random() * 1000);
        UUID envId = new UUID(0, randomId+1);
        UUID systemId = new UUID(0, randomId+2);
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

    @AfterEach
    public void afterExecuteCurrentStatement() {
        if(projectId.get() != null) {
            File file = new File("PROJECT_FOLDER/"+projectId.get());
            FileUtils.deleteFolder(file, true);
        }
    }

    @Test
    public void executeInValidCurrentStatement() {
        Assert.assertThrows(SqlExecuteFailException.class, () -> {
            List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
            Assert.assertEquals(24, processes.size());
            //Add a Temporary Process
            String processName = "SSH_PWD";
            String validation = "select * from InvalidTable";
            UUID sectionId = processes.stream()
                    .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                    .findFirst().get()
                    .getInSections().get(0);
            ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), validation);
            miaConfigurationController.addProcess(projectId.get(), processDto);
            processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
            Assert.assertEquals(25, processes.size());
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
            List<SqlResponse> sqlResponses = miaExecutionController.executeCurrentStatement(projectId.get(), "Test",
                    request).getBody();
            Assert.assertEquals(1, sqlResponses.size());
        });
    }

    @Test
    public void executeCurrentStatement_WhenListIsEmpty() {
        Assert.assertThrows(CurrentStatementListIsEmptyException.class, () -> {
            List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
            Assert.assertEquals(24, processes.size());
            //Add a Temporary Process
            String processName = "SSH_PWD";
            String validation = "select * from InvalidTable";
            UUID sectionId = processes.stream()
                    .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                    .findFirst().get()
                    .getInSections().get(0);
            ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), validation);
            List<ValidationDto> emptyCurrentStatements = new ArrayList<>();
            processDto.getProcessSettings().setCurrentStatement(emptyCurrentStatements);
            miaConfigurationController.addProcess(projectId.get(), processDto);
            processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
            Assert.assertEquals(25, processes.size());

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
            List<SqlResponse> sqlResponses = miaExecutionController.executeCurrentStatement(projectId.get(), "Test",
                    request).getBody();
            Assert.assertEquals(1, sqlResponses.size());
        });
    }

    @Test
    public void executeValidCurrentStatement() {
        List<ProcessShortDto> processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(24, processes.size());
        //Add a Temporary Process
        String processName = "SSH_PWD";
        String validation = "select * from mia_table mt where id = 1";
        UUID sectionId = processes.stream()
                .filter(processShortDto -> processShortDto.getName().equals("SSH_BG"))
                .findFirst().get()
                .getInSections().get(0);
        ProcessDto processDto = getProcess(UUID.randomUUID(), processName, Arrays.asList(sectionId), validation);
        miaConfigurationController.addProcess(projectId.get(), processDto);
        processes = miaConfigurationController.getProcesses(projectId.get()).getBody();
        Assert.assertEquals(25, processes.size());
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
        List<SqlResponse> sqlResponses =
                miaExecutionController.executeCurrentStatement(projectId.get(), "Test", request).getBody();
        Assert.assertEquals(1, sqlResponses.size());
    }

    @Test
    public void executeNonExistCurrentStatement() {
        Assert.assertThrows(ProcessOrCompoundNotFoundException.class, () -> {
            ExecutionRequest request = new ExecutionRequest();
            request.setSessionId(UUID.randomUUID());
            request.setProcess("SSH_PWD1");
            Map<String, String> parameters = new HashMap<>();
            parameters.put("accountNumber", "AC_422941_2");
            parameters.put("defaultSystem", "Billing System");
            parameters.put("environment", "Environment-1");
            parameters.put("exportGenevaDate", "true");
            parameters.put("workingDirectory", "/tmp/TA/");
            FlowData flowData = new FlowData();
            flowData.setParameters(parameters);
            request.setFlowData(flowData);
            List<SqlResponse> sqlResponses =
                    miaExecutionController.executeCurrentStatement(projectId.get(), "Test", request).getBody();
        });
    }

    private ProcessDto getProcess(UUID processId, String processName, List<UUID> inSections, String validation) {
        Map<String, String> atpValues = new HashMap<>();
        atpValues.put("pwd", "pwd");
        CommandDto commandDto = new CommandDto();
        commandDto.setType("SSH");
        commandDto.setAtpValues(atpValues);
        ValidationDto validationDto = new ValidationDto();
        validationDto.setName("mia_table");
        validationDto.setSystem("Billing System");
        validationDto.setValue(validation);
        validationDto.setType("SQL");
        List<ValidationDto> currentStatements = new ArrayList<>();
        currentStatements.add(validationDto);
        ProcessSettingsDto processSettingsDto = new ProcessSettingsDto();
        processSettingsDto.setCommand(commandDto);
        processSettingsDto.setCurrentStatement(currentStatements);
        ProcessDto processDto = new ProcessDto();
        processDto.setId(processId);
        processDto.setName(processName);
        processDto.setInSections(inSections);
        processDto.setProcessSettings(processSettingsDto);
        return processDto;
    }
}
