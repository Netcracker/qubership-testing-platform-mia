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

package org.qubership.atp.mia.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_NAME;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;

//@Disabled("Temporarily disabled for refactoring")
public class MiaServiceControllerTest extends BaseIntegrationTestConfiguration {

    @Autowired
    MiaServiceController miaServiceController;

    private UUID systemId;

    @BeforeEach
    public void beforeMiaServiceControllerTest() {
        int randomId = (int) (Math.random() * 1000);
        UUID envId = UUID.randomUUID();
        systemId = UUID.randomUUID();
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
        miaContext.getFlowData().setEnvironment(testEnvironment2);
        miaContext.getFlowData().setProjectName(DEFAULT_PROJECT_NAME + randomId);
    }

    @AfterEach
    public void afterMiaServiceControllerTest() {
        if(projectId.get() != null) {
            File file = new File("PROJECT_FOLDER/"+projectId.get());
            FileUtils.deleteFolder(file, true);
        }
    }

    @Test
    public void dbSize_test() {
        Assert.assertNotNull(miaServiceController.dbSize(projectId.get()).getBody());
    }

    @Test
    public void getTimeShifting_Tetst() {
        Assert.assertFalse(miaServiceController.getTimeShifting(projectId.get(), systemId.toString()).getBody());
    }

    @Test
    public void updateTimeShifting_Test() {
        // Incomplete Test.
        // miaContext.getFlowData.getEnvironment is coming as null. Fix it and continue to finish.
        Assert.assertThrows(NullPointerException.class, () -> {
            miaServiceController.updateTimeShifting(projectId.get(), UUID.randomUUID(), true).getBody();
        });
    }
}
