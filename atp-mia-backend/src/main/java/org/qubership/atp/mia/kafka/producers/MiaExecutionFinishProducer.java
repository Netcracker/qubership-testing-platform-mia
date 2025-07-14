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

package org.qubership.atp.mia.kafka.producers;

import java.util.Map;
import java.util.UUID;

import org.qubership.atp.mia.config.MiaConfiguration;
import org.qubership.atp.mia.kafka.configuration.KafkaConfiguration;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiaExecutionFinishProducer {

    private final KafkaTemplate<UUID, ExecutionResponse> miaExecutionFinishKafkaTemplate;
    @Getter
    private final boolean mock;
    @Value("${kafka.mia.execution.finish.topic}")
    public String miaExecutionFinishTopic;

    /**
     * Bean of producer creates in 2 places (depends on kafka enable or not).
     *  in {@link KafkaConfiguration}
     *  in {@link MiaConfiguration}
     * @param kafkaProducerConfigJson kafka producer config for Json
     */
    public MiaExecutionFinishProducer(Map<String, Object> kafkaProducerConfigJson) {
        mock = kafkaProducerConfigJson == null;
        if (!mock) {
            ProducerFactory<UUID, ExecutionResponse> producerFactory =
                    new DefaultKafkaProducerFactory<>(kafkaProducerConfigJson);
            this.miaExecutionFinishKafkaTemplate = new KafkaTemplate<>(producerFactory);
        } else {
            this.miaExecutionFinishKafkaTemplate = null;
        }
    }

    /**
     * Sends execution finish event to kafka.
     *
     * @param executionFinishEvent execution finish event
     */
    public boolean executionFinishEventSend(ExecutionResponse executionFinishEvent) {
        if (!mock && miaExecutionFinishKafkaTemplate != null) {
            log.debug("Send execution finish event for sseId = {}", executionFinishEvent.getSseId());
            miaExecutionFinishKafkaTemplate.send(
                    miaExecutionFinishTopic,
                    executionFinishEvent.getSseId(),
                    executionFinishEvent);
            return true;
        }
        return false;
    }
}
