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

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Link;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

//@Disabled("Temporarily disabled for refactoring")
@Slf4j
public class MongoIntegrationTest extends BaseIntegrationTestConfiguration {

    String POT_END_POINT_SAVE = "/rest/pot/save";
    String sessionId;

    @BeforeEach
    public void prepare() {
        sessionId = UUID.randomUUID().toString();
    }

    @Test
    public void potSessionWorkSuccessfullyAndSavedToDb() {
        // request build&run
        webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(prepareProcess("SSH_BG", "ls -la")), ExecutionRequest.class)
                .exchange().block();
        ExecutionRequest request = createPotSaveRequest();
        List<Link> saveResponse = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(POT_END_POINT_SAVE)
                                .queryParam("projectId", projectId.get())
                                .queryParam("sessionId", miaContext.getFlowData().getSessionId()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .exchange()
                .flatMap(resp -> {
                    if (resp.statusCode().isError()) {
                        String err = "Error during webClient SSH request execution code [" + resp.rawStatusCode() + "]";
                        log.error(err);
                        return Mono.error(new Exception(err));
                    }
                    ParameterizedTypeReference<List<Link>> typeRef = new ParameterizedTypeReference<List<Link>>() {
                    };
                    return resp.bodyToMono(typeRef);
                })
                .block();
        Assert.assertNotNull(saveResponse);
        String filename = saveResponse.get(0).getName();
        Path path = miaContext.getLogPath().resolve(filename);
        assertTrue(path.toFile().exists());
    }

    /**
     * Create request with custom connection timeouts,
     * which set in params map.
     */
    protected ExecutionRequest createPotSaveRequest() {
        miaContext.getFlowData().addParameter(Constants.CustomParameters.WORKING_DIRECTORY, "/tmp/TA");
        return ExecutionRequest.builder()
                .sessionId(miaContext.getFlowData().getSessionId())
                .flowData(miaContext.getFlowData())
                .build();
    }

    protected ExecutionRequest prepareProcess(String process, String command) {
        return ExecutionRequest.builder()
                .sessionId(miaContext.getFlowData().getSessionId())
                .command(command)
                .process(process)
                .flowData(miaContext.getFlowData())
                .build();
    }
}
