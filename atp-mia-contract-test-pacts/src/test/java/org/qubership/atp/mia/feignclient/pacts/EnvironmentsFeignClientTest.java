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
 */

package org.qubership.atp.mia.feignclient.pacts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.mia.clients.api.environments.dto.projects.EnvironmentFullVer1ViewDto;
import org.qubership.atp.mia.service.client.EnvironmentsFeignClient;
import org.qubership.atp.mia.service.client.ProjectsFeignClient;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {EnvironmentsFeignClient.class})
@ContextConfiguration(classes = {TestAppConfiguration.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(properties = {"feign.atp.environments.name=atp-environments", "feign.atp.environments.route=",
                "feign.atp.environments.url=http://localhost:8888"})
public class EnvironmentsFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-environments", "localhost", 8888, this);
    @Autowired
    EnvironmentsFeignClient environmentsFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID environmentId = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9702");
        ResponseEntity<EnvironmentFullVer1ViewDto> result_EnvironmentFull =
                environmentsFeignClient.getEnvironment(environmentId, true);

        Assert.assertEquals(result_EnvironmentFull.getStatusCode().value(), 200);
        Assert.assertTrue(result_EnvironmentFull.getHeaders().get("Content-Type").contains("application/json"));

    }

    @Pact(consumer = "atp-mia")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        DslPart infoForGetEnvironmentResponse = new PactDslJsonBody()
                .integerType("created")
                .integerType("modified")
                .uuid("createdBy")
                .uuid("modifiedBy")
                .uuid("projectId")
                .uuid("id")
                .stringType("description")
                .stringType("name")
                .array("systems").object().closeArray();

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /api/environments/{environmentId} OK")
                .path("/api/environments/7c9dafe9-2cd1-4ffc-ae54-45867f2b9702")
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(infoForGetEnvironmentResponse);
        return response.toPact();
    }

    private ProjectsFeignClient getFeignClient() {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new ResponseEntityDecoder(new JacksonDecoder())).contract(new SpringMvcContract())
                .target(ProjectsFeignClient.class, mockProvider.getUrl());
    }
}

