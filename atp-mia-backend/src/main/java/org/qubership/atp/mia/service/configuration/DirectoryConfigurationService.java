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

import static org.qubership.atp.mia.model.Constants.MIA_ROOT_DIRECTORY;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.MoveDirectoryRequestDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectDirectoriesDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectDirectoryDto;
import org.qubership.atp.mia.exceptions.configuration.CreateDirectoryException;
import org.qubership.atp.mia.exceptions.configuration.DeleteDirectoryException;
import org.qubership.atp.mia.exceptions.configuration.DirectoryCyclicDependencyException;
import org.qubership.atp.mia.exceptions.configuration.DirectoryNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.DuplicateDirectoryException;
import org.qubership.atp.mia.exceptions.configuration.UpdateDirectoryException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.repo.configuration.DirectoryConfigurationRepository;
import org.qubership.atp.mia.service.file.GridFsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class DirectoryConfigurationService {

    private final DirectoryConfigurationRepository directoryConfigurationRepository;
    private final GridFsService gridFsService;
    private final ModelMapper modelMapper;
    private final ProjectConfigurationService projectConfigurationService;
    private final FileConfigurationService fileConfigurationService;

    /**
     * Add directory.
     *
     * @param projectConfiguration projectConfiguration
     * @param projectDirectoryDto  {@link ProjectDirectoryDto}
     */
    public void addDirectory(ProjectConfiguration projectConfiguration, ProjectDirectoryDto projectDirectoryDto) {
        log.info("Create directory: '{}'", projectDirectoryDto);
        if (projectConfiguration.getAllDirectories().stream().anyMatch(dir -> {
            if (dir.getName().equals(projectDirectoryDto.getName())) {
                if (dir.getParentDirectory() != null) {
                    return dir.getParentDirectory().getId().equals(projectDirectoryDto.getParentDirectoryId());
                } else {
                    return projectDirectoryDto.getParentDirectoryId() == null;
                }
            }
            return false;
        })
        ) {
            throw new DuplicateDirectoryException(projectDirectoryDto.getName());
        }
        try {
            ProjectDirectory parentDirectory = projectDirectoryDto.getParentDirectoryId() == null
                    ? null
                    : getDirectoryById(projectConfiguration, projectDirectoryDto.getParentDirectoryId());
            ProjectDirectory projectDirectory = modelMapper.map(projectDirectoryDto, ProjectDirectory.class);
            projectDirectory.setProjectConfiguration(projectConfiguration);
            projectConfiguration.getDirectories().add(projectDirectory);
            if (parentDirectory != null) {
                parentDirectory.getDirectories().add(projectDirectory);
                projectDirectory.setParentDirectory(parentDirectory);
            }
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        directoryConfigurationRepository.save(projectDirectory);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            throw new CreateDirectoryException(e);
        }
    }

    /**
     * Delete directory.
     *
     * @param projectConfiguration projectConfiguration
     * @param directoryId          directory ID
     */
    public void deleteDirectory(ProjectConfiguration projectConfiguration, UUID directoryId) {
        log.info("Delete directory with ID '{}'", directoryId);
        Optional<ProjectDirectory> optionalProjectDirectory =
                projectConfiguration.getDirectories().stream().filter(dir -> dir.getId().equals(directoryId)).findAny();
        if (!optionalProjectDirectory.isPresent()) {
            throw new DirectoryNotFoundException(directoryId);
        }
        try {
            deleteWholeDirectory(projectConfiguration, optionalProjectDirectory.get());
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        directoryConfigurationRepository.deleteById(directoryId);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            throw new DeleteDirectoryException(e);
        }
    }

    /**
     * Map list of ProjectDirectory to list of ProjectDirectoriesDto.
     *
     * @param projectConfiguration list of {@link ProjectDirectory}
     * @return list of {@link ProjectDirectoryDto}
     */
    public ProjectDirectoriesDto directoriesDto(ProjectConfiguration projectConfiguration) {
        ProjectDirectory rootProjectDirectory = ProjectDirectory.builder()
                .name(MIA_ROOT_DIRECTORY)
                .directories(projectConfiguration.getRootDirectories())
                .files(projectConfiguration.getRootFiles())
                .build();
        return modelMapper.map(rootProjectDirectory, ProjectDirectoriesDto.class);
    }

    /**
     * Get directory by ID.
     *
     * @param projectDirectoryId project directory ID
     * @return {@link ProjectDirectory}
     */
    public static ProjectDirectory getDirectoryById(ProjectConfiguration projectConfig, UUID projectDirectoryId) {
        return projectConfig.getDirectories().stream()
                .filter(d -> d.getId().equals(projectDirectoryId)).findFirst()
                .orElseThrow(() -> new DirectoryNotFoundException(projectDirectoryId));
    }

    /**
     * Move directory.
     *
     * @param projectConfiguration    projectConfiguration
     * @param directoryId             directory ID
     * @param moveDirectoryRequestDto moveDirectoryRequestDto
     */
    public void moveDirectory(ProjectConfiguration projectConfiguration,
                              UUID directoryId,
                              MoveDirectoryRequestDto moveDirectoryRequestDto) {
        log.info("Move directory '{}' to {}", directoryId, moveDirectoryRequestDto);
        ProjectDirectory projectDirectory = checkAndGetDirectory(projectConfiguration, directoryId,
                moveDirectoryRequestDto.getParentDirectory());
        try {
            UUID parentDirectoryId = moveDirectoryRequestDto.getParentDirectory();
            ProjectDirectory parentDirectory = null;
            if (parentDirectoryId != null) {
                parentDirectory = getDirectoryById(projectConfiguration, parentDirectoryId);
                parentDirectory.getDirectories().add(projectDirectory);
            }
            if (projectDirectory.getParentDirectory() != null) {
                projectDirectory.getParentDirectory().getDirectories().remove(projectDirectory);
            }
            projectDirectory.setParentDirectory(parentDirectory);
            updateFilesPathOfDirectory(projectDirectory);
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        directoryConfigurationRepository.save(projectDirectory);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            throw new UpdateDirectoryException(e);
        }
    }

    /**
     * Update compound.
     *
     * @param projectConfiguration projectConfiguration
     * @param projectDirectoryDto  {@link ProjectDirectoryDto}
     */
    public void updateDirectory(ProjectConfiguration projectConfiguration, ProjectDirectoryDto projectDirectoryDto) {
        log.info("Update directory: '{}'", projectDirectoryDto);
        ProjectDirectory projectDirectory = checkAndGetDirectory(projectConfiguration, projectDirectoryDto.getId(),
                projectDirectoryDto.getParentDirectoryId());
        try {
            projectDirectory.setName(projectDirectoryDto.getName());
            updateFilesPathOfDirectory(projectDirectory);
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        directoryConfigurationRepository.save(projectDirectory);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            throw new UpdateDirectoryException(e);
        }
    }

    private ProjectDirectory checkAndGetDirectory(ProjectConfiguration projectConfiguration,
                                                  UUID directoryId,
                                                  UUID parentDirectoryId) {
        ProjectDirectory projectDirectory = projectConfiguration.getDirectories().stream()
                .filter(dir -> dir.getId().equals(directoryId)).findFirst()
                .orElseThrow(() -> new DirectoryNotFoundException(directoryId));
        // Check for cyclic
        if (parentDirectoryId != null && projectDirectory.getChildrenUuid().contains(parentDirectoryId)) {
            throw new DirectoryCyclicDependencyException(directoryId, parentDirectoryId);
        }
        return projectDirectory;
    }

    private void deleteWholeDirectory(ProjectConfiguration projectConfiguration,
                                      ProjectDirectory projectDirectory) {
        deleteWholeDirectory(projectConfiguration, projectDirectory, true);
    }

    private void deleteWholeDirectory(ProjectConfiguration projectConfiguration,
                                      ProjectDirectory projectDirectory,
                                      boolean deleteFromParent) {
        log.warn("Delete directory {} with ID '{}'", projectDirectory.getName(), projectDirectory.getId());
        if (!projectDirectory.getDirectories().isEmpty()) {
            projectDirectory.getDirectories().forEach(d -> deleteWholeDirectory(projectConfiguration, d, false));
            projectDirectory.setDirectories(new ArrayList<>());
        }
        if (!projectDirectory.getFiles().isEmpty()) {
            projectDirectory.getFiles()
                    .forEach(f -> fileConfigurationService.deleteFile(projectConfiguration, f, false));
            projectDirectory.setFiles(new ArrayList<>());
        }
        if (projectDirectory.getParentDirectory() != null && deleteFromParent) {
            projectDirectory.getParentDirectory().getDirectories().remove(projectDirectory);
        }
        projectConfiguration.getDirectories().remove(projectDirectory);
    }

    private void updateFilesPathOfDirectory(ProjectDirectory projectDirectory) {
        log.info("Update path for files in directory {} with ID {}",
                projectDirectory.getName(), projectDirectory.getId());
        if (!projectDirectory.getFiles().isEmpty()) {
            projectDirectory.getFiles().forEach(gridFsService::rename);
        }
        if (!projectDirectory.getDirectories().isEmpty()) {
            projectDirectory.getDirectories().forEach(this::updateFilesPathOfDirectory);
        }
    }
}
