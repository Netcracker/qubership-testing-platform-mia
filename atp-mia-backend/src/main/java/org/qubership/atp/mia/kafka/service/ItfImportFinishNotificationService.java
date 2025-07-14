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

package org.qubership.atp.mia.kafka.service;

import java.util.UUID;

import org.qubership.atp.mia.kafka.model.KafkaResponseImport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@ConditionalOnProperty(value = "kafka.enable")
@Slf4j
public class ItfImportFinishNotificationService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<UUID, String> kafkaTemplate;
    private final String topicName;

    public ItfImportFinishNotificationService(String topicName, KafkaTemplate<UUID, String> kafkaTemplate) {
        this.topicName = topicName;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send response back to ITF lite.
     *
     * @param uuid         uuid of import request
     * @param importResult results of import
     */
    public void notify(UUID uuid, KafkaResponseImport importResult) {
        try {
            String payload = objectMapper.writeValueAsString(importResult);
            log.info("ITF import ended with ID #{} and payload {}", uuid, payload);
            kafkaTemplate.send(topicName, uuid, payload);
        } catch (Exception e) {
            log.error("ITF import notify error. Cannot put terminate event to kafka for {}", e);
        }
    }
}
