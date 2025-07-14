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

package org.qubership.atp.mia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.qubership.atp.mia.TestConstants.JDBC_URL;
import static org.qubership.atp.mia.TestConstants.SQL_HOST;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN_VALUE;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD_VALUE;
import static org.qubership.atp.mia.TestConstants.SSH_HOST;
import static org.qubership.atp.mia.TestConstants.SSH_LOGIN;
import static org.qubership.atp.mia.TestConstants.SSH_LOGIN_VALUE;
import static org.qubership.atp.mia.TestConstants.SSH_PASSWORD;
import static org.qubership.atp.mia.TestConstants.SSH_PASSWORD_VALUE;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_NAME;
import static org.qubership.atp.mia.model.Constants.DEFAULT_SESSION_ID;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.mia.component.QueryDriverFactory;
import org.qubership.atp.mia.config.MacrosConfiguration;
import org.qubership.atp.mia.config.MiaConfiguration;
import org.qubership.atp.mia.config.SseProperties;
import org.qubership.atp.mia.kafka.producers.MiaExecutionFinishProducer;
import org.qubership.atp.mia.model.configuration.CommandPrefix;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.repo.configuration.CompoundConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.DirectoryConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.FileConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.ProcessConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.ProjectConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.SectionConfigurationRepository;
import org.qubership.atp.mia.repo.db.RecordingSessionRepository;
import org.qubership.atp.mia.repo.driver.CassandraDriver;
import org.qubership.atp.mia.repo.driver.OracleDriver;
import org.qubership.atp.mia.repo.driver.PostgreSqlDriver;
import org.qubership.atp.mia.repo.gridfs.GridFsRepository;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.SseEmitterService;
import org.qubership.atp.mia.service.configuration.CompoundConfigurationService;
import org.qubership.atp.mia.service.configuration.DirectoryConfigurationService;
import org.qubership.atp.mia.service.configuration.EnvironmentsService;
import org.qubership.atp.mia.service.configuration.FileConfigurationService;
import org.qubership.atp.mia.service.configuration.ProcessConfigurationService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.configuration.SectionConfigurationService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

public class ConfigTestBean {

    public static final String TEST_ENVIRONMENT_NAME = "testEnv";
    public static final UUID TEST_ENVIRONMENT_ID = new UUID(1, 1);
    public static final String TEST_SYSTEM_NAME = "Billing System";
    public static final UUID TEST_SYSTEM_ID = new UUID(2, 1);
    public static final String TEST_CONNECTION_SSH_NAME = Connection.SourceTemplateId.SSH.name();
    public static final UUID TEST_CONNECTION_SSH_ID = new UUID(3, 1);
    public static final String TEST_CONNECTION_DB_NAME = Connection.SourceTemplateId.DB.name();
    public static final UUID TEST_CONNECTION_DB_ID = new UUID(3, 2);
    public static final String TEST_CONNECTION_HTTP_NAME = Connection.SourceTemplateId.HTTP.name();
    public static final UUID TEST_CONNECTION_HTTP_ID = new UUID(3, 3);
    public static String SwitcherSQL1 = "Uncomment/comment Sysdatevalue";
    public static String SwitcherSQL2 = "comment SysdateOverride";

    protected static final ThreadLocal<UUID> projectId = new ThreadLocal();
    protected static final ThreadLocal<EnvironmentsService> environmentsService = new ThreadLocal();
    protected static final ThreadLocal<MiaContext> miaContext = new ThreadLocal();
    protected static final ThreadLocal<ProjectConfigurationRepository> projectConfigurationRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<ProcessConfigurationRepository> processConfigurationRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<CompoundConfigurationRepository> compoundConfigurationRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<SectionConfigurationRepository> sectionConfigurationRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<DirectoryConfigurationRepository> directoryConfigurationRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<FileConfigurationRepository> fileConfigurationRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<RecordingSessionRepository> recordingSessionRepository =  new ThreadLocal<>();
    protected static final ThreadLocal<ProjectConfigurationService> projectConfigurationService = new ThreadLocal();
    protected static final ThreadLocal<ProcessConfigurationService> processConfigurationService =  new ThreadLocal<>();
    protected static final ThreadLocal<CompoundConfigurationService> compoundConfigurationService =  new ThreadLocal<>();
    protected static final ThreadLocal<SectionConfigurationService> sectionConfigurationService =  new ThreadLocal<>();
    protected static final ThreadLocal<DirectoryConfigurationService> directoryConfigurationService =  new ThreadLocal<>();
    protected static final ThreadLocal<FileConfigurationService> fileConfigurationService =  new ThreadLocal<>();
    protected static final ThreadLocal<SseEmitterService> sseEmitterService =  new ThreadLocal<>();
    protected static final ThreadLocal<MiaExecutionFinishProducer> kafkaExecutionFinishProducer =  new ThreadLocal<>();
    protected static final ThreadLocal<ProjectConfiguration> testProjectConfiguration = new ThreadLocal();
    protected static final ThreadLocal<Decryptor> decryptor = new ThreadLocal();
    protected static final ThreadLocal<Encryptor> encryptor = new ThreadLocal();
    protected static final ThreadLocal<GridFsRepository> gridFsRepository = new ThreadLocal<>();
    protected static final ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();
    protected static final ThreadLocal<MiaFileService> miaFileService = new ThreadLocal<>();
    protected static final ThreadLocal<FlowData> FLOW_DATA_TEST = new ThreadLocal();
    protected static final ThreadLocal<ExecutionRequest> EXECUTION_REQUEST_TEST = new ThreadLocal();
    protected static final ThreadLocal<Connection> testConnectionSsh = new ThreadLocal();
    protected static final ThreadLocal<Connection> testConnectionDb = new ThreadLocal();
    protected static final ThreadLocal<Connection> testConnectionHttp = new ThreadLocal();
    protected static final ThreadLocal<System> testSystem = new ThreadLocal();
    protected static final ThreadLocal<Environment> testEnvironment = new ThreadLocal();
    protected static final ThreadLocal<Project> testProject = new ThreadLocal();
    protected static final ThreadLocal<QueryDriverFactory> queryDriverFactory = new ThreadLocal();
    protected static final ThreadLocal<CassandraDriver> cassandraDriver = new ThreadLocal();
    protected static final ThreadLocal<OracleDriver> oracleDriver = new ThreadLocal();
    protected static final ThreadLocal<PostgreSqlDriver> postgreSqlDriver = new ThreadLocal();

    protected MiaConfiguration miaConfiguration = spy(new MiaConfiguration());
    protected ModelMapper modelMapper = miaConfiguration.modelMapper();
    @MockBean
    protected Decryptor decryptorService;
    @MockBean
    protected Encryptor encryptorService;
    @MockBean
    protected Provider provider;
    @SpyBean
    protected CryptoUtils cryptoUtils;
    @SpyBean
    protected ContextRepository contextRepository;
    @SpyBean
    protected MacrosConfiguration macrosConfiguration;
    @SpyBean
    protected AtpUserService atpUserService;
    @SpyBean
    protected SseProperties sseProperties;
    @MockBean
    protected MetricsAggregateService metricsService;

    @BeforeEach
    public void beforeEach() throws AtpDecryptException, AtpEncryptException {
        projectId.set(UUID.randomUUID());
        processConfigurationRepository.set(mock(ProcessConfigurationRepository.class));
        compoundConfigurationRepository.set(mock(CompoundConfigurationRepository.class));
        sectionConfigurationRepository.set(mock(SectionConfigurationRepository.class));
        directoryConfigurationRepository.set(mock(DirectoryConfigurationRepository.class));
        fileConfigurationRepository.set(mock(FileConfigurationRepository.class));
        projectConfigurationService.set(mock(ProjectConfigurationService.class));
        processConfigurationService.set(spy(new ProcessConfigurationService(
                modelMapper, processConfigurationRepository.get(), projectConfigurationService.get())));
        compoundConfigurationService.set(spy(new CompoundConfigurationService(
                compoundConfigurationRepository.get(), modelMapper, projectConfigurationService.get())));
        sectionConfigurationService.set(spy(new SectionConfigurationService(
                modelMapper, projectConfigurationService.get(), sectionConfigurationRepository.get())));
        fileConfigurationService.set(spy(new FileConfigurationService(
                atpUserService, fileConfigurationRepository.get(), gridFsService.get(), miaFileService.get(), modelMapper,
                projectConfigurationService.get())));
        directoryConfigurationService.set(spy(new DirectoryConfigurationService(
                directoryConfigurationRepository.get(), gridFsService.get(), modelMapper,
                projectConfigurationService.get(), fileConfigurationService.get())));
        environmentsService.set(mock(EnvironmentsService.class));
        decryptor.set(mock(Decryptor.class));
        encryptor.set(mock(Encryptor.class));
        miaContext.set(spy(new MiaContext(projectConfigurationService.get(),
                contextRepository,
                ".." + File.separator + "atp-mia-distribution" + File.separator + "src" + File.separator + "main"
                        + File.separator + "resources" + File.separator + "POT_template.docx",
                environmentsService.get())));
        gridFsRepository.set(mock(GridFsRepository.class));
        gridFsService.set(spy(new GridFsService(gridFsRepository.get(), miaContext.get())));
        miaFileService.set(spy(new MiaFileService(gridFsService.get(), miaContext.get(), projectConfigurationService.get())));
        when(decryptor.get().decryptIfEncrypted(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (String) args[0];
            }
        });
        when(encryptor.get().encrypt(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (String) args[0];
            }
        });
        cryptoUtils.initForTest(decryptor.get(), encryptor.get());
        testConnectionSsh.set(createTestConnectionSsh());
        testConnectionDb.set(createTestConnectionDb());
        testConnectionHttp.set(createTestConnectionHttp());
        testSystem.set(createTestSystem());
        testEnvironment.set(createTestEnvironment());
        testProject.set(createTestProject());
        when(environmentsService.get().getEnvironmentsByProject(projectId.get()))
                .thenReturn(Collections.singletonList(testEnvironment.get()));
        when(environmentsService.get().getEnvironmentsFull(testEnvironment.get().getId(), projectId.get()))
                .thenReturn(testEnvironment.get());
        when(environmentsService.get().getEnvByName(any(), any())).thenReturn(testEnvironment.get());
        when(environmentsService.get().getProjects()).thenReturn(Collections.singletonList(testProject.get()));
        when(environmentsService.get().getProject(eq(projectId.get()))).thenReturn(testProject.get());
        testProjectConfiguration.set(ProjectConfiguration.builder()
                .projectId(projectId.get())
                .projectName(DEFAULT_PROJECT_NAME)
                .gitUrl("url")
                .lastLoadedWhen(LocalDateTime.now().minusYears(1))
                .headerConfiguration(HeaderConfiguration.builder()
                        .switchers(Arrays.asList(
                                Switcher.builder()
                                        .value(false)
                                        .actionType("SQL")
                                        .name(SwitcherSQL1)
                                        .display("Sysdatevalue Uncomment/comment")
                                        .actionTrue("update gparams set name='SYSdateValue' where name='#SYSdateValue'")
                                        .build(),
                                Switcher.builder()
                                        .value(true)
                                        .actionType("SQL")
                                        .name(SwitcherSQL2)
                                        .display("SysdateOverride comment")
                                        .actionFalse("update gparams set name='SYSdateoverride' where name='#SYSdateOverride'")
                                        .build()
                        ))
                        .build())
                .commonConfiguration(CommonConfiguration.builder()
                        .commandShellPrefixes(Arrays.asList(CommandPrefix.builder()
                                .system(testSystem.get().getName())
                                .prefixes(new LinkedHashMap<String, String>() {{
                                    put("accountNumber", "echo \"Something with accountNumber %s\"");
                                    put("genevaDate", "echo \"Something with genevaDate %s\"");
                                    put("infinys_root", "cd %s\nsource infinys.env");
                                    put("workingDirectory", "mkdir -p %s");
                                    put("exportGenevaDate", "export GENEVA_FIXEDDATE=\":genevaDate\"");
                                    put("fullTraceMode", "export TRACE_LEVEL=FULL\nexport TRACE_ALL=ON");
                                }})
                                .build()))
                        .build())
                .build());
        when(projectConfigurationService.get().getConfigByProjectId(eq(projectId.get())))
                .thenReturn(testProjectConfiguration.get());
        when(projectConfigurationService.get().getConfiguration(eq(projectId.get())))
                .thenReturn(testProjectConfiguration.get());
        when(projectConfigurationService.get().getProjectPathWithType(any(), any(), any())).thenCallRealMethod();
        FLOW_DATA_TEST.set(new FlowData(projectId.get(), DEFAULT_PROJECT_NAME, DEFAULT_SESSION_ID));
        FLOW_DATA_TEST.get().setEnvironment(testEnvironment.get());
        FLOW_DATA_TEST.get().setParameters(null);
        EXECUTION_REQUEST_TEST.set(ExecutionRequest.builder().flowData(FLOW_DATA_TEST.get()).build());
        miaContext.get().setContext(EXECUTION_REQUEST_TEST.get(), projectId.get(), TEST_ENVIRONMENT_NAME);
        cassandraDriver.set(spy(new CassandraDriver(miaContext.get())));
        oracleDriver.set(spy(new OracleDriver(miaConfiguration.executorServiceForSql(1, 1, 30000L))));
        postgreSqlDriver.set(spy(new PostgreSqlDriver(miaConfiguration.executorServiceForSql(1, 1, 30000L))));
        queryDriverFactory.set(new QueryDriverFactory(Arrays.asList(cassandraDriver.get(), oracleDriver.get(), postgreSqlDriver.get())));
        kafkaExecutionFinishProducer.set(mock(MiaExecutionFinishProducer.class));
        sseEmitterService.set(spy(new SseEmitterService(atpUserService, kafkaExecutionFinishProducer.get(), null, sseProperties)));
    }

    private Connection createTestConnectionSsh() {
        return Connection.builder()
                .id(TEST_CONNECTION_SSH_ID)
                .name(TEST_CONNECTION_SSH_NAME)
                .sourceTemplateId(Connection.SourceTemplateId.SSH.id)
                .systemId(TEST_SYSTEM_ID)
                .parameters(new HashMap<String, String>() {{
                    put(SSH_HOST, "localhost:22");
                    put(SSH_LOGIN, SSH_LOGIN_VALUE);
                    put(SSH_PASSWORD, SSH_PASSWORD_VALUE);
                }})
                .build();
    }

    private Connection createTestConnectionDb() {
        return Connection.builder()
                .id(TEST_CONNECTION_DB_ID)
                .name(TEST_CONNECTION_DB_NAME)
                .sourceTemplateId(Connection.SourceTemplateId.DB.id)
                .systemId(TEST_SYSTEM_ID)
                .parameters(new HashMap<String, String>() {{
                    put(SQL_HOST, java.lang.System.getProperty("POSTGRES_IP"));
                    put(JDBC_URL, "jdbc:postgresql://" + java.lang.System.getProperty("POSTGRES_IP") + ":5432/mia");
                    put(SQL_LOGIN, SQL_LOGIN_VALUE);
                    put(SQL_PASSWORD, SQL_PASSWORD_VALUE);
                }})
                .build();
    }

    private Connection createTestConnectionHttp() {
        return Connection.builder()
                .id(TEST_CONNECTION_HTTP_ID)
                .name(TEST_CONNECTION_HTTP_NAME)
                .sourceTemplateId(Connection.SourceTemplateId.HTTP.id)
                .systemId(TEST_SYSTEM_ID)
                .parameters(new HashMap<String, String>() {{
                    put("http_host", "http://localhost:8080");
                    put("url", "http://localhost:8080");
                }})
                .build();
    }

    private System createTestSystem() {
        return System.builder()
                .id(TEST_SYSTEM_ID)
                .name(TEST_SYSTEM_NAME)
                .environmentId(TEST_ENVIRONMENT_ID)
                .connections(Arrays.asList(testConnectionSsh.get(), testConnectionDb.get(), testConnectionHttp.get()))
                .build();
    }

    private Environment createTestEnvironment() {
        return Environment.builder()
                .projectId(projectId.get())
                .id(TEST_ENVIRONMENT_ID)
                .name(TEST_ENVIRONMENT_NAME)
                .systems(Collections.singletonList(testSystem.get()))
                .build();
    }

    private Project createTestProject() {
        return Project.builder()
                .id(projectId.get())
                .name(DEFAULT_PROJECT_NAME)
                .environments(Collections.singletonList(testEnvironment.get().getId()))
                .build();
    }
}
