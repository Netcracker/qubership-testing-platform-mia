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

package org.qubership.atp.mia.service.configuration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.qubership.atp.mia.clients.api.environments.dto.projects.EnvironmentFullVer1ViewDto;
import org.qubership.atp.mia.clients.api.environments.dto.projects.EnvironmentsWithFilterRequestDto;
import org.qubership.atp.mia.clients.api.environments.dto.projects.FilterRequestDto;
import org.qubership.atp.mia.clients.api.environments.dto.projects.SystemEnvironmentsViewDto;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.IncorrectEnvironmentResponseException;
import org.qubership.atp.mia.model.CacheKeys;
import org.qubership.atp.mia.model.environment.AbstractConfiguratorModel;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.service.client.EnvironmentsFeignClient;
import org.qubership.atp.mia.service.client.ProjectsFeignClient;
import org.qubership.atp.mia.utils.converters.DtoConvertService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@RequiredArgsConstructor
public class EnvironmentsService {

    private final EnvironmentsFeignClient environmentsFeignClient;
    private final ProjectsFeignClient projectsFeignClient;
    private final DtoConvertService responseEntityConverter;
    private final EnvironmentsService self; // For caching

    /**
     * Get Environment by name.
     *
     * @param projectId project ID
     * @param name      Environment name
     * @return {@link Environment}
     */
    @Cacheable(value = CacheKeys.Constants.ENVIRONMENTS_BY_NAME_KEY, key = "#projectId.toString() + \"_\" + #name",
            condition = "#projectId!=null", sync = true)
    public Environment getEnvByName(UUID projectId, String name) {
        EnvironmentsWithFilterRequestDto requestDto = new EnvironmentsWithFilterRequestDto()
                .projectId(projectId)
                .addFieldsItem("id")
                .addFieldsItem("name")
                .addFieldsItem("systems")
                .filter(Collections.singletonList(new FilterRequestDto()
                        .name("name")
                        .addValueItem(name)));
        List<EnvironmentFullVer1ViewDto> foundEnvironments =
                environmentsFeignClient.getEnvironmentsByRequest(true, null, null, requestDto).getBody();
        return CollectionUtils.isEmpty(foundEnvironments) ? null : responseEntityConverter.convertList(
                foundEnvironments, Environment.class).get(0);
    }

    /**
     * Get list environments by ID.
     */
    @Cacheable(value = CacheKeys.Constants.ENVIRONMENTS_KEY, key = "#projectId", condition = "#projectId!=null")
    public List<AbstractConfiguratorModel> getEnvironmentsByProject(UUID projectId) {
        return responseEntityConverter.convertList(
                projectsFeignClient.getEnvironmentsShort(projectId).getBody(),
                AbstractConfiguratorModel.class);
    }

    /**
     * Get full information about environment.
     *
     * @param id ID of environment
     * @param projectId ID of project
     * @return Environment or RuntimeException if environment not found
     */
    @Cacheable(value = CacheKeys.Constants.ENVIRONMENTSFULL_KEY,
            key = "#projectId + \"_\" + #id", condition = "#id!=null")
    public Environment getEnvironmentsFull(UUID id, UUID projectId) {
        try {
            return responseEntityConverter.convert(
                    environmentsFeignClient.getEnvironment(id, true).getBody(),
                    Environment.class);
        } catch (Exception e) {
            throw new RuntimeException("Can't parse response environment with id: " + id, e);
        }
    }

    /**
     * Get project by ID.
     */
    public Project getProject(UUID projectId) {
        try {
            return responseEntityConverter.convert(projectsFeignClient.getShortProject(projectId, false).getBody(),
                    Project.class);
        } catch (Exception e) {
            throw new RuntimeException("Can't parse response for project with id: " + projectId, e);
        }
    }

    /**
     * Get projects.
     */
    @Cacheable(value = CacheKeys.Constants.MIA_PROJECTS_KEY, sync = true)
    public List<AbstractConfiguratorModel> getProjects() {
        try {
            final List<AbstractConfiguratorModel> projects = responseEntityConverter
                    .convertList(projectsFeignClient.getAllShort(false).getBody(),
                            AbstractConfiguratorModel.class);
            return projects;
        } catch (Exception e) {
            throw new IncorrectEnvironmentResponseException(e);
        }
    }

    /**
     * Get all systems for selected project.
     *
     * @param projectId of project.
     * @return list of systems for selected project.
     */
    @Cacheable(value = CacheKeys.Constants.SYSTEM_NAMES, key = "#projectId", condition = "#projectId!=null",
            sync = true)
    public List<SystemEnvironmentsViewDto> getSystemsForProject(UUID projectId) {
        return projectsFeignClient.getAllShortSystemsOnProject(projectId).getBody();
    }
}
