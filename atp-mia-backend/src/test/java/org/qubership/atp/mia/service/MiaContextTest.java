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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_NAME;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.System;

@ExtendWith(SkipTestInJenkins.class)
public class MiaContextTest extends ConfigTestBean {

    @Test
    public void getContext_returnsCorrectProjectName() {
        Assert.assertTrue(miaContext.get().getFlowData().getProjectName().equals(DEFAULT_PROJECT_NAME));
    }

    @Test
    public void setContextRequestHeader_getContext_returnsCorrectProjectName() {
        UUID projectId = new UUID(0, 220);
        UUID envId = new UUID(0, 221);
        UUID systemId = new UUID(0, 222);
        System testSystem2 = System.builder()
                .id(systemId)
                .name(TEST_SYSTEM_NAME + "2")
                .environmentId(envId)
                .connections(Arrays.asList())
                .build();
        Environment testEnvironment2 = Environment.builder()
                .projectId(projectId)
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + "2")
                .systems(Arrays.asList(testSystem2))
                .build();
        Project testProject2 = Project.builder()
                .id(projectId)
                .name(DEFAULT_PROJECT_NAME + "2")
                .environments(Arrays.asList(testEnvironment2.getId()))
                .build();
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Arrays.asList(testEnvironment2));
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Arrays.asList(testEnvironment2));
        when(environmentsService.get().getEnvironmentsFull(eq(envId), eq(projectId))).thenReturn(testEnvironment2);
        when(environmentsService.get().getProjects()).thenReturn(Arrays.asList(testProject2));
        when(environmentsService.get().getProject(eq(projectId))).thenReturn(testProject2);
        miaContext.get().setContext(projectId, null);
        Assert.assertEquals(miaContext.get().getFlowData().getProjectName(), testProject2.getName());
    }

    @Test
    public void setContextRequestParameter_getContext_returnsCorrectProjectName() {
        UUID projectId = new UUID(0, 331);
        UUID envId = new UUID(0, 332);
        UUID systemId = new UUID(0, 333);
        UUID sessionId = new UUID(0, 555);
        System testSystem3 = System.builder()
                .id(systemId)
                .name(TEST_SYSTEM_NAME + "3")
                .environmentId(envId)
                .connections(Arrays.asList())
                .build();
        Environment testEnvironment3 = Environment.builder()
                .projectId(projectId)
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + "3")
                .systems(Arrays.asList(testSystem3))
                .build();
        Project testProject3 = Project.builder()
                .id(projectId)
                .name(DEFAULT_PROJECT_NAME + "3")
                .environments(Arrays.asList(testEnvironment3.getId()))
                .build();
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Arrays.asList(testEnvironment3));
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Arrays.asList(testEnvironment3));
        when(environmentsService.get().getEnvironmentsFull(eq(envId), eq(projectId))).thenReturn(testEnvironment3);
        when(environmentsService.get().getProjects()).thenReturn(Arrays.asList(testProject3));
        when(environmentsService.get().getProject(eq(projectId))).thenReturn(testProject3);
        miaContext.get().setContext(projectId, sessionId);
        Assert.assertEquals(miaContext.get().getFlowData().getProjectName(), testProject3.getName());
        Assert.assertEquals(miaContext.get().getFlowData().getSessionId(), sessionId);
    }
}
