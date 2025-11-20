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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.qubership.atp.mia.TestConstants.JDBC_URL;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN_VALUE;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD_VALUE;
import static org.qubership.atp.mia.TestConstants.SSH_HOST;
import static org.qubership.atp.mia.integration.utils.TestUtils.getSshTestParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Spy;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.Main;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.integration.environment.IntegrationCassandraContainerEnvironment;
import org.qubership.atp.mia.integration.environment.IntegrationMongoContainerEnvironment;
import org.qubership.atp.mia.integration.environment.IntegrationPostgresEnvironment;
import org.qubership.atp.mia.integration.environment.IntegrationSshServerEnvironment;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.repo.gridfs.GridFsRepository;
import org.qubership.atp.mia.repo.impl.SshConnectionManager;
import org.qubership.atp.mia.repo.impl.SshSession;
import org.qubership.atp.mia.repo.impl.pool.ssh.SshSessionPool;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.configuration.EnvironmentsService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

//@Disabled("Temporarily disabled for refactoring")
@ExtendWith(SkipTestInJenkins.class)
@SpringBootTest(classes = {Main.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application.properties"},
        properties = {"management.server.port=0", "gridfs.enable=true", "spring.cloud.vault.enabled=false",
        "db.execution.timeout=30"})
@ContextConfiguration(initializers = BaseIntegrationTestConfiguration.CustomInitializer.class)
@Isolated
public abstract class BaseIntegrationTestConfiguration extends ConfigTestBean {

    protected static final String PROCESS_END_POINT = "/rest/flow/execute/process";
    protected final static String mongoTest_path = "./src/test/config/";
    protected final static String mongoTest_filename = "mongosmoke.txt";
    public static IntegrationMongoContainerEnvironment mongoContainerEnvironment;
    public static IntegrationSshServerEnvironment integrationContainerEnvironment;
    public static IntegrationPostgresEnvironment integrationPostgresEnvironment;
    public static IntegrationCassandraContainerEnvironment cassandraContainerEnvironment;
    @MockBean
    public EnvironmentsService environmentsService;
    @Autowired
    protected WebClient webClient;
    @LocalServerPort
    private int localMiaPort;
    @SpyBean
    protected MiaContext miaContext;
    @SpyBean
    protected MiaFileService miaFileService;
    protected static String sshServerHost;
    protected static int sshPort;
    protected static String postgresJdbcUrl;
    private static Map<String, String> initDbParameters;
    private static Map<String, String> initSshParameters;
    @SpyBean
    protected GridFsService gridFsService;
    @SpyBean
    protected GridFsRepository gridFsRepository;
    @Spy
    SshSessionPool SshSessionPool = new SshSessionPool("300", "30000", miaContext);

    @BeforeAll
    public static void beforeAll() {
        mongoContainerEnvironment = IntegrationMongoContainerEnvironment.getInstance();
        integrationContainerEnvironment = IntegrationSshServerEnvironment.getInstance();
        integrationPostgresEnvironment = IntegrationPostgresEnvironment.getInstance();
        cassandraContainerEnvironment = IntegrationCassandraContainerEnvironment.getInstance();
    }

    @AfterEach
    public void afterIntegrationClass() {
        testConnectionSsh.get().setParameters(initSshParameters);
        testConnectionDb.get().setParameters(initDbParameters);
    }

    @BeforeEach
    public void beforeIntegrationClass() {
        when(environmentsService.getEnvironmentsByProject(projectId.get()))
                .thenReturn(Collections.singletonList(testEnvironment.get()));
        when(environmentsService.getEnvironmentsFull(testEnvironment.get().getId(),
                testEnvironment.get().getProjectId())).thenReturn(testEnvironment.get());
        when(environmentsService.getEnvByName(any(), any())).thenReturn(testEnvironment.get());
        when(environmentsService.getProjects()).thenReturn(Collections.singletonList(testProject.get()));
        when(environmentsService.getProject(eq(projectId.get()))).thenReturn(testProject.get());
        miaContext.setContext(EXECUTION_REQUEST_TEST.get(), projectId.get(), TEST_ENVIRONMENT_NAME);
        sshServerHost = integrationContainerEnvironment.getSshServerIp();
        sshPort = integrationContainerEnvironment.getSshServerPort();
        initSshParameters = testConnectionSsh.get().getParameters();
        testConnectionSsh.get().getParameters().put(SSH_HOST, sshServerHost + ":" + sshPort);
        postgresJdbcUrl = integrationPostgresEnvironment.getJdbcUrl();
        initDbParameters = testConnectionDb.get().getParameters();
        testConnectionDb.get().setParameters(
                new HashMap<String, String>() {{
                    put(JDBC_URL, postgresJdbcUrl);
                    put(SQL_LOGIN, SQL_LOGIN_VALUE);
                    put(SQL_PASSWORD, SQL_PASSWORD_VALUE);
                }}
        );
        webClient = WebClient.builder().baseUrl("http://localhost:" + localMiaPort).build();
    }

    protected WebClient getWebClient() {
        return WebClient.builder().baseUrl("http://localhost:" + localMiaPort).build();
    }

    public SshConnectionManager getSshConnectionManager() {
        //System.out.println("SSH server will have host:[" + sshServerHost + "], port[" + sshPort + "]");
        SshSession session = new SshSession(
                new Server(Connection.builder().parameters(getSshTestParams(sshServerHost + ":" + sshPort)).build(), "ssh"),
                miaContext.getCommonConfiguration());
        return new SshConnectionManager(session, "", miaContext);
    }

    static class CustomInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of("gridfs.host=" + mongoContainerEnvironment.getMongoAddress())
                    .applyTo(configurableApplicationContext.getEnvironment());
            TestPropertyValues.of("gridfs.port=" + mongoContainerEnvironment.getMongoPort())
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    /**
     * Create request with default connection timeouts - 60 min.
     */
    protected ExecutionRequest createSshRequest(String process, String command) {
        int timeout = 60 * 60 * 1000;
        return createSshRequest(process, command, getSshTestParams(sshServerHost + ":" + sshPort, timeout, timeout));
    }

    /**
     * Create request with custom connection timeouts,
     * which set in params map.
     */
    protected ExecutionRequest createSshRequest(String process, String command, Map<String, String> params) {
        testConnectionSsh.get().setParameters(params);
        // fill request data
        miaContext.getFlowData().addParameter(Constants.CustomParameters.WORKING_DIRECTORY, "/tmp/TA");
        if (!miaContext.getFlowData().getParameters().containsKey("sshPort")) {
            miaContext.getFlowData().addParameter("sshPort", String.valueOf(integrationContainerEnvironment.getSshServerPort()));
        }
        return ExecutionRequest.builder()
                .command(command)
                .process(process)
                .flowData(miaContext.getFlowData())
                .build();
    }
}
