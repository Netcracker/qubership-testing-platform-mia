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

import org.qubership.atp.mia.kafka.configuration.KafkaConfiguration;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.service.SseEmitterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(
        value = "kafka.enable",
        havingValue = "true"
)
public class MiaExecutionFinishListener {

    private static final String KAFKA_MIA_EXECUTION_FINISH_RESPONSE_LISTENER_ID =
            "miaExecutionFinishListener";
    private final SseEmitterService sseEmitterService;

    /**
     * Listen start execution kafka topic.
     */
    @KafkaListener(
            groupId = KAFKA_MIA_EXECUTION_FINISH_RESPONSE_LISTENER_ID
                    + "_#{T(org.qubership.atp.mia.utils.PodNameUtils).getServicePodName()}",
            topics = "${kafka.mia.execution.finish.topic}",
            containerFactory = KafkaConfiguration.MIA_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME
    )
    @Transactional
    public void listenItfLiteExecutionFinishEvent(@Payload ExecutionResponse payload) {
        log.debug("Start mia execution processing by event from kafka for sseId {}]", payload.getSseId());
        // check if current sseEmitters map has sseEmitter with key = sseId
        SseEmitter sseEmitter = sseEmitterService.getEmitter(payload.getSseId());
        if (sseEmitter != null) {
            sseEmitterService.sendEventWithExecutionResult(sseEmitter, payload);
        }
    }
}
