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

import static org.qubership.atp.mia.utils.FileUtils.logIntoFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.qubership.atp.mia.exceptions.rest.RestCopyResultToStringException;
import org.qubership.atp.mia.exceptions.rest.RestCreateConnectionFailException;
import org.qubership.atp.mia.exceptions.rest.RestNotFoundException;
import org.qubership.atp.mia.exceptions.rest.RestParseErrorException;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.ContentType;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.model.impl.executable.RestLoopParameters;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.execution.RestClientService;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.stereotype.Repository;

import clover.org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RestRepository {

    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final TypeReference<DbTable> TYPE_REF_DB_TABLE = new TypeReference<DbTable>() {
    };
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final MiaContext miaContext;
    private final RestClientService restClient;
    private final MetricsAggregateService metricsService;

    /**
     * Sends rest request.
     */
    public CommandResponse sendRestRequest(Command command) {
        if (command.getRest() == null) {
            throw new RestNotFoundException();
        }
        log.info("Sending REST request");
        final System system = miaContext.getFlowData().getSystem(command.getSystem());
        //TODO: remove next string after migration connections from REST to HTTP in envs
        Server server;
        try {
            server = system.getServer("REST");
        } catch (IllegalArgumentException e1) {
            try {
                server = system.getServer(Connection.SourceTemplateId.HTTP);
            } catch (IllegalArgumentException e2) {
                throw new RestCreateConnectionFailException(e2);
            }
        }
        final HashMap<String, String> connectionInfo = new HashMap<>();
        final Rest rest = command.getRest();
        final RestLoopParameters restLoopParameters = rest.getRestLoopParameters();
        if (restLoopParameters != null && !Strings.isNullOrEmpty(restLoopParameters.getTextToCheck())) {
            restLoopParameters.setTextToCheck(miaContext.evaluate(restLoopParameters.getTextToCheck()));
        }
        log.info("Preparing REST client for project: {}", miaContext.getProjectId());
        HttpClient client = restClient.prepareRestClient(server, rest.isDisableRedirect(), connectionInfo);
        HttpRequestBase request = restClient.prepareRestRequest(rest, server, connectionInfo);
        connectionInfo.put("timestampRequest", Utils.getTimestamp());
        HttpResponse httpResponse = restClient.executeRestRequest(client, request);
        boolean textChecked = true;
        Map.Entry<File, String> responseBody = getResponseBody(command, httpResponse);
        if (responseBody.getValue() != null) {
            textChecked = checkForText(restLoopParameters, responseBody.getValue(), httpResponse.getAllHeaders());
        }
        int retryCount = 0;
        if (!textChecked && restLoopParameters != null
                && restLoopParameters.getMaxNumberRepeats() >= 0
                && restLoopParameters.getTimeoutRepeats() > 0) {
            for (int repeatId = 0; repeatId < restLoopParameters.getMaxNumberRepeats(); repeatId++) {
                try {
                    Thread.sleep(restLoopParameters.getTimeoutRepeats() * 1000L);
                } catch (InterruptedException e) {
                    log.error("restLoopParameters timeout error.", e);
                }
                httpResponse = restClient.executeRestRequest(client, request);
                responseBody = getResponseBody(command, httpResponse);
                if (responseBody.getValue() != null) {
                    textChecked = checkForText(restLoopParameters, responseBody.getValue(),
                            httpResponse.getAllHeaders());
                }
                retryCount++;
                if (textChecked) {
                    break;
                }
            }
            String pollingInfo = "ReTried Count : " + retryCount + " / "
                    + restLoopParameters.getMaxNumberRepeats()
                    + "\n Status : " + (textChecked ? "PASSED" : "FAILED")
                    + "\n TimeOut of Repeats : " + restLoopParameters.getTimeoutRepeats() * retryCount
                    + " Seconds"
                    + "\n Text to Check : " + restLoopParameters.getTextToCheck();
            connectionInfo.put("pollingStatus", pollingInfo);
        }
        connectionInfo.put("timestampResponse", Utils.getTimestamp());
        connectionInfo.put("code", String.valueOf(httpResponse.getStatusLine().getStatusCode()));
        StringJoiner headerResponse = new StringJoiner("\n");
        Arrays.stream(httpResponse.getAllHeaders()).forEach(h -> headerResponse.add(h.toString()));
        connectionInfo.put("headersResponse", headerResponse.toString());
        if (rest.isSaveCookie()) {
            Arrays.stream(httpResponse.getAllHeaders()).forEach(header -> {
                if (header.getName().contains("Cookie")) {
                    miaContext.getFlowData().addParameter("Cookie", header.getValue());
                    log.info("Cookie is saved to flow data [ " + header.getValue() + " ].");
                }
            });
        }
        final CommandOutput commandOutput = new CommandOutput(responseBody.getKey().getPath(), null,
                ContentType.getType(Utils.getHeaderValue(httpResponse, HEADER_CONTENT_TYPE)).isDisplay(), miaContext);
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.addLog(commandOutput);
        commandResponse.setConnectionInfo(connectionInfo);
        if (!textChecked) {
            commandResponse.addError(new IllegalArgumentException(
                    String.format("Text '%s' defined but not found", restLoopParameters == null
                            ? "null_value"
                            : restLoopParameters.getTextToCheck())
            ));
        }
        if (rest.isParseResponseAsTable()) {
            try {
                String commandOutputContent = commandOutput.contentFromFile().toString();
                JsonNode node = MAPPER.readTree(commandOutputContent.substring(1, commandOutputContent.length() - 1));
                if (node.has("table") && node.get("table").has("columns")
                        && node.get("table").has("data")) {
                    SqlResponse sqlResponse = new SqlResponse();
                    DbTable table = MAPPER.convertValue(node.get("table"), TYPE_REF_DB_TABLE);
                    if (table.getData() == null) {
                        table.setData(new ArrayList<>());
                    }
                    if (table.getColumns() == null) {
                        table.setColumns(new ArrayList<>());
                    }
                    sqlResponse.setData(table);
                    if (rest.isHasFile() && node.has("file")) {
                        String file = node.get("file").asText();
                        sqlResponse.setLink(new Link(file, Utils.getFileNameFromPath(file)));
                    }
                    commandResponse.setSqlResponse(sqlResponse);
                }
            } catch (IOException e) {
                log.error("Exception during parse response as table: {}", e.getMessage());
                commandResponse.addError(e);
            }
        }
        commandResponse.setPostScriptExecutionReport(executeScript(rest, httpResponse, responseBody.getValue(),
                command.getName()));
        try {
            connectionInfo.put("postScript", rest.getScript());
            connectionInfo.put("postScriptResults", commandResponse.getPostScriptExecutionReport());
            connectionInfo.put("bodyResponse", responseBody.getValue());
            File fileFullInfo = miaContext.getLogPath()
                    .resolve(miaContext.createLogFileName("REST_FULL_INFO", "json")).toFile();
            logIntoFile(Utils.GSON.toJson(connectionInfo), fileFullInfo);
            final CommandOutput commandFullInfo = new CommandOutput(fileFullInfo.getPath(), null, false, miaContext);
            commandResponse.addCommandOutput(commandFullInfo);
        } finally {
            connectionInfo.remove("postScript");
            connectionInfo.remove("postScriptResults");
            connectionInfo.remove("bodyResponse");
        }
        return commandResponse;
    }

    /**
     * Compute result of user script.
     */
    public String executeScript(Rest rest, HttpResponse httpResponse, String responseBody, String processName) {
        log.trace("Start executing post script of {} process", processName);
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("Nashorn");
        if (engine == null) {
            return String.format("%s can't define OpenJDK Nashorn script engine.", Constants.ERROR);
        }
        engine.put("request", rest);
        engine.put("response", httpResponse);
        engine.put("globalVariables", miaContext.getFlowData().getParameters());
        engine.put("collectionVariables", new HashMap<String, Object>());
        if (responseBody != null) {
            Object parsedResponse = parseResponse(new ObjectMapper(), responseBody.trim());
            engine.put("responseBody", parsedResponse);
        }
        String script = rest.getScript();
        if (StringUtils.isNotEmpty(script)) {
            try {
                engine.eval(script);
                @SuppressWarnings("unchecked")
                HashMap<String, String> result = (HashMap<String, String>) engine.eval("collectionVariables;");
                result.keySet().forEach(k -> {
                    try {
                        Object value = result.get(k);
                    } catch (ClassCastException cce) {
                        if (cce.getMessage().contains("jdk.nashorn.internal.runtime.Undefined")) {
                            result.put(k, "");
                        }
                    }
                });
                miaContext.getFlowData().addParameters(result);
                return "Post script has been successfully executed";
            } catch (ScriptException e) {
                log.trace("{} in script execution: {}", Constants.ERROR, e.getMessage());
                return String.format("%s in script: %s", Constants.ERROR, e.getMessage());
            } catch (Exception e) {
                log.trace("{} in script execution: {}", Constants.ERROR, e.getMessage());
                return String.format("%s something went wrong when adding the result to global variables: %s",
                        Constants.ERROR, e.getMessage());
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Parses a response string as JSON (map or list) with fallback to raw string.
     * @param mapper Jackson ObjectMapper for JSON parsing
     * @param responseBody The response string to parse
     * @return Parsed object (Map, List) or original string if parsing fails
     */
    private Object parseResponse(ObjectMapper mapper, String responseBody) {
        try {
            // Try to parse as Map first
            return mapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException mapException) {
            log.debug("Unable to parse responseBody as map, trying as list: {}", mapException.getMessage());
            try {
                // Try to parse as List
                return mapper.readValue(responseBody, new TypeReference<List<Object>>() {});
            } catch (JsonProcessingException listException) {
                log.debug("Unable to parse responseBody as list: {}", listException.getMessage());
                log.info("Using raw responseBody string as JSON parsing failed");
                // Return the original string
                return responseBody;
            }
        }
    }

    /**
     * Check rest headers or body for text.
     *
     * @param loopParameters loopParameters
     * @param responseBody   responseBody
     * @param headers        headers
     * @return true if search text found in boby or headers, false otherwise
     */
    boolean checkForText(RestLoopParameters loopParameters, String responseBody, Header[] headers) {
        boolean textChecked = true;
        if (loopParameters != null
                && !Strings.isNullOrEmpty(loopParameters.getTextToCheck())) {
            textChecked = loopParameters.isCheckTextInBody() && responseBody != null
                    && responseBody.contains(loopParameters.getTextToCheck());
            if (!textChecked) {
                textChecked = loopParameters.isCheckTextInHeaders() && headers != null
                        && Arrays.stream(headers).anyMatch(h -> h.toString().contains(loopParameters.getTextToCheck()));
            }
        }
        return textChecked;
    }

    /**
     * Save HttpResponse in File and if it's text the string representation also returned.
     *
     * @param httpResponse httpResponse
     * @return link to file with {@link HttpResponse} and string representation if header CONTENT_TYPE is text.
     * @throws RuntimeException if {@link HttpEntity} in {@code httpResponse} is not parsable.
     */
    protected Map.Entry<File, String> getResponseBody(Command command, HttpResponse httpResponse) {
        String stringBody = null;
        Path filename;
        ContentType contentType = ContentType.getType(Utils.getHeaderValue(httpResponse, HEADER_CONTENT_TYPE));
        if (command.getLogFileNameFormat() != null) {
            filename = Paths.get(StringUtils.deleteWhitespace(miaContext.evaluate(command.getLogFileNameFormat())));
        } else if (Utils.isHeaderNamePresent(httpResponse, HEADER_CONTENT_DISPOSITION)
                && (Utils.getFirstGroupFromStringByRegexp(
                Utils.getHeaderValue(httpResponse, HEADER_CONTENT_DISPOSITION), "filename=(.*)")) != null) {
            filename = Paths.get(Utils.getFirstGroupFromStringByRegexp(
                            Utils.getHeaderValue(httpResponse, HEADER_CONTENT_DISPOSITION), "filename=(.*)")
                    .replaceAll("\"", ""));
        } else {
            filename = Paths.get(miaContext.createFileName(contentType));
        }
        File file = miaContext.getLogPath().resolve(filename.getFileName()).toFile();
        try (FileOutputStream outStream = new FileOutputStream(file)) {
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                entity.writeTo(outStream);
                long restResponseSizeInKb = file.length() / 1024;
                log.info("[SIZE] REST Response length: {} kb", restResponseSizeInKb);
                metricsService.restResponseSize(restResponseSizeInKb);
                log.debug("File with response created, path: " + file.getPath());
                try {
                    if (contentType.isTextFormat()) {
                        stringBody = FileUtils.readFileToString(file);
                    }
                } catch (IOException e) {
                    throw new RestCopyResultToStringException(!file.exists(), e);
                }
            }
        } catch (Exception e) {
            throw new RestParseErrorException(e);
        }
        return new AbstractMap.SimpleEntry<>(file, stringBody);
    }
}
