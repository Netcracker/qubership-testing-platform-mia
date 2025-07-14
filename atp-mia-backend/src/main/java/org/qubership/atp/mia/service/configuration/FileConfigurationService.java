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

import static org.qubership.atp.mia.service.configuration.DirectoryConfigurationService.getDirectoryById;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.MoveProjectFileRequestDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectFileDto;
import org.qubership.atp.mia.exceptions.configuration.DeleteFileException;
import org.qubership.atp.mia.exceptions.configuration.DeleteFileGitSynchronizeException;
import org.qubership.atp.mia.exceptions.configuration.DuplicateFileException;
import org.qubership.atp.mia.exceptions.configuration.FIleNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.SerializeFlowJsonFailedException;
import org.qubership.atp.mia.exceptions.configuration.UpdateFileException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.repo.configuration.FileConfigurationRepository;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class FileConfigurationService {

    private final AtpUserService atpUserService;
    private final FileConfigurationRepository fileConfigurationRepository;
    private final GridFsService gridFsService;
    private final MiaFileService miaFileService;
    private final ModelMapper modelMapper;
    private final ProjectConfigurationService projectConfigurationService;

    /**
     * Add file.
     *
     * @param projectConfiguration projectConfiguration
     * @param file                 MultipartFile
     * @param projectFileDto       {@link ProjectFileDto}
     */
    public void addProjectFile(ProjectConfiguration projectConfiguration,
                               MultipartFile file,
                               ProjectFileDto projectFileDto) {
        log.info("Create file: '{}'", projectFileDto);
        if (projectConfiguration.getFiles().stream()
                .anyMatch(
                        f -> {
                            if (f.getName().equals(projectFileDto.getName())) {
                                if (f.getDirectory() != null) {
                                    return f.getDirectory().getId().equals(projectFileDto.getDirectory());
                                } else {
                                    return projectFileDto.getDirectory() == null;
                                }
                            }
                            return false;
                        })) {
            throw new DuplicateFileException(projectFileDto.getName());
        }
        ProjectFile projectFile = createProjectFile(projectConfiguration, file, projectFileDto);

        try {
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        fileConfigurationRepository.save(projectFile);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            miaFileService.removeProjectFile(projectFile);
        }
    }

    /**
     * Delete file.
     *
     * @param projectConfiguration   project Configuration
     * @param projectFile            file to be deleted
     * @param removeFromParentFolder need to remove from parent folder as well.
     */
    public void deleteFile(ProjectConfiguration projectConfiguration, ProjectFile projectFile,
                           boolean removeFromParentFolder) {
        log.warn("Delete file '{}' with ID '{}' from directory '{}'", projectFile.getName(), projectFile.getId(),
                projectFile.getDirectory() == null ? "ROOT" : projectFile.getDirectory().getName());
        miaFileService.removeProjectFile(projectFile);
        if (projectFile.getDirectory() != null && removeFromParentFolder) {
            projectFile.getDirectory().getFiles().remove(projectFile);
        }
        projectConfiguration.getFiles().remove(projectFile);

        projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                () -> {
                    fileConfigurationRepository.delete(projectFile);
                    return projectConfiguration;
                },
                true
        );
    }

    /**
     * Delete file.
     *
     * @param projectConfiguration projectConfiguration
     * @param projectFileId        file ID
     */
    public void deleteProjectFile(ProjectConfiguration projectConfiguration, UUID projectFileId) {
        log.info("Delete file with ID '{}'", projectFileId);
        ProjectFile projectFile = fileConfigurationRepository.findById(projectFileId)
                .orElseThrow(() -> new FIleNotFoundException(projectFileId));
        try {
            deleteFile(projectConfiguration, projectFile, true);
        } catch (SerializeFlowJsonFailedException e) {
            log.error("{}", e.getMessage());
            throw new DeleteFileGitSynchronizeException(e);
        } catch (Exception e) {
            throw new DeleteFileException(e);
        }
    }

    /**
     * Get project file.
     *
     * @param projectId     project ID
     * @param projectFileId file ID
     * @return {@link Resource}
     */
    public ResponseEntity<Resource> getProjectFile(UUID projectId, UUID projectFileId, ServletContext servletContext) {
        log.info("Get file with ID '{}'", projectFileId);
        ProjectFile projectFile = projectConfigurationService.getConfigByProjectId(projectId).getFiles().stream()
                .filter(file -> file.getId().equals(projectFileId)).findFirst()
                .orElseThrow(() -> new FIleNotFoundException(projectFileId));
        return miaFileService.downloadFile(projectId, ProjectFileType.MIA_FILE_TYPE_PROJECT, null,
                projectFile.getPathFile().toString(), servletContext);
    }

    /**
     * Move file.
     *
     * @param projectConfiguration      projectConfiguration
     * @param fileId                    file ID
     * @param moveProjectFileRequestDto moveProjectFileRequestDto
     */
    public void moveProjectFile(ProjectConfiguration projectConfiguration, UUID fileId,
                                MoveProjectFileRequestDto moveProjectFileRequestDto) {
        log.info("Move project file '{}' to {}", fileId, moveProjectFileRequestDto);
        ProjectFile projectFile = checkAndGetFile(projectConfiguration, fileId);
        try {
            UUID parentDirectoryId = moveProjectFileRequestDto == null
                    || moveProjectFileRequestDto.getDirectory() == null
                    ? null
                    : moveProjectFileRequestDto.getDirectory();
            ProjectDirectory newDirectory = parentDirectoryId != null
                    ? getDirectoryById(projectConfiguration, parentDirectoryId)
                    : null;
            findFileWithTheSameNameAndDeleteIt(projectConfiguration, newDirectory, projectFile.getName());
            ProjectDirectory currentDirectory = projectFile.getDirectory();
            if (currentDirectory != null) {
                currentDirectory.getFiles().remove(projectFile);
            }
            if (newDirectory != null) {
                newDirectory.getFiles().add(projectFile);
            }
            projectFile.setDirectory(newDirectory);
            gridFsService.rename(projectFile);
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        fileConfigurationRepository.save(projectFile);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            throw new UpdateFileException(e);
        }
    }

    /**
     * Update file.
     *
     * @param projectConfiguration projectConfiguration
     * @param projectFileDto       {@link ProjectFileDto}
     */
    public void updateProjectFile(ProjectConfiguration projectConfiguration, MultipartFile file,
                                  ProjectFileDto projectFileDto) {
        log.info("Update file '{}'", projectFileDto);
        ProjectFile projectFile = checkAndGetFile(projectConfiguration, projectFileDto.getId());
        try {
            if (projectFileDto.getName() != null && !projectFile.getName().equals(projectFileDto.getName())) {
                miaFileService.renameProjectFile(projectFile, projectFileDto.getName());
            }
            if (file != null && !file.isEmpty()) {
                projectFile.setLastUpdateBy(atpUserService.getAtpUser());
                LocalDateTime updatedTime = LocalDateTime.now();
                projectFile.setLastUpdateWhen(updatedTime);
                projectFile.setSize(file.getSize());
                projectFile.getProjectConfiguration().setLastLoadedWhen(updatedTime);
                projectFile.setGridFsObjectId(
                        miaFileService.saveProjectFile(file, projectFile.getPathFile()).toString());
            }
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        fileConfigurationRepository.save(projectFile);
                        return projectConfiguration;
                    }, true);
        } catch (Exception e) {
            throw new UpdateFileException(e);
        }
    }

    private ProjectFile checkAndGetFile(ProjectConfiguration projectConfiguration, UUID fileId) {
        Optional<ProjectFile> optionalProjectFile = projectConfiguration.getFiles().stream()
                .filter(f -> f.getId().equals(fileId)).findFirst();
        if (!optionalProjectFile.isPresent()) {
            throw new FIleNotFoundException(fileId);
        }
        return optionalProjectFile.get();
    }

    private ProjectFile createProjectFile(ProjectConfiguration projectConfiguration,
                                          MultipartFile file,
                                          ProjectFileDto projectFileDto) {
        ProjectFile projectFile = modelMapper.map(projectFileDto, ProjectFile.class);
        projectFile.setProjectConfiguration(projectConfiguration);
        if (projectFileDto.getDirectory() != null) {
            ProjectDirectory directory = getDirectoryById(projectConfiguration, projectFileDto.getDirectory());
            projectFile.setDirectory(directory);
            directory.getFiles().add(projectFile);
        }
        projectFile.setLastUpdateBy(atpUserService.getAtpUser());
        LocalDateTime updatedTime = LocalDateTime.now();
        projectFile.setLastUpdateWhen(updatedTime);
        projectFile.setSize(file.getSize());
        projectFile.getProjectConfiguration().setLastLoadedWhen(updatedTime);
        projectFile.setGridFsObjectId(miaFileService.saveProjectFile(file, projectFile.getPathFile()).toString());
        projectConfiguration.getFiles().add(projectFile);
        return projectFile;
    }

    private void findFileWithTheSameNameAndDeleteIt(ProjectConfiguration projectConfiguration,
                                                    ProjectDirectory projectDirectory,
                                                    String fileName) {
        Stream<ProjectFile> files = projectDirectory == null
                ? projectConfiguration.getRootFiles().stream()
                : projectDirectory.getFiles().stream();
        files.filter(f -> f.getName().equals(fileName)).collect(Collectors.toSet())
                .forEach(f -> deleteFile(projectConfiguration, f, true));
    }
}
