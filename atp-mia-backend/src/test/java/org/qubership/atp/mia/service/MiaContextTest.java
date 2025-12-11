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

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
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
        Assertions.assertEquals(DEFAULT_PROJECT_NAME, miaContext.get().getFlowData().getProjectName());
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
                .connections(Collections.emptyList())
                .build();
        Environment testEnvironment2 = Environment.builder()
                .projectId(projectId)
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + "2")
                .systems(Collections.singletonList(testSystem2))
                .build();
        Project testProject2 = Project.builder()
                .id(projectId)
                .name(DEFAULT_PROJECT_NAME + "2")
                .environments(Collections.singletonList(testEnvironment2.getId()))
                .build();
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Collections.singletonList(testEnvironment2));
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Collections.singletonList(testEnvironment2));
        when(environmentsService.get().getEnvironmentsFull(eq(envId), eq(projectId)))
                .thenReturn(testEnvironment2);
        when(environmentsService.get().getProjects())
                .thenReturn(Collections.singletonList(testProject2));
        when(environmentsService.get().getProject(eq(projectId)))
                .thenReturn(testProject2);
        miaContext.get().setContext(projectId, null);
        Assertions.assertEquals(testProject2.getName(), miaContext.get().getFlowData().getProjectName());
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
                .connections(Collections.emptyList())
                .build();
        Environment testEnvironment3 = Environment.builder()
                .projectId(projectId)
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + "3")
                .systems(Collections.singletonList(testSystem3))
                .build();
        Project testProject3 = Project.builder()
                .id(projectId)
                .name(DEFAULT_PROJECT_NAME + "3")
                .environments(Collections.singletonList(testEnvironment3.getId()))
                .build();
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Collections.singletonList(testEnvironment3));
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId)))
                .thenReturn(Collections.singletonList(testEnvironment3));
        when(environmentsService.get().getEnvironmentsFull(eq(envId), eq(projectId)))
                .thenReturn(testEnvironment3);
        when(environmentsService.get().getProjects())
                .thenReturn(Collections.singletonList(testProject3));
        when(environmentsService.get().getProject(eq(projectId)))
                .thenReturn(testProject3);
        miaContext.get().setContext(projectId, sessionId);
        Assertions.assertEquals(testProject3.getName(), miaContext.get().getFlowData().getProjectName());
        Assertions.assertEquals(sessionId, miaContext.get().getFlowData().getSessionId());
    }

    /**
     * This test was primarily intended to clarify behavior of MiaContext#evaluateWithMacroses
     * and MiaContext#replaceMacrosWithResultOfMacros in various broken macro syntax cases.
     */
    @Test
    public void evaluateWithMacroses_test() {
        /*
            Successful evaluation
         */
        String result = miaContext.get().evaluateWithMacroses("Start ${Random(100)} end");

        /*
            Both 2 lines below (incomplete macros - starts, but not completes properly) produce the following error:

            java.lang.StringIndexOutOfBoundsException: Range [8, -1) out of bounds for length 12
            at ...
            at org.qubership.atp.mia.service.MiaContextTest.evaluateWithMacroses_test(MiaContextTest.java:122)

            String s1 = miaContext.get().evaluateWithMacroses("Start ${Random(100)");
            String s2 = miaContext.get().evaluateWithMacroses("Start ${ end");
         */

        /*
            Below line doesn't contain start of MIA macro - '${'.
            So, it's checked against regexp - may be, it's ATP macro?

            if (text.matches("(?s).*\\$\\w+\\(.*\\)") || text.matches("(?s).*\\#\\w+\\(.*\\)")) {
                return AtpMacrosUtils.evaluateWithAtpMacros(text, getFlowData().getProjectId());
            }
         */
        result = miaContext.get().evaluateWithMacroses("Start } end");

        /*
            Below line produces an exception:
            java.lang.StringIndexOutOfBoundsException: Range [0, -1) out of bounds for length 0
            on the command:
            final String typeAsString = macrosText.substring(0, macrosText.indexOf("("));

            So, it's supposed, that macros has parameter(s).
            But, this exception is just logged.
            So, method returns original parameter value.
         */
        result = miaContext.get().evaluateWithMacroses("Start ${} end");

        /*
            Below line produces an exception
                java.lang.StringIndexOutOfBoundsException: Range [10, -1) out of bounds for length 14
            here:
                final String macrosText = text.substring(start, end);

            String s5 = miaContext.get().evaluateWithMacroses("Start } ${ end");
         */

        String expected = "That's formatted date: 20-12-2025 23:55, please check.";
        result = miaContext.get().evaluateWithMacroses(
                "That's formatted date: "
                        + "${Date_Formatter(20251220 23550000, yyyyMMdd HHmmssSS, dd-MM-yyyy HH:mm)}, please check.");
        Assertions.assertEquals(expected, result);
    }
}
