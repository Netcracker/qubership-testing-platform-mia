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

package org.qubership.atp.mia.repo.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.repo.impl.RestRepository.HEADER_CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.RestLoopParameters;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.service.execution.RestClientService;

public class RestRepositoryTest extends RestRepositoryTestConfiguration {

    final ThreadLocal<HttpClient> client = new ThreadLocal<>();
    final ThreadLocal<RestClientService> restClientExecutor = new ThreadLocal<>();
    final ThreadLocal<Command> command = new ThreadLocal<>();
    final ThreadLocal<String> filename = new ThreadLocal<>();
    final ThreadLocal<File> logFile = new ThreadLocal<>();
    final ThreadLocal<HttpUriRequestBase> requestBase = new ThreadLocal<>();
    final ThreadLocal<ClassicHttpResponse> response = new ThreadLocal<>();
    final ThreadLocal<StatusLine> status = new ThreadLocal<>();

    @AfterEach
    public void cleanLogFile() {
        final File logFile = new File(filename.get());
        if (logFile.exists()) {
            logFile.deleteOnExit();
        }
    }

    @BeforeEach
    public void beforeRestRepositoryTest() {
        // mock
        client.set(mock(HttpClient.class));
        restClientExecutor.set(mock(RestClientService.class));
        requestBase.set(mock(HttpUriRequestBase.class));
        response.set(mock(ClassicHttpResponse.class));
        status.set(mock(StatusLine.class));
        repository.set(spy(new RestRepository(miaContext.get(), restClientExecutor.get(), metricsService)));
        // construct
        command.set(new Command());
        command.get().setSystem(Constants.DEFAULT_SYSTEM_NAME);
        command.get().setRest(rest.get());
        Header[] headers = new Header[]{new BasicHeader("filename=", "text.log")};
        Path path = Path.of("PROJECT_FOLDER/"
                + miaContext.get().getFlowData().getProjectId() + "/"
                + ProjectFileType.MIA_FILE_TYPE_LOG + "/"
                + miaContext.get().getFlowData().getSessionId() + "/");
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            System.err.println("Error creating folders: " + e.getMessage());
        }
        logFile.set(new File(path.resolve(miaContext.get().createLogFileName(command.get())).toString()));
        filename.set(logFile.get().getName());
        // stub
        when(status.get().getStatusCode()).thenReturn(200);
        when(new StatusLine(response.get())).thenReturn(status.get());
        when(response.get().getHeaders()).thenReturn(headers);
        when(restClientExecutor.get().createFileWithResponse(any(), any())).thenReturn(filename.get());
        when(restClientExecutor.get().prepareRestClient(any(), anyBoolean(), anyMap())).thenReturn(client.get());
        when(restClientExecutor.get().prepareRestRequest(any(), any(), anyMap())).thenReturn(requestBase.get());
        when(restClientExecutor.get().executeRestRequest(any(), any())).thenReturn(response.get());
    }

    @Test
    public void when_checkForTextForBody_then_restClientExecutorOnce() {
        doReturn(new AbstractMap.SimpleEntry<>(new File("./logs/tests/" + "test.txt"), "bodytest")).when(repository.get()).getResponseBody(
                eq(command.get()), any(ClassicHttpResponse.class));
        rest.get().setParseResponseAsTable(false);
        rest.get().setRestLoopParameters(RestLoopParameters.builder().textToCheck("test").build());
        CommandResponse commandResponse = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(commandResponse);
        verify(restClientExecutor.get(), times(1)).executeRestRequest(any(HttpClient.class), any(HttpUriRequestBase.class));
    }

    @Test
    public void when_checkForTextForHeader_then_restClientExecutorOnce() {
        doReturn(new AbstractMap.SimpleEntry<>(new File("./logs/tests/" + "test.txt"), "blabla")).when(repository.get()).getResponseBody(
                eq(command.get()), any(ClassicHttpResponse.class));
        rest.get().setParseResponseAsTable(false);
        rest.get().setRestLoopParameters(RestLoopParameters.builder().textToCheck("text").build());
        CommandResponse commandResponse = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(commandResponse);
        verify(restClientExecutor.get(), times(1)).executeRestRequest(any(HttpClient.class), any(HttpUriRequestBase.class));
    }

    @Test
    public void when_checkForTextForIncorrectBody_then_restClientExecutor4times() {
        doReturn(new AbstractMap.SimpleEntry<>(new File("./logs/tests/" + "test.txt"), "bodytest")).when(repository.get()).getResponseBody(
                eq(command.get()), any(ClassicHttpResponse.class));
        rest.get().setParseResponseAsTable(false);
        rest.get().setRestLoopParameters(RestLoopParameters.builder().textToCheck("incorrect").build());
        CommandResponse commandResponse = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(commandResponse);
        Assertions.assertInstanceOf(IllegalArgumentException.class, commandResponse.getErrors().getFirst());
        verify(restClientExecutor.get(), times(4)).executeRestRequest(any(HttpClient.class), any(HttpUriRequestBase.class));
    }

    @Test
    public void when_restResponseSqlFilled_getResponse() throws IOException {
        String responseStr = "{\"file\":\"http://project-artery-dev1.dev-cloud.somedomain.com/uploads/"
                + "project_amm_202009071422139305.xlsx\",\"table\":{\"columns\":[\"TEST CASE ID\","
                + "\"STATUS\",\"COMMENT\"],\"data\":[[\"MISSED_705449853-14-29-04\",\"MISSED\","
                + "\"No matching record found for event 705449853-14-29-04].\"]]}}";
        getAndCheckResponse(responseStr);
    }

    @Test
    public void when_restResponseSqlNull_getResponse() throws IOException {
        String responseStr = "{\"table\": {\"columns\": [], \"data\": []}}";
        getAndCheckResponse(responseStr);
    }

    private void getAndCheckResponse(String responseStr) throws IOException {
        doReturn(new AbstractMap.SimpleEntry<>(new File(logFile.get().getPath()), responseStr)).when(repository.get())
                .getResponseBody(eq(command.get()), any(ClassicHttpResponse.class));
        createLogFile(logFile.get().getPath(), responseStr);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getSqlResponse());
        DbTable table = result.getSqlResponse().getData();
        Assertions.assertNotNull(table);
        Assertions.assertNotNull(table.getColumns());
        Assertions.assertNotNull(table.getData());
    }
    /**
     * when content type is not set
     * then getType return undefined
     */
    @Test
    public void when_ContentDisposition_executeSaveResponse() {
        // construct
        rest.get().setParseResponseAsTable(false);
        File file = miaContext.get().getLogPath().resolve("text.zip").toFile();
        Header[] headers = new Header[]{new BasicHeader("Content-Disposition", "filename=text.zip")};
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity(
                new ByteArrayInputStream("anyfile".getBytes(StandardCharsets.UTF_8)),
                ContentType.APPLICATION_OCTET_STREAM,
                true
        );
        // stub
        when(response.get().getHeaders()).thenReturn(headers);
        when(response.get().getEntity()).thenReturn(basicHttpEntity);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getCommandOutputs().getFirst().isDisplayed());
        Assertions.assertEquals(file.getPath(), result.getCommandOutputs().getFirst().getInternalPathToFile());
    }

    @Test
    public void when_ExecuteRest_RESTFullInfoFilePresent() throws Exception {
        // construct
        rest.get().setParseResponseAsTable(false);
        Header[] headers = new Header[]{new BasicHeader("Content-Disposition", "filename=text.json"),
                new BasicHeader(HEADER_CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())};
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity(
                new ByteArrayInputStream("anyfile".getBytes(StandardCharsets.UTF_8)),
                ContentType.APPLICATION_OCTET_STREAM,
                true
        );
        // stub
        when(response.get().getHeaders()).thenReturn(headers);
        when(response.get().getEntity()).thenReturn(basicHttpEntity);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getCommandOutputs().size());
        CommandOutput co2 = result.getCommandOutputs().get(1);
        Assertions.assertFalse(co2.isDisplayed());
        Assertions.assertTrue(co2.getLink().getPath().contains("REST_FULL_INFO"));
        String fileContent = FileUtils.readFileToString(new File(co2.getInternalPathToFile()), "utf-8");
        Assertions.assertTrue(fileContent.contains("timestampRequest"));
        Assertions.assertTrue(fileContent.contains("code"));
        Assertions.assertTrue(fileContent.contains("timestampResponse"));
        Assertions.assertTrue(fileContent.contains("postScriptResults"));
        Assertions.assertTrue(fileContent.contains("headersResponse"));
        Assertions.assertTrue(fileContent.contains("bodyResponse"));
        Assertions.assertFalse(result.getConnectionInfo().containsKey("bodyResponse"));
    }

    @Test
    public void when_ContentType_executeSaveResponse() {
        // construct
        rest.get().setParseResponseAsTable(false);
        Header[] headers = new Header[]{new BasicHeader("Content-Type", "application/pdf")};
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity(
                new ByteArrayInputStream("anyfile".getBytes(StandardCharsets.UTF_8)),
                ContentType.APPLICATION_OCTET_STREAM,
                true
        );
        // stub
        when(response.get().getHeaders()).thenReturn(headers);
        when(response.get().getEntity()).thenReturn(basicHttpEntity);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getCommandOutputs().getFirst().isDisplayed());
    }

    @Test
    public void when_ContentTypeWithCharSet_executeSaveResponse() {
        // construct
        rest.get().setParseResponseAsTable(false);
        Header[] headers = new Header[]{new BasicHeader("Content-Type", "application/json;charset=utf-8")};
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity(
                new ByteArrayInputStream("anyfile".getBytes(StandardCharsets.UTF_8)),
                ContentType.APPLICATION_OCTET_STREAM,
                true
        );
        // stub
        when(response.get().getHeaders()).thenReturn(headers);
        when(response.get().getEntity()).thenReturn(basicHttpEntity);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getCommandOutputs().getFirst().isDisplayed());
        Assertions.assertTrue(result.getCommandOutputs().getFirst().getLink().getName().endsWith(".json"),
                "File should have Json extension");
    }

    @Test // Test-32143
    public void when_ContentTypeApplicationJson_executeSaveResponse_shouldNotReadResponseEntityTwice() {
        // expect
        final String expected = "{\"message\":\"it's json\"}";
        // mock
        ClassicHttpResponse response = Mockito.mock(ClassicHttpResponse.class);
        StatusLine status = Mockito.mock(StatusLine.class);
        // construct
        Header[] headers =
                new Header[]{new BasicHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())};
        HttpEntity entity = EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setStream(new ByteArrayInputStream(expected.getBytes(StandardCharsets.UTF_8)))
                .chunked()
                .build();
        // stub
        when(new StatusLine(response)).thenReturn(status);
        when(status.getStatusCode()).thenReturn(200);
        when(response.getHeaders()).thenReturn(headers);
        when(response.getEntity()).thenReturn(entity);
        // assert
        Map.Entry<File, String> result = repository.get().getResponseBody(command.get(), response);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getKey());
        Assertions.assertEquals(expected, result.getValue());
    }

    @Test
    public void when_restLoopParametersTextToEvaluate_executeRest_shouldEvaluate() {
        // expect
        final String expectedVariableName = "variableToSubstitute";
        final String expectedVariable = ":" + expectedVariableName;
        final String expectedValue = "testRestLoopValue";
        miaContext.get().getFlowData().addParameter(expectedVariableName, expectedValue);
        // construct
        rest.get().setParseResponseAsTable(false);
        rest.get().setRestLoopParameters(RestLoopParameters.builder().textToCheck(expectedVariable).build());
        Header[] headers =
                new Header[]{new BasicHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())};
        HttpEntity entity = EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setStream(new ByteArrayInputStream(expectedValue.getBytes(StandardCharsets.UTF_8)))
                .chunked()
                .build();
        // stub
        when(response.get().getHeaders()).thenReturn(headers);
        when(response.get().getEntity()).thenReturn(entity);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getErrors());
    }

    @Test
    public void negativeWhen_restLoopParametersTextWontEvaluate_executeRest_shouldError() {
        // expect
        final String expectedVariableName = "variableToSubstitute";
        final String expectedValue = "testRestLoopValue";
        miaContext.get().getFlowData().addParameter(expectedVariableName, expectedValue);
        // construct
        rest.get().setParseResponseAsTable(false);
        rest.get().setRestLoopParameters(RestLoopParameters.builder().textToCheck(expectedVariableName).build());
        Header[] headers =
                new Header[]{new BasicHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())};
        HttpEntity entity = EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setStream(new ByteArrayInputStream(expectedValue.getBytes(StandardCharsets.UTF_8)))
                .chunked()
                .build();
        // stub
        when(response.get().getHeaders()).thenReturn(headers);
        when(response.get().getEntity()).thenReturn(entity);
        CommandResponse result = repository.get().sendRestRequest(command.get());
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getErrors());
        Assertions.assertFalse(result.getErrors().isEmpty());
        final String errMsg = "Text '%s' defined but not found".formatted(rest.get().getRestLoopParameters().getTextToCheck());
        Assertions.assertEquals(errMsg, result.getErrors().getFirst().getMessage());
    }
}
