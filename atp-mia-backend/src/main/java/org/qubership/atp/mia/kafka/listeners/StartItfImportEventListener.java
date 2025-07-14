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

import java.util.UUID;

import org.qubership.atp.mia.kafka.configuration.KafkaConfiguration;
import org.qubership.atp.mia.kafka.model.KafkaRequestImport;
import org.qubership.atp.mia.kafka.model.KafkaResponseImport;
import org.qubership.atp.mia.kafka.service.ItfImportFinishNotificationService;
import org.qubership.atp.mia.kafka.service.ItfImportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "kafka.enable", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class StartItfImportEventListener {

    private final ItfImportService exportService;
    private final ItfImportFinishNotificationService itfImportFinishNotificationService;

    /**
     * Listen start execution kafka topic.
     */
    @KafkaListener(
            id = "${kafka.itf.import.group}",
            groupId = "${kafka.itf.import.group}",
            topics = "${kafka.itf.import.topic}",
            containerFactory = KafkaConfiguration.ITF_IMPORT_CONTAINER_FACTORY,
            autoStartup = "true"
    )
    public void listen(@Payload KafkaRequestImport event) {
        log.info("Start ITF import by event from kafka [{}]", event);
        try {
            exportService.importRequest(event);
        } catch (Exception e) {
            final KafkaResponseImport response = exportService.generateErrorImportResponse(event, e);
            itfImportFinishNotificationService.notify(UUID.fromString(response.getId()), response);
        }
    }
}
