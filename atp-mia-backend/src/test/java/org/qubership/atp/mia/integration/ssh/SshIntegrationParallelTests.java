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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Statuses;
import org.springframework.http.MediaType;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Parallel tests which check is one thread takes response from another.
 */
//@Disabled("Temporarily disabled for refactoring")
public class SshIntegrationParallelTests extends BaseIntegrationTestConfiguration {

    /**
     * Amount of parallel threads in test.
     */
    private final int maxBound = 6;
    private final int minBound = 1;

    @Test
    public void testSsh_whenManyParallelQueries_expectEveryQueryCorrectOutput() {
        final String process = "Fill Input by 0";
        List<String> commands =
                IntStream.range(minBound, maxBound).mapToObj(x -> "sleep " + x + "; echo 'process" + x + "'").collect(Collectors.toList());
        Set<RequestResponseMap> commandsWithRequests = new HashSet<>();
        commands.forEach(c -> commandsWithRequests.add( new RequestResponseMap(c, createSshRequest(process, c))));
        checkResponsesArray(commandsWithRequests);
    }

    @Test
    public void testSsh_whenParallelQueriesAndRandomSleep_expectEveryQueryCorrectOutput() {
        final String process = "Fill Input by 0";
        List<String> commands = IntStream.range(minBound, maxBound).mapToObj(x -> "sleep "
                + (new Random().nextInt(maxBound))
                + "; echo 'process" + x + "'").collect(Collectors.toList());
        Set<RequestResponseMap> commandsWithRequests = new HashSet<>();
        commands.forEach(c -> commandsWithRequests.add( new RequestResponseMap(c, createSshRequest(process, c))));
        checkResponsesArray(commandsWithRequests);
    }

    @Test
    public void testSsh_whenNoSleepInParallel_expectEveryQueryCorrectOutput() {
        final String process = "Fill Input by 0";
        List<String> commands = IntStream.range(minBound, maxBound).mapToObj(x -> "echo 'process" + x + "'").collect(Collectors.toList());
        Set<RequestResponseMap> commandsWithRequests = new HashSet<>();
        commands.forEach(c -> commandsWithRequests.add( new RequestResponseMap(c, createSshRequest(process, c))));
        checkResponsesArray(commandsWithRequests);
    }

    private ExecutionResponse sendRequest(UUID projectId, ExecutionRequest request) {
        // request build&run
        ExecutionResponse result;
        try {
            System.out.println("PROCESS NAME: " + request.getCommand());
            result = webClient.post().uri(uriBuilder ->
                            uriBuilder.path(PROCESS_END_POINT)
                                    .queryParam("projectId", projectId)
                                    .queryParam("env", TEST_ENVIRONMENT_NAME).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), ExecutionRequest.class)
                    .exchange()
                    .flatMap(resp -> {
                        if (resp.statusCode().is5xxServerError()) {
                            resp.body((clientResponse, context) -> clientResponse.getBody());
                            return resp.bodyToMono(ExecutionResponse.class);
                        }
                        return resp.bodyToMono(ExecutionResponse.class);
                    })
                    .doOnError(x -> new ExecutionResponse())
                    .block();
            System.out.println("PROCESS executed POST: " + request.getCommand());
            // check response
            Assert.assertNotNull(result);
            Assert.assertEquals(Statuses.SUCCESS, result.getProcessStatus().getStatus());
            CommandOutput commandOutput = result.getCommandResponse().getCommandOutputs().getFirst();
            Assert.assertNotNull(commandOutput);
            String filename = commandOutput.getLink().getName();
            Assert.assertTrue(commandOutput.getLink().getPath().contains(filename));
            Assert.assertTrue(commandOutput.getInternalPathToFile().contains(filename));
            Assert.assertTrue(commandOutput.getExternalPathToFile().contains(filename));
        } catch (NullPointerException npe) {
            result = new ExecutionResponse();
            System.out.println("NPE during execution of the command " + request.getCommand() + "; " + npe);
        }
        return result;
    }

    private void checkResponsesArray(Set<RequestResponseMap> requestResponseMap) {
        ExecutorService executorService = Executors.newFixedThreadPool(requestResponseMap.size());
        final UUID id = projectId.get();
        requestResponseMap.forEach(c -> c.response = executorService.submit(() -> {
            System.out.println("Send request for key '" + c.key + "' with command " + c.request.getCommand());
            ExecutionResponse response = sendRequest(id, c.request);
            System.out.println("Receive response '" + c.key + "' with command response " + response.getExecutedCommand());
            return response;
        }));
        executorService.shutdown();
        int countPos = 0;
        List<String> varsNeg = new ArrayList<>();
        for (RequestResponseMap requestResponse : requestResponseMap) {
            ExecutionResponse response;
            try {
                response = requestResponse.response.get();
            } catch (InterruptedException|ExecutionException e) {
                throw new RuntimeException(e);
            }
            boolean contentSameToCommand = false;
            try {
                contentSameToCommand = requestResponse.key.equals(response.getExecutedCommand());
            } catch (NullPointerException npe) {
                System.out.println("NPE on reading response of command " + requestResponse.key + "; " + npe);
            }
            if (contentSameToCommand) {
                countPos++;
            } else {
                varsNeg.add(String.format("[Expected result [%s];  Actual result: [%s]]\n",
                        requestResponse.key, response.getExecutedCommand()));
            }
        }
        String err = String.format("Errors occurred during execution. Total commands: %s, Positive answers: %s, "
                + "Negative answers: %s, where errors:\n %s", requestResponseMap.size(), countPos, varsNeg.size(), varsNeg);
        Assert.assertEquals(err, requestResponseMap.size(), countPos);
    }

    @RequiredArgsConstructor
    class RequestResponseMap {
        final String key;
        final ExecutionRequest request;
        Future<ExecutionResponse> response;
    }
}
