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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.util.Strings;
import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.FlowConfigDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectConfigurationDto;
import org.qubership.atp.mia.exceptions.configuration.DeserializeJsonConfigFailedException;
import org.qubership.atp.mia.exceptions.configuration.UpdateConfigurationException;
import org.qubership.atp.mia.exceptions.fileservice.ArchiveFileNotFoundException;
import org.qubership.atp.mia.exceptions.fileservice.ArchiveIoExceptionDuringClose;
import org.qubership.atp.mia.exceptions.history.MiaHistoryRevisionRestoreException;
import org.qubership.atp.mia.model.CacheKeys;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.repo.configuration.ProjectConfigurationRepository;
import org.qubership.atp.mia.repo.db.RecordingSessionRepository;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.service.git.GitService;
import org.qubership.atp.mia.service.history.impl.AbstractEntityHistoryService;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ProjectConfigurationService extends AbstractEntityHistoryService<ProjectConfiguration> {

    private final ConfigurationFileDeserializer configurationFileDeserializer;
    private final ConfigurationFileSerializer configurationFileSerializer;
    private final MiaFileService miaFileService;
    private final GitService gitService;
    private final Path miaConfigPath;
    private final ModelMapper modelMapper;
    private final ProjectConfigurationRepository projectConfigurationRepository;
    private final RecordingSessionRepository recordingSessionRepository;
    private final ProjectConfigurationService self; // For caching
    private final ServletContext servletContext;

    /**
     * archive configuration to Zip file.
     *
     * @param projectId Project ID
     * @return Resource
     */
    public ResponseEntity<Resource> archiveConfigToZip(UUID projectId) {
        ProjectConfiguration projectConfiguration = getConfigByProjectId(projectId);
        UUID sessionId = UUID.randomUUID();
        Path pathForConfiguration = getProjectPathWithType(projectId, ProjectFileType.MIA_FILE_TYPE_LOG, sessionId)
                .resolve("project_configuration");
        pathForConfiguration.toFile().mkdirs();
        try {
            configurationFileSerializer.serializeToPath(projectConfiguration, pathForConfiguration);
            String zipName = String.format("project_configuration_%s_%s.zip", projectId, sessionId);
            Path zipPath = getProjectPathWithType(projectId, ProjectFileType.MIA_FILE_TYPE_LOG, sessionId)
                    .resolve(zipName);
            try (ZipOutputStream zos =
                         new ZipOutputStream(
                                 new BufferedOutputStream(
                                         new FileOutputStream(zipPath.toFile())))) {
                File[] files = pathForConfiguration.toFile().listFiles();
                if (files != null) {
                    for (File file : files) {
                        FileUtils.addDirToZipArchive(zos, file, null, null);
                    }
                }
            } catch (FileNotFoundException e) {
                throw new ArchiveFileNotFoundException(pathForConfiguration);
            } catch (IOException e) {
                throw new ArchiveIoExceptionDuringClose(pathForConfiguration, e);
            }
            return miaFileService.downloadFile(projectId, ProjectFileType.MIA_FILE_TYPE_LOG, sessionId, zipName,
                    servletContext);
        } finally {
            FileUtils.deleteFolder(pathForConfiguration.toFile(), true);
        }
    }

    /**
     * Find project configuration by project ID.
     *
     * @param projectId project ID
     * @return Optional
     */
    public Optional<ProjectConfiguration> findByProjectId(UUID projectId) {
        return projectConfigurationRepository.findById(projectId);
    }

    /**
     * Get configuration by project ID.
     *
     * @param projectId project ID
     * @return ProjectConfiguration instance
     */
    @Cacheable(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectId", condition = "#projectId != null")
    public ProjectConfiguration getConfigByProjectId(UUID projectId) {
        return getConfiguration(projectId);
    }

    /**
     * Get configuration by project ID.
     *
     * @param projectId project ID
     * @return ProjectConfiguration instance
     */
    public ProjectConfiguration getConfiguration(UUID projectId) {
        return findByProjectId(projectId).orElseGet(() -> {
            Path pathForConfiguration = getProjectPathWithType(projectId,
                    ProjectFileType.MIA_FILE_TYPE_CONFIGURATION, null)
                    .resolve(String.valueOf(System.currentTimeMillis()));
            Path pathForDefaultConfiguration = miaConfigPath.resolve("project").resolve("default");
            try {
                pathForConfiguration.toFile().mkdirs();
                FileUtils.copyFolder(pathForDefaultConfiguration, pathForConfiguration);
                ProjectConfiguration defaultProjectConfiguration = loadConfigurationFromFile(
                        ProjectConfiguration.builder().projectId(projectId).build(), pathForConfiguration, false);
                log.info("DEFAULT configuration parsed successfully. Save it!");
                return projectConfigurationRepository.save(defaultProjectConfiguration);
            } catch (Exception e) {
                throw new DeserializeJsonConfigFailedException("Failed copy files for DEFAULT project to "
                        + pathForConfiguration, e.getMessage());
            } finally {
                FileUtils.deleteFolder(pathForConfiguration.toFile(), true);
            }
        });
    }

    /**
     * Make old version of config for backward compatible.
     *
     * @param projectId projectId
     * @return {@link FlowConfigDto}
     */
    public FlowConfigDto getOldConfig(UUID projectId) {
        return configurationFileSerializer.getOldConfig(self.getConfigByProjectId(projectId), false);
    }

    /**
     * Gets path of project for files with needed type.
     *
     * @param projectFileType ProjectFileType
     * @return path
     */
    public Path getProjectPathWithType(UUID projectId, ProjectFileType projectFileType, UUID sessionId) {
        log.trace("Creating project path for projectId: '{}', type: '{}', sessionId: '{}'",
                projectId, projectFileType, sessionId);
        Path path = FileMetaData.PROJECT_FOLDER.resolve(projectId.toString()).resolve(projectFileType.name())
                .resolve(sessionId == null ? "" : sessionId.toString());
        FileUtils.createDirectories(path);
        return path;
    }

    /**
     * Get configuration by project ID and reload from external resource.
     *
     * @param projectConfiguration project Configuration
     * @return ProjectConfiguration instance
     */
    @Transactional
    @CacheEvict(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectConfiguration.projectId",
            condition = "#projectConfiguration.projectId != null")
    public ProjectConfiguration hardReloadConfiguration(ProjectConfiguration projectConfiguration) {
        return hardReloadConfiguration(projectConfiguration, false);
    }

    /**
     * Hard reload configuration from GIT (if present).
     *
     * @param projectConfiguration projectConfiguration
     * @return projectConfiguration
     */
    @CacheEvict(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectConfiguration.projectId",
            condition = "#projectConfiguration.projectId != null")
    public ProjectConfiguration hardReloadConfiguration(ProjectConfiguration projectConfiguration,
                                                        boolean isMigration) {
        try {
            if (projectConfiguration.getGitUrl() != null && !projectConfiguration.getGitUrl().isEmpty()) {
                return loadConfigurationFromGit(projectConfiguration, isMigration);
            }
        } catch (IOException e) {
            log.error("Error while loading Configuration from GIT: {}", e.getMessage());
        }
        return projectConfiguration;
    }

    /**
     * Upload zip config (as {@link MultipartFile}) to MIA and applies it.
     *
     * @param projectId id of project which config need to be updated.
     * @param file      directory which is contains MIA config.
     * @return ProjectConfiguration loaded, or throws exception.
     */
    @Transactional
    @CacheEvict(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectId", condition = "#projectId != null")
    public ProjectConfiguration loadConfigFromZip(UUID projectId, MultipartFile file) {
        File archivePath = null;
        try {
            // copy zip
            archivePath = FileMetaData.PROJECT_FOLDER.resolve(projectId.toString())
                    .resolve(miaFileService.uploadConfigurationFileOnBe(file)).toFile();
            File unzippedDir = null;
            try {
                // unzip
                unzippedDir = FileUtils.unzipConfig(archivePath,
                        new File(archivePath.getParent(), "unzipped-" + System.currentTimeMillis()));
                return loadConfigurationFromGit(
                        ProjectConfiguration.builder()
                                .projectId(projectId)
                                .gitUrl(unzippedDir.toString())
                                .build(),
                        true);
            } catch (IOException e) {
                throw new ArchiveIoExceptionDuringClose(unzippedDir, e.getMessage());
            } finally {
                // clean directories
                if (unzippedDir != null) {
                    FileUtils.deleteFolder(unzippedDir, true);
                }
            }
        } finally {
            // clean downloaded archive
            if (archivePath != null && archivePath.exists() && !archivePath.isDirectory()) {
                archivePath.delete();
            }
        }
    }

    /**
     * Load configuration from GIT.
     *
     * @param projectConfiguration {@link ProjectConfiguration}
     * @return deserialized {@link ProjectConfiguration}
     */
    public ProjectConfiguration loadConfigurationFromGit(ProjectConfiguration projectConfiguration,
                                                         boolean isMigration) throws IOException {
        log.info("Load configuration for '{}' from '{}'", projectConfiguration.getProjectId(),
                projectConfiguration.getGitUrl());
        Path pathForConfiguration = getProjectPathWithType(projectConfiguration.getProjectId(),
                ProjectFileType.MIA_FILE_TYPE_CONFIGURATION, null).resolve(String.valueOf(System.currentTimeMillis()));
        try {
            pathForConfiguration.toFile().mkdirs();
            if (projectConfiguration.getGitUrl().startsWith("http")) {
                gitService.downloadGitRepo(projectConfiguration.getGitUrl(), pathForConfiguration);
            } else {
                FileUtils.copyFolder(Paths.get(projectConfiguration.getGitUrl()), pathForConfiguration);
            }
            loadConfigurationFromFile(projectConfiguration, pathForConfiguration, isMigration);
            projectConfiguration.setLastLoadedWhen(LocalDateTime.now());
            log.info("Project with ID '{}' has been parsed successfully, save it", projectConfiguration.getProjectId());
            if (isMigration) {
                //It is a migration step. That mean that need to remove old configuration and clear IDs for new
                // generation of it in case use the same configuration on different projects
                self.removeProject(projectConfiguration.getProjectId(), false);
                projectConfiguration.setPrimaryMigrationDone(true);
            }
            if (Strings.isNotBlank(projectConfiguration.getGitUrl())
                    && !projectConfiguration.getGitUrl().startsWith("http")) {
                projectConfiguration.setGitUrl(null);
            }
            return projectConfigurationRepository.save(projectConfiguration);
        } catch (IOException e) {
            throw new DeserializeJsonConfigFailedException("Failed copy files from "
                    + projectConfiguration.getGitUrl() + " to " + pathForConfiguration, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to deserialize configuration for project '{}': {}",
                    projectConfiguration.getProjectId(), e.getMessage());
            throw new DeserializeJsonConfigFailedException("Failed to deserialize", e.getMessage());
        } finally {
            FileUtils.deleteFolder(pathForConfiguration.toFile(), true);
        }
    }

    /**
     * Remove project by ID.
     *
     * @param projectId project ID
     */
    @Transactional
    @CacheEvict(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectId", condition = "#projectId != null")
    public void removeProject(UUID projectId, Boolean withPot) {
        projectConfigurationRepository.deleteByProjectId(projectId);
        if (withPot != null && withPot) {
            recordingSessionRepository.deleteByProjectId(projectId);
        }
        log.info("Project with ID {} removed successfully!", projectId);
    }

    /**
     * Synchronize project DB and GIT.
     */
    @CacheEvict(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectId")
    public void synchronizeConfiguration(UUID projectId,
                                         Supplier<ProjectConfiguration> saveConfiguration,
                                         boolean isEthalonFiles) {
        log.info("invoke synchronizeConfigurationTx");
        self.synchronizeConfigurationTx(projectId, saveConfiguration, isEthalonFiles);
        log.info("completed invoke synchronizeConfigurationTx");
    }

    /**
     * Transactional part of synchronize Configuration
     */
    @Transactional
    public void synchronizeConfigurationTx(UUID projectId,
                                              Supplier<ProjectConfiguration> saveConfiguration,
                                              boolean isEthalonFiles) {
        ProjectConfiguration updatedProjectConfiguration = saveConfiguration.get();
        if (updatedProjectConfiguration.getGitUrl() != null
                && !updatedProjectConfiguration.getGitUrl().isEmpty()) {
            Path pathForConfiguration =
                    getProjectPathWithType(projectId, ProjectFileType.MIA_FILE_TYPE_CONFIGURATION, null)
                            .resolve(UUID.randomUUID().toString());
            configurationFileSerializer.serialize(
                    updatedProjectConfiguration,
                    pathForConfiguration,
                    isEthalonFiles);
        }
    }

    /**
     * Map list of ProjectConfiguration to list of ProjectConfigurationDto.
     *
     * @param projectConfiguration list of {@link ProjectConfiguration}
     * @return list of {@link ProjectConfigurationDto}
     */
    public ProjectConfigurationDto toDto(ProjectConfiguration projectConfiguration) {
        return modelMapper.map(projectConfiguration, ProjectConfigurationDto.class);
    }

    /**
     * Update ProjectConfiguration fields (GitUrl, CommonConfiguration, HeaderConfiguration, PotHeaderConfiguration.
     *
     * @param projectConfiguration    projectConfiguration
     * @param projectConfigurationDto {@link ProjectConfigurationDto}
     * @return {@link ProjectConfigurationDto}
     */
    @Transactional
    @CacheEvict(value = CacheKeys.Constants.CONFIGURATION_KEY, key = "#projectConfiguration.projectId",
            condition = "#projectConfiguration.projectId != null")
    public ProjectConfigurationDto updateConfiguration(ProjectConfiguration projectConfiguration,
                                                       ProjectConfigurationDto projectConfigurationDto) {
        log.info("Update project configuration: '{}'", projectConfigurationDto);
        try {
            UUID projectId = projectConfiguration.getProjectId();
            ProjectConfiguration projectConfigurationEntity =
                    modelMapper.map(projectConfigurationDto, ProjectConfiguration.class);
            if (Strings.isNotBlank(projectConfigurationEntity.getGitUrl())
                    && !projectConfigurationEntity.getGitUrl().equals(projectConfiguration.getGitUrl())) {
                projectConfiguration = self.hardReloadConfiguration(
                        ProjectConfiguration.builder()
                                .projectId(projectId)
                                .gitUrl(projectConfigurationDto.getGitUrl())
                                .build(),
                        true);
            } else {
                projectConfiguration.setGitUrl(projectConfigurationEntity.getGitUrl());
                if (projectConfigurationDto.getCommonConfiguration() != null) {
                    projectConfigurationEntity.getCommonConfiguration().setProjectId(projectId);
                    projectConfigurationEntity.getCommonConfiguration().updateShellPrefixes();
                    projectConfiguration.setCommonConfiguration(projectConfigurationEntity.getCommonConfiguration());
                }
                if (projectConfigurationDto.getHeaderConfiguration() != null) {
                    projectConfigurationEntity.getHeaderConfiguration().setProjectId(projectId);
                    projectConfiguration.setHeaderConfiguration(projectConfigurationEntity.getHeaderConfiguration());
                }
                if (projectConfigurationDto.getPotHeaderConfiguration() != null) {
                    projectConfigurationEntity.getPotHeaderConfiguration().setProjectId(projectId);
                    projectConfiguration.setPotHeaderConfiguration(
                            projectConfigurationEntity.getPotHeaderConfiguration());
                }
                updateProject(projectConfiguration, false);
            }
            log.info("Successfully updated configuration for project '{}'", projectConfiguration.getProjectId());
            return toDto(projectConfiguration);
        } catch (Exception e) {
            throw new UpdateConfigurationException(e);
        }
    }

    /**
     * Update project.
     */
    public void updateProject(ProjectConfiguration projectConfiguration, boolean isEthalonFiles) {
        self.synchronizeConfiguration(projectConfiguration.getProjectId(),
                () -> projectConfigurationRepository.save(projectConfiguration), isEthalonFiles);
    }

    /**
     * Update project.
     */
    public void updateProjectWithReplicationOff(ProjectConfiguration projectConfiguration, boolean isEthalonFiles) {
        self.synchronizeConfiguration(
                projectConfiguration.getProjectId(),
                () -> {
                    try {
                        projectConfigurationRepository.setReplicationRoleReplica();
                        log.info("replicationRoleReplica set");
                        ProjectConfiguration saved =  projectConfigurationRepository.save(projectConfiguration);
                        log.info("saved successfully ProjectConfiguration ");
                        return  saved;
                    } finally {
                        projectConfigurationRepository.setReplicationRoleOrigin();
                    }
                },
                isEthalonFiles
        );
    }

    private ProjectConfiguration loadConfigurationFromFile(ProjectConfiguration projectConfiguration,
                                                           Path pathFlowJson, boolean isMigration) {
        log.trace("Loading configuration from file for project '{}' at path '{}'",
                projectConfiguration.getProjectId(), pathFlowJson);
        try {
            return configurationFileDeserializer.deserialize(pathFlowJson, projectConfiguration,
                    getProjectPathWithType(projectConfiguration.getProjectId(),
                            ProjectFileType.MIA_FILE_TYPE_PROJECT, null), isMigration);
        } catch (Exception e) {
            throw new DeserializeJsonConfigFailedException("Error in deserialize config", e);
        }
    }

    @Override
    public ProjectConfiguration get(UUID id) {
        return projectConfigurationRepository.findById(id).orElseThrow(MiaHistoryRevisionRestoreException::new);
    }

    @Override
    public ProjectConfiguration restore(DateAuditorEntity entity) {
        log.info("Restoring project configuration '{}'", entity);
        ProjectConfiguration projectConfiguration = (ProjectConfiguration) entity;
        updateProject(projectConfiguration, false);
        log.info("Successfully restored project configuration '{}'", projectConfiguration.getProjectId());
        return projectConfiguration;
    }
}
