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

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.springframework.http.MediaType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Negative tests which verify that execution response contains error
 * and ssh response to ease rc understanding.
 */

//@Disabled("Temporarily disabled for refactoring")
@Slf4j
public class ExecutorBackCompatibilityTest extends BaseIntegrationTestConfiguration {

    @Test
    public void runSsh_whenBracketsAndAmpersandWithWrongSemicolon_expectBashSyntaxError() {
        // prepare
        String process = "SSH_BG";
        String bashCommand = "{ ls -la &; }"; // correct: { ls -la & ; }
        ExecutionRequest request = createSshRequest(process, bashCommand);
        // request build&run
        ExecutionResponse response = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .retrieve()
                .bodyToMono(ExecutionResponse.class)
                .block();
        // check response
        Assert.assertNotNull(response);
        CommandOutput commandOutput = response.getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String filename = commandOutput.getLink().getName();
        Assert.assertTrue("result doesn't contain filepath",
                commandOutput.getLink().getPath().contains(filename));
    }

    @Test()
    public void runSsh_whenBracketsAndNoSemicolon_expectBashSyntaxError() {
        // prepare
        String process = "SSH_BG";
        String bashCommand = "{ ls -la }"; // correct: { ls -la; }
        ExecutionRequest request = createSshRequest(process, bashCommand);
        // request build&run
        Exception response = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .exchange().flatMap(clientResponse -> clientResponse.bodyToMono(Exception.class))
                .block();
        Assert.assertTrue(response.getMessage().contains("Incorrect exit"));
        Assert.assertTrue(response.getMessage().contains("incorrect command"));
    }
}
