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
import java.util.List;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.mia.clients.api.environments.dto.projects.EnvironmentResDto;
import org.qubership.atp.mia.clients.api.environments.dto.projects.ProjectFullVer1ViewDto;
import org.qubership.atp.mia.clients.api.environments.dto.projects.ProjectFullVer2ViewDto;
import org.qubership.atp.mia.service.client.ProjectsFeignClient;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {ProjectsFeignClient.class})
@ContextConfiguration(classes = {TestAppConfiguration.class})
@Import({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class, FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(properties = {"feign.atp.environments.name=atp-environments", "feign.atp.environments.route=",
                "feign.atp.environments.url=http://localhost:8888"})
public class ProjectsFeignClientTest {

    @Rule
    public PactProviderRule mockProvider = new PactProviderRule("atp-environments", "localhost", 8888, this);
    @Autowired
    ProjectsFeignClient projectsFeignClient;

    @Test
    @PactVerification()
    public void allPass() {
        UUID projectId = UUID.fromString("7c9dafe9-2cd1-4ffc-ae54-45867f2b9702");
        ResponseEntity<List<ProjectFullVer2ViewDto>> result_getAllProjects =
                projectsFeignClient.getAllProjects(null, false);
        Assert.assertEquals(result_getAllProjects.getStatusCode().value(), 200);
        Assert.assertTrue(result_getAllProjects.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<ProjectFullVer1ViewDto> result_getProject = projectsFeignClient
                .getProject(projectId, null);
        Assert.assertEquals(result_getProject.getStatusCode().value(), 200);
        Assert.assertTrue(result_getProject.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<EnvironmentResDto>> result_getEnvironments = projectsFeignClient
                .getEnvironments(projectId, true);
        Assert.assertEquals(result_getEnvironments.getStatusCode().value(), 200);
        Assert.assertTrue(result_getEnvironments.getHeaders().get("Content-Type").contains("application/json"));

        ResponseEntity<List<EnvironmentResDto>> result_getTemporaryEnvironments = projectsFeignClient
                .getTemporaryEnvironments(projectId, true);
        Assert.assertEquals(result_getTemporaryEnvironments.getStatusCode().value(), 200);
        Assert.assertTrue(result_getTemporaryEnvironments.getHeaders().get("Content-Type").contains("application/json"));
    }

    @Pact(consumer = "atp-mia")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart object = new PactDslJsonBody()
                .stringType("description")
                .stringType("name")
                .integerType("created")
                .uuid("createdBy")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("shortName")
                .array("environments").object().closeArray();

        DslPart objects = new PactDslJsonArray().template(object);

        DslPart infoForGetProjectResponse = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .stringType("shortName")
                .array("environments").object().closeArray();

        DslPart object1 = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .stringType("graylogName")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .eachLike("systems").closeArray();

        DslPart infoForGetEnvironmentsResponse1 = new PactDslJsonArray().template(object1);

        DslPart object2 = new PactDslJsonBody()
                .integerType("created")
                .uuid("createdBy")
                .stringType("description")
                .stringType("graylogName")
                .uuid("id")
                .integerType("modified")
                .uuid("modifiedBy")
                .stringType("name")
                .uuid("projectId")
                .eachLike("systems").closeArray();

        DslPart infoForGetEnvironmentsResponse2 = new PactDslJsonArray().template(object2);

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /api/projects OK")
                .path("/api/projects")
                .query("full=false")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(objects)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId} OK")
                .path("/api/projects/7c9dafe9-2cd1-4ffc-ae54-45867f2b9702")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(infoForGetProjectResponse)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/environments OK")
                .path("/api/projects/7c9dafe9-2cd1-4ffc-ae54-45867f2b9702/environments")
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(infoForGetEnvironmentsResponse1)

                .given("all ok")
                .uponReceiving("GET /api/projects/{projectId}/temporary/environments OK")
                .path("/api/projects/7c9dafe9-2cd1-4ffc-ae54-45867f2b9702/temporary/environments")
                .query("full=true")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(headers)
                .body(infoForGetEnvironmentsResponse2);

        return response.toPact();
    }
}

