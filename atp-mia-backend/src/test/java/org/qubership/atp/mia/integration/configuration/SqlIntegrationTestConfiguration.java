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

package org.qubership.atp.mia.integration.configuration;

import static org.qubership.atp.mia.TestConstants.SYS_DATE_VALUE;

import java.util.List;

import org.junit.Assert;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Statuses;
import org.springframework.http.MediaType;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class SqlIntegrationTestConfiguration extends BaseIntegrationTestConfiguration {

    protected ExecutionRequest prepareSqlRequest(String sqlCommand) {
        String process = "SQL_GPARAMS";
        FlowData flowData = miaContext.getFlowData();
        flowData.addParameter("SYSdateValue", SYS_DATE_VALUE);
        flowData.addParameter("processName", process);
        flowData.addParameter(Constants.CustomParameters.WORKING_DIRECTORY, "/tmp/TA/");
        return ExecutionRequest.builder()
                .process(process)
                .command(sqlCommand)
                .flowData(flowData)
                .build();
    }

    protected ExecutionResponse executeDbQuery(String jdbcConStr, String sqlCommand) {
        ExecutionRequest request = prepareSqlRequest(sqlCommand);
        ExecutionResponse result = webClient.post().uri(uriBuilder ->
                        uriBuilder.path(PROCESS_END_POINT)
                                .queryParam("projectId", projectId.get())
                                .queryParam("env", testEnvironment.get().getName()).build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), ExecutionRequest.class)
                .exchange()
                .flatMap(response -> {
                    if (response.statusCode().isError()) {
                        String err = "Error during webClient request execution: code[" + response.rawStatusCode() + "]";
                        log.error(err);
                        return Mono.error(new Exception(err));
                    }
                    return response.bodyToMono(ExecutionResponse.class);
                })
                .block();
        System.out.println("ResponseBody: " + result);
        Assert.assertNotNull("Http response shouldn't be null!", result);
        Assert.assertEquals("Http response status should be SUCCESS",
                Statuses.SUCCESS, result.getProcessStatus().getStatus());
        return result;
    }

    protected void checkColumnNames(List<String> columns) {
        Assert.assertEquals(2, columns.size());
        Assert.assertEquals("id", columns.get(0));
        Assert.assertEquals("name", columns.get(1));
    }
}
