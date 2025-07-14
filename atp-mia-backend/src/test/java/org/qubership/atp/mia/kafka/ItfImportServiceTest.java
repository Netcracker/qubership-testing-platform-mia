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

public class ItfImportServiceTest {
/*
    private ItfImportService importService;
    private FileConfigurationService configurationService;
    private LockManager lockManager;
    private ItfImportFinishNotificationService notificationService;

    @BeforeEach
    public void setUp() {
        configurationService = mock(FileConfigurationService.class);
        lockManager = new LockManager(10, 10, 10, new InMemoryLockProvider());
        notificationService = mock(ItfImportFinishNotificationService.class);
        importService = new ItfImportService(configurationService, lockManager, notificationService); //can
        // remove executorService object & also in actual class
    }

    @Test(expected = MiaException.class)
    public void importRequest_shouldSuccessfullySend() throws Exception {
        KafkaRequestImport req = prepareInputRequest("rest");
        ArgumentCaptor<String> reqFromCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UpdateConfigurationRequest> configCaptor = ArgumentCaptor.forClass(UpdateConfigurationRequest.class);
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        importService.importRequest(req);
    }

    @Test
    public void testValidate_shouldReturnTrue() throws Exception {
      boolean result = Whitebox.invokeMethod(importService, "validateRequestInputs", prepareInputRequest("rest"));
      assertTrue(result);
    }

    @Test
    public void testMapToProcess_shouldReturnRestProcess() throws Exception {
        KafkaRequestImport request = prepareInputRequest("rest");
        String processName = request.getMiaProcessName() + "_" + request.getId();
        Process result = Whitebox.invokeMethod(importService, "mapToProcess", request, processName);
        assertEquals( "Process", result.execType);
        assertEquals( processName, result.getName());
    }

    @Test
    public void testMapToProcess_shouldReturnSoapProcess() throws Exception {
        KafkaRequestImport request = prepareInputRequest("soap");
        String processName = request.getMiaProcessName() + "_" + request.getId();
        Process result = Whitebox.invokeMethod(importService, "mapToProcess", request, processName);
        assertEquals( "Process", result.execType);
        assertEquals( processName, result.getName());
    }

    @Test
    public void testSendResponse_responseSuccessfullySent() throws Exception {
        KafkaRequestImport request = prepareInputRequest("rest");
        String processName = request.getMiaProcessName() + "_" + request.getId();
        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<KafkaResponseImport> responseCaptor = ArgumentCaptor.forClass(KafkaResponseImport.class);

        String result = Whitebox.invokeMethod(importService, "sendResponseToItf", request, processName);
        verify(notificationService).notify(idCaptor.capture(), responseCaptor.capture());

        assertEquals( processName, result);
        assertEquals(request.getRequestDetails().getItfLiteId(), responseCaptor.getValue().getRequestId());
    }

    private KafkaRequestImport prepareInputRequest(String reqType) {
        KafkaRequestImport request = new KafkaRequestImport();
        KafkaRequestImportDetails requestImportDetails = new KafkaRequestImportDetails();
        requestImportDetails.setItfLiteId("itfId");
        requestImportDetails.setHttpMethod("GET");
        requestImportDetails.setUrl("url");
        if (!reqType.equals("rest")) {
            requestImportDetails.setTransportType(KafkaRequestImportDetails.KafkaRequestImportType.SOAP);
        } else {
            requestImportDetails.setTransportType(KafkaRequestImportDetails.KafkaRequestImportType.REST);
        }
        request.setId(UUID.randomUUID().toString());
        request.setProjectId("projectId");
        request.setMiaProcessName("processName");
        request.setMiaPath("miaPath");
        request.setRequestDetails(requestImportDetails);

        return request;
    }*/
}
