/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.mia.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.repo.driver.PostgreSqlDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

@Disabled("Temporarily disabled for refactoring")
public class MiaExecutionControllerTest extends BaseIntegrationTestConfiguration {

    @Autowired
    MiaExecutionController miaExecutionController;
    @SpyBean
    PostgreSqlDriver postgreSqlDriverSpy;

    int randomId;

    @BeforeEach
    public void beforeMiaExecutionControllerTest() {
        randomId = (int) (ThreadLocalRandom.current().nextDouble() * 1000);
        UUID envId = UUID.randomUUID();
        UUID systemId = UUID.randomUUID();
        System testSystem2 = System.builder()
                .id(systemId)
                .name(TEST_SYSTEM_NAME + randomId)
                .environmentId(envId)
                .connections(List.of())
                .build();
        Environment testEnvironment2 = Environment.builder()
                .projectId(projectId.get())
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + randomId)
                .systems(List.of(testSystem2))
                .build();
        Project testProject2 = Project.builder()
                .id(projectId.get())
                .name(DEFAULT_PROJECT_NAME + randomId)
                .environments(List.of(testEnvironment2.getId()))
                .build();
        Mockito.when(environmentsService.getEnvironmentsByProject(eq(projectId.get())))
                .thenReturn(List.of(testEnvironment2));
        Mockito.when(environmentsService.getEnvironmentsFull(eq(envId), eq(projectId.get()))).thenReturn(testEnvironment2);
        Mockito.when(environmentsService.getProjects()).thenReturn(List.of(testProject2));
        Mockito.when(environmentsService.getProject(eq(projectId.get()))).thenReturn(testProject2);
        miaContext.setContext(projectId.get(), null);
    }

    @Test
    public void getNextBillDate() {
        String expectedValue = "2024-06-19";
        doReturn(expectedValue).when(postgreSqlDriverSpy).executeQueryAndGetFirstValue(any(), any());
        ExecutionRequest request1 = new ExecutionRequest();
        request1.setSessionId(UUID.randomUUID());
        Map<String, String> parameters = new HashMap<>();
        parameters.put("accountNumber", "AC_422941_2");
        parameters.put("defaultSystem", "Billing System");
        parameters.put("environment", "Environment-1");
        parameters.put("exportGenevaDate", "true");
        parameters.put("workingDirectory", "/tmp/TA/");
        FlowData flowData = new FlowData();
        flowData.setParameters(parameters);
        request1.setFlowData(flowData);
        Assertions.assertEquals(expectedValue, miaExecutionController.getNextBillDate(projectId.get(), "Test", request1));
    }
}

