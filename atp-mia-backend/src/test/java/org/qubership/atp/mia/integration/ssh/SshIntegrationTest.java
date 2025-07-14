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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.qubership.atp.mia.integration.utils.TestUtils.getSshTestParams;
import static org.qubership.atp.mia.integration.utils.TestUtils.readFile;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_ID;

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Statuses;
import org.springframework.http.MediaType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


//@Disabled("Temporarily disabled for refactoring")
@Slf4j
public class SshIntegrationTest extends BaseIntegrationTestConfiguration {

    @Test()
    public void testSshBg_ExpectConnectionTimeoutBreakConnect() {
        // prepare
        String process = "SSH_BG";
        String bashCommand = "sleep 15; echo 20 > txt;";
        String expectedErrMessage = "Ssh command execution was interrupted by timeout.";
        ExecutionRequest request = createSshRequest(process, bashCommand,
                getSshTestParams(sshServerHost + ":" + sshPort, 99000, 2000));
        // request build&run
        Object result = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .exchange()
                .flatMap(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(Exception.class);
                    }
                    return response.bodyToMono(ExecutionResponse.class);
                })
                .block();
        // check exception response
        assertTrue(result instanceof Exception);
        assertTrue(((Exception) result).getMessage().contains(expectedErrMessage));
    }

    @Test
    public void testSshBg_ExpectExecutionTimeoutBreakExecution() {
        // prepare
        String process = "SSH_BG";
        String bashCommand = "sleep 15; echo 20 > txt;";
        String expectedErrMessage = "Ssh command execution was interrupted by timeout.";
        ExecutionRequest request = createSshRequest(process, bashCommand,
                getSshTestParams(sshServerHost + ":" + sshPort, 99000, 2000));
        // request build&run
        assertThrows(Exception.class, () -> {
            ExecutionResponse result = webClient.post().uri(uriBuilder ->
                            uriBuilder.path(PROCESS_END_POINT)
                                    .queryParam("projectId", DEFAULT_PROJECT_ID)
                                    .queryParam("env", testEnvironment.get().getName()).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), ExecutionRequest.class)
                    .exchange()
                    .flatMap(resp -> {
                        if (resp.statusCode().isError()) {
                            String err = "Error during webClient request execution: code[" + resp.rawStatusCode() + "]";
                            log.error(err);
                            return Mono.error(new Exception(err));
                        }
                        return resp.bodyToMono(ExecutionResponse.class);
                    })
                    .block();
            // check response
            Assert.assertNotNull(result);
            Assert.assertEquals(Statuses.FAIL, result.getProcessStatus().getStatus());
            Exception outputException = result.getCommandResponse().getErrors().getFirst();
            Assert.assertNotNull(outputException);
            assertTrue(outputException.getMessage().contains(expectedErrMessage));
        });
    }

    @Test
    public void testSshDownloadFilesFromLsResult() {
        // prepare
        String process = "SSH_DownloadFiles_EngageOne";
        String bashCommand = "ls -la";
        miaContext.getFlowData().addParameter("infinys_root", "/tmp/TA/");
        ExecutionRequest request = createSshRequest(process, bashCommand);
        getSshConnectionManager().runCommand("mkdir -p /tmp/TA/PROJECT_ENGAGEONE/output/periodicbill/{ebill,print}");
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
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        CommandOutput commandOutput = response.getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String filename = commandOutput.getLink().getName();
        assertTrue(commandOutput.getLink().getPath().contains(filename));
        assertTrue(commandOutput.getInternalPathToFile().contains(filename));
        assertTrue(commandOutput.getExternalPathToFile().contains(filename));
        //compare execution result and downloaded file
        List<String> content = commandOutput.contentFromFile();
        List<String> file = readFile(commandOutput.getInternalPathToFile());
        Assert.assertArrayEquals(content.toArray(), file.toArray());
    }

    @Test
    public void testSshLsCommand() {
        // prepare
        String process = "SSH_BG";
        String bashCommand = "ls -la";
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
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        CommandOutput commandOutput = response.getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String filename = commandOutput.getLink().getName();
        assertTrue(commandOutput.getLink().getPath().contains(filename));
        assertTrue(commandOutput.getInternalPathToFile().contains(filename));
        assertTrue(commandOutput.getExternalPathToFile().contains(filename));
    }

    @Test
    public void testSshCommandGlobalVariables() {
        // prepare
        String process = "SSH_BG";
        String bashCommand = "ls -la";
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
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        CommandOutput commandOutput = response.getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String filename = commandOutput.getLink().getName();
        assertTrue(commandOutput.getLink().getPath().contains(filename));
        assertTrue(commandOutput.getInternalPathToFile().contains(filename));
        assertTrue(commandOutput.getExternalPathToFile().contains(filename));
    }

    @Test
    public void testSshTransferFile() {
        // prepare
        String process = "SSH_TransferFile_E1EXP";
        String bashCommand = "ls -la";
        ExecutionRequest request = createSshRequest(process, bashCommand);
        // request build&run
        ExecutionResponse response = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .exchange()
                .flatMap(resp -> {
                    if (resp.statusCode().isError()) {
                        String err = "Error during webClient SSH request execution code [" + resp.rawStatusCode() + "]";
                        log.error(err);
                        return Mono.error(new Exception(err));
                    }
                    return resp.bodyToMono(ExecutionResponse.class);
                })
                .block();
        // check response
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        CommandOutput commandOutput = response.getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String filename = commandOutput.getLink().getName();
        assertTrue(commandOutput.getLink().getPath().contains(filename));
        assertTrue(commandOutput.getInternalPathToFile().contains(filename));
        assertTrue(commandOutput.getExternalPathToFile().contains(filename));
    }

    @Test
    public void testSshUploadFile() {
        String process = "SSH_UploadFile";
        String bashCommand = "cd :pathForUpload; ls | grep soap";
        String filename = "soap_example.soap";
        miaContext.getFlowData().addParameter("pathForUpload", "/root/");
        miaContext.getFlowData().addParameter("input_file", "etalon_files/" + filename);
        ExecutionRequest request = createSshRequest(process, bashCommand);
        // request build&run
        Optional<ExecutionResponse> response = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .retrieve()
                .bodyToMono(ExecutionResponse.class).blockOptional();
        //.block();
        // check response
        assertTrue(response.isPresent());
        Assert.assertEquals(Statuses.SUCCESS, response.get().getProcessStatus().getStatus());
        CommandOutput commandOutput = response.get().getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String savedFileAtServer = commandOutput.getLink().getName();
        assertTrue(commandOutput.getLink().getPath().contains(savedFileAtServer));
        assertTrue(commandOutput.getInternalPathToFile().contains(savedFileAtServer));
        assertTrue(commandOutput.getExternalPathToFile().contains(savedFileAtServer));
        Assert.assertEquals(filename, commandOutput.contentFromFile().get(0));
    }

    @Test
    public void testSshUploadFilesAndDownloadAll() {
        // prepare
        String process = "SSH_UploadFileAndDownloadResult_DUMP";
        String bashCommand = "ls -la";
        miaContext.getFlowData().addParameter("accountNumber", "1000000103");
        miaContext.getFlowData().addParameter("accountNumber_", "1000000103_");
        miaContext.getFlowData().addParameter("infinys_root", "/tmp/TA/");
        miaContext.getFlowData().addParameter("customerRef", "1234567");
        miaContext.getFlowData().addParameter("customerRef_", "1234567_");
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
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        Assert.assertNotNull(response.getCommandResponse().getCommandOutputs());
        Assert.assertEquals(6, response.getCommandResponse().getCommandOutputs().size());
        CommandOutput commandOutput = response.getCommandResponse().getCommandOutputs().getFirst();
        Assert.assertNotNull(commandOutput);
        String filename = commandOutput.getLink().getName();
        assertTrue(commandOutput.getLink().getPath().contains(filename));
        assertTrue(commandOutput.getInternalPathToFile().contains(filename));
        assertTrue(commandOutput.getExternalPathToFile().contains(filename));
    }
}
