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

package org.qubership.atp.mia.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.config.ContextInterceptor;
import org.qubership.atp.mia.config.MiaConfiguration;
import org.qubership.atp.mia.controllers.api.dto.FlowConfigDto;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.executable.PotHeader;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.output.HtmlPage;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.Marker;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.repo.impl.ProofOfTestingRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.execution.CompoundService;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactUrl;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import lombok.extern.slf4j.Slf4j;

@Provider("atp-mia")
@PactUrl(urls = {"classpath:pacts/atp-logcollector-atp-mia.json"})
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(controllers = {MiaExecutionController.class, MiaConfigurationController.class})
@ContextConfiguration(classes = {MiaAndLogCollectorContractTest.TestApp.class})
@EnableAutoConfiguration
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        MiaConfiguration.class,
        ContextInterceptor.class,
        ProcessService.class,
        MiaExecutionController.class,
        MiaConfigurationController.class
})
@Slf4j
@Disabled
public class MiaAndLogCollectorContractTest {

    @MockBean
    ProofOfTestingRepository proofOfTestingRepository;

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    ContextRepository contextRepository;
    @Autowired
    private MiaConfiguration miaConfiguration;
    @MockBean
    private ProcessService processService;
    @MockBean
    private CompoundService compoundService;
    @MockBean
    private MiaContext miaContext;
    @MockBean
    private ProjectConfigurationService projectConfigurationService;
    @MockBean
    private MiaFileService miaFileService;
    @MockBean
    private ServletContext servletContext;

    public void beforeAll() {
        when(projectConfigurationService.getOldConfig(any(UUID.class))).thenReturn(createFlowConfig());
        LinkedList<ExecutionResponse> list = new LinkedList<>();
        list.add(createExecutionResponse());
        when(compoundService.executeCompound(any(), any())).thenReturn(list);
        when(processService.executeProcess(any(), any())).thenReturn(createExecutionResponse());
    }

    public CommandResponse createCommandResponse() {
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setCommand("command");
        commandResponse.setCheckStatusCodeFlag(true);
        commandResponse.setType(CommandResponse.CommandResponseType.REST);
        commandResponse.setCommandOutputs(createListCommandOutput());
        commandResponse.setPostScriptExecutionReport("post");
        commandResponse.setStatusCode("status");
        commandResponse.setErrors(new LinkedList<>());
        commandResponse.setSqlResponse(createSqlResponseDto());
        HtmlPage htmlPage = new HtmlPage("internal", "text", miaContext);
        commandResponse.setHtmlPage(htmlPage);
        return commandResponse;
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    public CommonConfiguration createCommonConfiguration() {
        return CommonConfiguration.builder().genevaDateMask("geneva").sshRsaFilePath("ssh")
                .defaultSystem("defaultSystem").commonVariables(new HashMap<String, String>()).build();
    }

    @State("all ok")
    public void allPass() {
    }

    public ExecutionResponse createExecutionResponse() {
        ExecutionResponse executionResponse = new ExecutionResponse();
        executionResponse.setProcessName("proc");
        executionResponse.setExecutedCommand("command");
        executionResponse.setCommandResponse(createCommandResponse());
        executionResponse.setPrerequisites(Collections.singletonList(createCommandResponse()));
        ProcessStatus processStatus = new ProcessStatus();
        processStatus.setStatus(Statuses.SUCCESS);
        processStatus.setMarker(Marker.builder().build());
        executionResponse.setProcessStatus(processStatus);
        return executionResponse;
    }

    public FlowConfigDto createFlowConfig() {
        return miaConfiguration.modelMapper().map(ProjectConfiguration.builder()
                .validationResult("validation")
                .headerConfiguration(createHeaderConfiguration())
                .commonConfiguration(createCommonConfiguration())
                .potHeaderConfiguration(createPotHeaderConfiguration())
                .sections(new LinkedList<>())
                .build(), FlowConfigDto.class).defaultSystem("defaultSystem");
    }

    public HeaderConfiguration createHeaderConfiguration() {
        return HeaderConfiguration.builder().switchers(Collections.singletonList(createSwitcher())).build();
    }

    public LinkedList<CommandOutput> createListCommandOutput() {
        LinkedList<CommandOutput> list = new LinkedList<>();
        list.add(new CommandOutput("asd", "asd", true, miaContext));
        return list;
    }

    public PotHeaderConfiguration createPotHeaderConfiguration() {
        return PotHeaderConfiguration.builder().headers(Collections.singletonList(new PotHeader("name", "type", "Billing System", "value"))).build();
    }

    public SqlResponse createSqlResponseDto() {
        SqlResponse sqlResponse = new SqlResponse();
        sqlResponse.setData(new DbTable(
                Collections.singletonList("q"), Collections.singletonList(Collections.singletonList("asd"))));
        sqlResponse.setTableName("tavble");
        sqlResponse.setQuery("query");
        sqlResponse.setDescription("descr");
        sqlResponse.setRecords(1);
        TableMarkerResult tableMarkerResult = new TableMarkerResult();
        tableMarkerResult.setTableRowCount("ER", "AR", Statuses.SUCCESS);
        tableMarkerResult.addColumnStatus("colName", Statuses.SUCCESS, "AV", "EV");
        sqlResponse.setTableMarkerResult(tableMarkerResult);
        sqlResponse.setInternalPathToFile("interna");
        sqlResponse.setLink(new Link("name", "asd"));
        sqlResponse.setSaveToWordFile(true);
        sqlResponse.setSaveToZipFile(true);
        return sqlResponse;
    }

    public Switcher createSwitcher() {
        return Switcher.builder().value(true).actionType("actionType").name("name").display("display")
                .actionTrue("actionTrue").actionFalse("actionFalse").build();
    }

    @Configuration
    public static class TestApp {
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        beforeAll();
        context.setTarget(new MockMvcTestTarget(mockMvc));
    }
}
