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

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.service.execution.RestClientService;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@ExtendWith(SkipTestInJenkins.class)
@PowerMockIgnore(value = {"javax.management.*", "javax.script.*"})
@SpringBootTest(classes = {RestRepository.class}, properties = {"spring.cloud.vault.enabled=false"})
@PrepareForTest(Utils.class)
public class RestRepositoryPostScriptTest extends ConfigTestBean {

    private final String DEFAULT_PROCESS_NAME = "process";

    @MockBean
    private RestRepository restRepository;

    @MockBean
    protected SqlExecutionHelperService sqlService;

    @BeforeEach
    public void init() {
        restRepository = new RestRepository(miaContext.get(), mock(RestClientService.class), metricsService);
    }

    @Test
    public void givenEmptyScript_whenExecutingPostScript_thenExpectEmptyResult() {
        // Arrange
        String expected = "";
        Rest rest = new Rest();
        HttpResponse response = mock(HttpResponse.class);
        String body = "body";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void givenScriptWithGlobalVariable_whenExecutingPostScript_thenParameterIsStoredInFlowData() {
        // Arrange
        String expectedMessage = "Post script has been successfully executed";
        String expectedKey = "example_field";
        String expectedValue = "some_variable";
        String script = String.format("collectionVariables.put(\"%s\", \"%s\");", expectedKey, expectedValue);
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "body";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expectedMessage, result);
        Assertions.assertTrue(miaContext.get().getFlowData().getParameters().containsKey(expectedKey));
        Assertions.assertEquals(expectedValue, miaContext.get().getFlowData().getParameters().get(expectedKey));
    }

    @Test
    public void givenScriptUsingRestrictedJavaAPI_whenExecutingPostScript_thenExpectError() {
        // Arrange
        String expectedError = "Error in script: ReferenceError: \"java\" is not defined in <eval> at line number 1";
        String script = "var file = new java.io.File(\"sample.js\").createNewFile();";
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "body";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expectedError, result);
    }

    @Test
    public void givenPlainTextResponseBody_whenExecutingPostScript_thenExpectSuccess() {
        // Arrange
        String expectedMessage = "Post script has been successfully executed";
        String expectedKey = "example_field";
        String expectedValue = "some_variable";
        String script = String.format("collectionVariables.put(\"%s\", \"%s\");", expectedKey, expectedValue);
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "plain text body";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expectedMessage, result);
        Assertions.assertTrue(miaContext.get().getFlowData().getParameters().containsKey(expectedKey));
        Assertions.assertEquals(expectedValue, miaContext.get().getFlowData().getParameters().get(expectedKey));
    }

    @Test
    public void givenValidJsonResponse_whenExecutingPostScript_thenExpectSuccess() {
        // Arrange
        String expectedMessage = "Post script has been successfully executed";
        String expectedKey = "test_key";
        String expectedValue = "test_value";
        String script = String.format("collectionVariables.put(\"%s\", responseBody.get(\"field\"));", expectedKey);
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "{\"field\": \"test_value\"}";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expectedMessage, result);
        Assertions.assertTrue(miaContext.get().getFlowData().getParameters().containsKey(expectedKey));
        Assertions.assertEquals(expectedValue, miaContext.get().getFlowData().getParameters().get(expectedKey));
    }

    @Test
    public void givenValidJsonListResponse_whenExecutingPostScript_thenExpectSuccess() {
        // Arrange
        String expectedMessage = "Post script has been successfully executed";
        String expectedKey = "list_size";
        int expectedSize = 3;
        String script = String.format("collectionVariables.put(\"%s\", responseBody.size());", expectedKey);
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "[\"one\", \"two\", \"three\"]";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expectedMessage, result);
        Assertions.assertEquals(expectedSize, miaContext.get().getFlowData().getParameters().get(expectedKey));
    }

    @Test
    public void givenMalformedJsonResponse_whenExecutingPostScript_thenExpectFailure() {
        // Arrange
        String expectedErrorMessage = "Error in script";
        String script = "collectionVariables.put(\"test\", responseBody.get(\"field\"));";
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "{field: 'missing_quotes'}"; // Malformed JSON
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertTrue(result.contains(expectedErrorMessage));
    }

    @Test
    public void givenEmptyJsonResponse_whenExecutingPostScript_thenExpectScriptError() {
        // Arrange
        String expectedMessage = "Post script has been successfully executed";
        String expectedKey = "empty_check";
        Set<String> expectedValues = new HashSet<String>(Arrays.asList("undefined", ""));
        String script = String.format("collectionVariables.put(\"%s\", JSON.stringify(responseBody));", expectedKey);
        Rest rest = new Rest();
        rest.setScript(script);
        HttpResponse response = mock(HttpResponse.class);
        String body = "{}";
        // Act
        String result = restRepository.executeScript(rest, response, body, DEFAULT_PROCESS_NAME);
        // Assert
        Assertions.assertEquals(expectedMessage, result);
        Assertions.assertTrue(expectedValues
                .contains(String.valueOf(miaContext.get().getFlowData().getParameters().get(expectedKey))));
    }
}
