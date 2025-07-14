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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.executable.Rest;

@ExtendWith(SkipTestInJenkins.class)
public abstract class RestRepositoryTestConfiguration extends ConfigTestBean {

    protected ThreadLocal<RestRepository> repository = new ThreadLocal<>();
    protected ThreadLocal<Rest> rest = new ThreadLocal<>();
    protected ThreadLocal<Server> server = new ThreadLocal<>();

    static void createLogFile(String filename, String responseStr) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            bw.write(responseStr);
            bw.flush();
        }
    }

    @BeforeEach
    public void beforeRestRepositoryTestConfiguration() {
        // rest
        rest.set(Rest.builder()
                .endpoint("/CUSTOMECA/services/CUSTOMECAAXPaymentsPort")
                .method(":postMethod")
                .headers("soapaction:null\r\ncontent-type:text/xml;charset=UTF-8\r\n")
                .parseResponseAsTable(true)
                .body("test")
                .build());
        //server
        Connection testConnectionHttp = Connection.builder()
                .id(TEST_CONNECTION_HTTP_ID)
                .name(TEST_CONNECTION_HTTP_NAME)
                .sourceTemplateId(Connection.SourceTemplateId.HTTP.id)
                .systemId(TEST_SYSTEM_ID)
                .parameters(new HashMap<String, String>() {{
                    put("url", "http://localhost:8080");
                    put("login", "login");
                    put("password", "pass");
                }})
                .build();
        server.set(new Server(testConnectionHttp, testConnectionHttp.getName()));
    }
}
