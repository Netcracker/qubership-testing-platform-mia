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

package org.qubership.atp.mia.kafka;

import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.kafka.service.ItfImportFinishNotificationService;
import org.qubership.atp.mia.kafka.service.ItfImportService;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

public class ItfImportFinishNotificationServiceTest extends ConfigTestBean {

    private static ItfImportFinishNotificationService notificationService;
    @SpyBean
    private ItfImportService itfImportService;

    private String topicName;
    private KafkaTemplate<UUID, String> kafkaTemplate;

    @BeforeEach
    public void setUp() {
        topicName = "topic";
        kafkaTemplate = mock(KafkaTemplate.class);
        notificationService = new ItfImportFinishNotificationService(topicName, kafkaTemplate);
    }
/*
    @Test
    public void sendMiaErrorResponse_shouldSuccessfullySend() {
        UUID sessionId = UUID.randomUUID();
        KafkaResponseImport importResponse = itfImportService.generateErrorImportResponse(generateItfRequest(), new Exception());
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> sessionIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        notificationService.notify(sessionId, importResponse);
        verify(kafkaTemplate).send(topicNameCaptor.capture(), sessionIdCaptor.capture(), payloadCaptor.capture());
        assertEquals(topicName, topicNameCaptor.getValue());
        assertEquals(sessionId, sessionIdCaptor.getValue());
    }

    @Test
    public void sendMiaResponse_shouldSuccessfullySend() {
        UUID sessionId = UUID.randomUUID();
        KafkaResponseImport importResponse = itfImportService.generateFinishedImportResponse(generateItfRequest(),
                "testProcess_11");
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> sessionIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        notificationService.notify(sessionId, importResponse);
        verify(kafkaTemplate).send(topicNameCaptor.capture(), sessionIdCaptor.capture(), payloadCaptor.capture());
        assertEquals(topicName, topicNameCaptor.getValue());
        assertEquals(sessionId, sessionIdCaptor.getValue());
    }

    private static KafkaRequestImport generateItfRequest() {
        KafkaRequestImport request = new KafkaRequestImport();
        KafkaRequestImportDetails requestImportDetails = new KafkaRequestImportDetails();
        requestImportDetails.setItfLiteId("itfId");
        request.setId("test");
        request.setProjectId("projectId");
        request.setMiaProcessName("itfResponse");
        request.setMiaPath("miaPath");
        request.setRequestDetails(requestImportDetails);
        return request;
    }

 */
}
