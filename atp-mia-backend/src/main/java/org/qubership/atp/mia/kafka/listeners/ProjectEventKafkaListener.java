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

import static org.qubership.atp.mia.model.CacheKeys.Constants.PROJECTNAME_KEY;

import org.qubership.atp.mia.kafka.configuration.KafkaConfiguration;
import org.qubership.atp.mia.kafka.model.notification.ProjectEvent;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(value = "kafka.enable", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class ProjectEventKafkaListener {

    private final ProjectConfigurationService projectConfigService;

    /**
     * Listen request about changes for project.
     *
     * @param projectEvent vent of project
     */
    @Caching(put = {
            @CachePut(value = PROJECTNAME_KEY, key = "#projectEvent.projectId",
                    condition = "#projectEvent!=null&&#projectEvent.projectId!=null")
    })
    @KafkaListener(id = "${kafka.catalog.notification.group}",
            groupId = "${kafka.catalog.notification.group}",
            topics = "${kafka.catalog.notification.topic:catalog_notification_topic}",
            containerFactory = KafkaConfiguration.CATALOG_PROJECT_EVENT_CONTAINER_FACTORY,
            autoStartup = "true")
    public void listen(ProjectEvent projectEvent) {
        switch (projectEvent.getType()) {
            case CREATE:
                projectConfigService.getConfigByProjectId(projectEvent.getProjectId());
                break;
            case UPDATE: {
                ProjectConfiguration config = projectConfigService.getConfigByProjectId(projectEvent.getProjectId());
                config.setProjectName(projectEvent.getProjectName());
                try {
                    projectConfigService.updateProject(config, false);
                } catch (Exception e) {
                    log.error("'Update project' event is received but something went wrong during save", e);
                }
                break;
            }
            case DELETE: {
                projectConfigService.removeProject(projectEvent.getProjectId(), true);
                break;
            }
            default: {
                throw new RuntimeException("Unknown type of events " + projectEvent.getType());
            }
        }
    }
}

