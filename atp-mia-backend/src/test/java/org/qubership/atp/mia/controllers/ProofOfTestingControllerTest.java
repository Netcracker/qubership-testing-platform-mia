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

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.repo.impl.ProofOfTestingRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.wildfly.common.Assert;

@ExtendWith(SkipTestInJenkins.class)
@SpringBootTest(classes = {MiaProofOfTestingController.class}, properties = {"spring"
        + ".cloud.vault.enabled=false"})
public class ProofOfTestingControllerTest {

    @Autowired
    MiaProofOfTestingController miaProofOfTestingController;

    @MockBean
    MiaContext miaContext;

    @MockBean
    ProofOfTestingRepository proofOfTestingRepository;

    @Test
    public void getSessionTest() {
        Assert.assertNotNull(miaProofOfTestingController.startSession(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    public void saveProofOfTestingTest() {
        miaProofOfTestingController.saveProofOfTesting(UUID.randomUUID(), UUID.randomUUID(), null);
    }
/*
    private MockMvc mockMvc;

    private ProofOfTestingRepository potRepository;
    private RecordingSessionRepository recordingSessionRepository;
    private ContextRepository contextRepository;

    private RecordingSessionsService recordingSessionsService;
    private ProofOfTestingService potService;
    private ProofOfTestingController potController;
    private JacksonTester<ExecutionRequest> requestJacksonTester;

    private static final String projectId = "d0262bb2-ba9c-4ac8-8c83-dcea75fe62dc";
    private static final UUID defaultId = UUID.fromString("fe868c24-bc33-4bbd-aad6-f573cffd2dd0");

    @BeforeEach
    public void initController() {
        potRepository = Mockito.mock(ProofOfTestingRepository.class);
        recordingSessionsService = Mockito.mock(RecordingSessionsService.class);
        recordingSessionRepository = Mockito.mock(RecordingSessionRepository.class);
        contextRepository = Mockito.mock(ContextRepository.class);
        // not mocks
        JacksonTester.initFields(this, new ObjectMapper());
        recordingSessionsService = new RecordingSessionsService(recordingSessionRepository, contextRepository);
        potService = new ProofOfTestingService(recordingSessionsService, potRepository);
        potController = new ProofOfTestingController(potService);
        mockMvc = MockMvcBuilders.standaloneSetup(potController)
                .build();
    }

    // ------------------------ SAVE SESSION ----------------------------
    @Test
    public void saveSession_whenSessionNotStarted_thenExpectException() {
        final String oldSessionId = defaultId.toString();
        doThrow(new EmptyResultDataAccessException("error", 1)).when(recordingSessionRepository).deleteById(defaultId);
        String environmentName = mockEnvironmentNameInContext("DefaultName");
        mockRecordingSessionSave(environmentName);
        // when
        try {
            mockMvc.perform(post("/rest/pot/save")
                    .param("projectId", projectId)
                    .param("sessionId", oldSessionId)
                    .content(requestJacksonTester.write(ExecutionRequest.builder().build()).getJson())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON));
            Assert.fail("should throw error, that session not started.");
        } catch (Exception e) {
            // then
            MiaException err = (MiaException) e.getCause();
            Assert.assertEquals(ErrorCodes.MIA_2156_POT_SESSION_NOT_FOUND.getCode(), err.getErrorCode());
        }
    }

    @Test
    public void saveSession_whenSessionStarted_thenExpectSaveSession() throws Exception {
        final String oldSessionId = defaultId.toString();
        doThrow(new EmptyResultDataAccessException("error", 1)).when(recordingSessionRepository).deleteById(defaultId);
        String environmentName = mockEnvironmentNameInContext("DefaultName");
        mockRecordingSessionSave(environmentName);
        ExecutionRequest executionRequest = ExecutionRequest.builder().sessionId(defaultId).build();
        RecordingSession recordingSession = fillRecordingSession(defaultId, projectId, environmentName,
                fillSteps(3, "step"));
        when(recordingSessionsService.getSession(oldSessionId)).thenReturn(Optional.of(recordingSession));
        // when
        MockHttpServletResponse response = mockMvc.perform(
                        (post("/rest/pot/save")
                                .param("projectId", projectId)
                                .param("sessionId", oldSessionId)
                                .content(requestJacksonTester.write(executionRequest).getJson())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)))
                .andReturn().getResponse();
        Assert.assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    public String mockEnvironmentNameInContext(String envName) {
        Context context = Mockito.mock(Context.class);
        Environment environment = Mockito.mock(Environment.class);
        when(environment.getName()).thenReturn(envName);
        when(context.getEnvironment()).thenReturn(environment);
        when(contextRepository.getContext()).thenReturn(context);
        return envName;
    }

    public RecordingSession mockRecordingSessionSave(String environmentName) {
        return mockRecordingSessionSave(projectId, environmentName);
    }

    public RecordingSession mockRecordingSessionSave(String projectId, String environmentName) {
        RecordingSession recordingSession = new RecordingSession(projectId, environmentName);
        recordingSession.setId(defaultId);
        when(recordingSessionRepository.save(any())).thenReturn(recordingSession);
        return recordingSession;
    }*/
}
