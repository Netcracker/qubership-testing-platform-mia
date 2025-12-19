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

package org.qubership.atp.mia.repo.migration.v3;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.qubership.atp.mia.config.BeanAwareSpringLiquibase;
import org.qubership.atp.mia.exceptions.configuration.DeserializeJsonConfigFailedException;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.IncorrectEnvironmentResponseException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectsJson;
import org.qubership.atp.mia.model.environment.AbstractConfiguratorModel;
import org.qubership.atp.mia.service.configuration.EnvironmentsService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadProjectsFromJson implements CustomTaskChange {

    private ProjectConfigurationService projectConfigurationService;
    private EnvironmentsService environmentsService;
    private Path miaConfigPath;

    {
        try {
            environmentsService = BeanAwareSpringLiquibase.getBean(EnvironmentsService.class);
            projectConfigurationService = BeanAwareSpringLiquibase.getBean(ProjectConfigurationService.class);
            miaConfigPath = BeanAwareSpringLiquibase.getBean(Path.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(Database database) {
        String originalThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName("atp-mia-migration-v3");
        try {
            projectConfigurationService.findByProjectId(new UUID(0, 0)); //correct initialize service
        } catch (Exception e) {
            //ignore
        }
        try {
            List<AbstractConfiguratorModel> projects = environmentsService.getProjects();
            Set<ProjectsJson> projectsJsonList = new ObjectMapper().readValue(
                            miaConfigPath.resolve("project").resolve("projects_config.json").toFile(),
                            new TypeReference<Set<ProjectsJson>>() {
                            })
                    .stream().filter(p -> {
                        try {
                            UUID projectId = UUID.fromString(p.getId());
                            if (p.getConfigUrl() == null) {
                                log.error("Project with ID '{}': Config Url not defined! Skip migration", p.getId());
                                return false;
                            }
                            Optional<AbstractConfiguratorModel> project = projects.stream()
                                    .filter(proj -> proj.getId().equals(projectId))
                                    .findFirst();
                            if (project.isPresent()) {
                                p.setName(project.get().getName());
                            } else {
                                log.error("Project with ID '{}' not found", p.getId());
                                return false;
                            }
                            return true;
                        } catch (IllegalArgumentException e) {
                            log.error("Project with ID '{}' can't be loaded", p.getId());
                            return false;
                        }
                    })
                    .collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(projectsJsonList)) {
                ExecutorService threadPoolForProjects = Executors.newFixedThreadPool(projectsJsonList.size());
                List<Future<ProjectsJson>> futures = new ArrayList<>();
                projectsJsonList.forEach(projectJson -> futures.add(threadPoolForProjects.submit(
                        new LoadProjectCallable(projectJson, projectConfigurationService))));
                threadPoolForProjects.shutdown();
                boolean successExecution = true;
                for (Future<ProjectsJson> future : futures) {
                    try {
                        ProjectsJson resultExecution = future.get(3, TimeUnit.MINUTES);
                        successExecution &= resultExecution.isSuccessfulLoad();
                        if (resultExecution.isSuccessfulLoad()) {
                            log.info("Project with ID '{}' and name '{}' has been successful loaded from git URL '{}'",
                                    resultExecution.getId(), resultExecution.getName(), resultExecution.getConfigUrl());
                        } else {
                            log.error("Project with ID '{}' and name '{}' load fail  from git URL '{}' with reason: {}",
                                    resultExecution.getId(), resultExecution.getName(), resultExecution.getConfigUrl(),
                                    resultExecution.getException().getMessage());
                        }
                    } catch (InterruptedException e) {
                        log.error("LoadProjectsFromJson InterruptedException", e);
                        successExecution = false;
                    } catch (ExecutionException e) {
                        log.error("LoadProjectsFromJson ExecutionException", e);
                        successExecution = false;
                    } catch (TimeoutException e) {
                        log.error("LoadProjectsFromJson aborted by timeout");
                        successExecution = false;
                    }
                }
                if (!successExecution) {
                    throw new DeserializeJsonConfigFailedException("Not at all projects has been loaded from "
                            + "projects_config.json", "");
                }
            } else {
                log.warn("Nothing to load from projects_config.json");
            }
        } catch (IncorrectEnvironmentResponseException e) {
            if (e.getMessage().contains("MIA-5006")) {
                log.error("{} Skip that step", e.getMessage());
            }
        } catch (IOException e) {
            throw new DeserializeJsonConfigFailedException("Fail load projects_config.json", e.getMessage());
        } finally {
            Thread.currentThread().setName(originalThreadName);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public void setUp() {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }

    @RequiredArgsConstructor
    class LoadProjectCallable implements Callable<ProjectsJson> {

        private final ProjectsJson projectJson;

        private final ProjectConfigurationService projectConfigurationService;

        @Override
        public ProjectsJson call() throws Exception {
            UUID projectId = UUID.fromString(projectJson.getId());
            Thread.currentThread().setName("atp-mia-migration-v3-" + projectId);
            log.info("Load from json for project {}", projectId);
            try {
                Optional<ProjectConfiguration> projectConfigurationOptional =
                        projectConfigurationService.findByProjectId(projectId);
                ProjectConfiguration projectConfiguration = projectConfigurationOptional.isPresent()
                        ? projectConfigurationOptional.get()
                        : ProjectConfiguration.builder()
                        .projectId(projectId)
                        .projectName(projectJson.getName())
                        .build();
                if (projectConfiguration.isPrimaryMigrationDone()) {
                    log.info("Project with ID '{}' already migrated", projectId);
                } else {
                    projectConfiguration.setGitUrl(projectJson.getConfigUrl());
                    projectConfigurationService.loadConfigurationFromGit(projectConfiguration, true);
                }
                projectJson.setSuccessfulLoad(true);
            } catch (Exception e) {
                projectJson.setSuccessfulLoad(false);
                projectJson.setException(e);
            }
            return projectJson;
        }
    }
}
