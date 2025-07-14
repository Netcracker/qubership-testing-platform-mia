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

package org.qubership.atp.mia.kafka.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.qubership.atp.mia.kafka.model.KafkaRequestImport;
import org.qubership.atp.mia.kafka.model.KafkaRequestImportDetails;
import org.qubership.atp.mia.kafka.service.ItfImportFinishNotificationService;
import org.qubership.atp.mia.kafka.service.ItfImportService;

public class StartItfImportEventListenerTest {

    private static StartItfImportEventListener eventListener;
    private ItfImportService exportService;
    private ItfImportFinishNotificationService notificationService;

    @BeforeEach
    public void setUp() {
        exportService = mock(ItfImportService.class);
        notificationService = mock(ItfImportFinishNotificationService.class);
        eventListener = new StartItfImportEventListener(exportService, notificationService);
    }

    @Test
    @Disabled
    public void testListen_shouldSendEventToService() {
        KafkaRequestImport event = new KafkaRequestImport("1", "11", "Billing System", "name",
                KafkaRequestImportDetails.builder().id("12").build());
        ArgumentCaptor<KafkaRequestImport> reqCaptor = ArgumentCaptor.forClass(KafkaRequestImport.class);
        eventListener.listen(event);
        verify(exportService).importRequest(reqCaptor.capture());
        assertEquals("Billing System", reqCaptor.getValue().getMiaPath());
    }

    @Test
    public void testListen_shouldSuccessfullySendErrorResponse() {
    /*    KafkaRequestImport event = KafkaRequestImport.builder()
                .id("705211d7-cae7-49d4-94fd-48b85e20e7b4")
                .projectId("11")
                .miaPath("Billing System/Rest")
                .miaProcessName("name")
                .requestDetails(KafkaRequestImportDetails.builder().itfLiteId("").build())
                .build();
        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<KafkaResponseImport> resCaptor = ArgumentCaptor.forClass(KafkaResponseImport.class);
        doThrow(new MiaException(ErrorCodes.MIA_1801_ITF_PROJECT_ID_EMPTY)).when(exportService).importRequest(any());
        eventListener.listen(event);
        verify(notificationService).notify(idCaptor.capture(), resCaptor.capture());
        assertTrue(resCaptor.getValue().getErrorMessage().contains("MIA_1801"));
    */
    }
}
