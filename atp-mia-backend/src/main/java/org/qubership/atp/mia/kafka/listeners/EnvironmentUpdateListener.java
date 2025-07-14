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
import org.qubership.atp.mia.kafka.model.notification.EnvironmentUpdateEvent;
import org.qubership.atp.mia.service.cache.MiaCacheService;
import org.qubership.atp.mia.service.execution.SshExecutionHelperService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "kafka.enable", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class EnvironmentUpdateListener {

    private final MiaCacheService miaCacheService;
    private final SshExecutionHelperService sshExecutionHelperService;
    private final CacheManager cacheManager;

    /**
     * Listen start for environment updates to clear cache.
     */
    @KafkaListener(
            id = "${kafka.env.update.listen.group}",
            groupId = "${kafka.env.update.listen.group}",
            topics = "${kafka.env.update.listen.topic}",
            containerFactory = KafkaConfiguration.ENVIRONMENT_UPDATE_EVENT_CONTAINER_FACTORY,
            autoStartup = "true"
    )
    public void listenToEnvironmentService(@Payload EnvironmentUpdateEvent event) {
        log.info("Environment Update Event Listened for project id : {}", event.getProjectId());
        miaCacheService.clearEnvironmentsCache(cacheManager, event.getProjectId());
        sshExecutionHelperService.resetCache();
    }
}
