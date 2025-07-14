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

package org.qubership.atp.mia.ei.component;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.qubership.atp.mia.ei.component.ExportImportUtils.reverseMap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.ei.MiaImportParentDirectoryNotFound;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportDirectory;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.repo.configuration.DirectoryConfigurationRepository;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImportLoaderDirectory extends ImportLoader<ExportImportEntities, ProjectDirectory, ExportImportDirectory> {

    private final GridFsService gridFsService;
    private final DirectoryConfigurationRepository directoryConfigurationRepository;
    private final ProjectConfigurationService projectConfigurationService;

    /**
     * Constructor contains services to save directory and update it in configuration.
     *
     * @param objectLoaderFromDiskService      EI service to load entities for import.
     * @param gridFsService                    for updating gridFsFiles when folder renamed
     * @param directoryConfigurationRepository repository to save directories
     * @param projectConfigurationService      service to update configurations
     */
    public ImportLoaderDirectory(@Autowired ObjectLoaderFromDiskService objectLoaderFromDiskService,
                                 GridFsService gridFsService,
                                 DirectoryConfigurationRepository directoryConfigurationRepository,
                                 ProjectConfigurationService projectConfigurationService) {
        super(objectLoaderFromDiskService);
        this.gridFsService = gridFsService;
        this.directoryConfigurationRepository = directoryConfigurationRepository;
        this.projectConfigurationService = projectConfigurationService;
    }

    @Override
    public Class<ExportImportDirectory> getClazz() {
        return ExportImportDirectory.class;
    }

    @Override
    public ExportImportEntities getEntityType() {
        return ExportImportEntities.MIA_DIRECTORY;
    }

    @Override
    public ProjectDirectory toEntity(
            ProjectConfiguration projectConfiguration, ExportImportDirectory exportImportDirectory
    ) {
        ProjectDirectory parentDirectory = isNull(exportImportDirectory.getParentDirectory()) ? null :
                new ProjectDirectory().toBuilder()
                        .id(exportImportDirectory.getParentDirectory())
                        .build();
        return ProjectDirectory.builder()
                .id(exportImportDirectory.getId())
                .sourceId(exportImportDirectory.getSourceId())
                .name(exportImportDirectory.getName())
                .parentDirectory(parentDirectory)
                .projectConfiguration(projectConfiguration)
                .build();
    }

    @Override
    public List<UUID> validate(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path)
            throws MiaException {
        // Map existing directory with its id and sourceId for quick access
        Map<UUID, ProjectDirectory> existingIdToDirectory =
                ExportImportUtils.createMapIdToExistingDirectory(projectConfiguration);

        return loadImportDirectories(projectConfiguration, importData, path, existingIdToDirectory).stream()
                .map(ProjectDirectory::getId)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void importEntity(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path) {
        // Map existing directory with its id and sourceId for quick access
        Map<UUID, ProjectDirectory> existingIdToDirectory =
                ExportImportUtils.createMapIdToExistingDirectory(projectConfiguration);

        List<ProjectDirectory> directories =
                loadImportDirectories(projectConfiguration, importData, path, existingIdToDirectory);

        // 3. Merge imported directories to existing
        mergeImportDirectoriesWithExisting(projectConfiguration, directories, existingIdToDirectory);
    }

    @NotNull
    private List<ProjectDirectory> loadImportDirectories(ProjectConfiguration projectConfiguration,
                                                         ExportImportData importData,
                                                         Path path,
                                                         Map<UUID, ProjectDirectory> existingIdToDirectory
    ) {
        List<ProjectDirectory> directories = new ArrayList<>();
        // Map imported directory with its name for quick access and relationship rebuild
        Map<UUID, ProjectDirectory> idToDirectory = new HashMap<>();
        // 1. Transform export-import entity to ProjectDirectory and prepare relationship maps
        List<ExportImportDirectory> eiDirectories = loadConfiguration(importData, path);
        for (ExportImportDirectory eiDirectory : eiDirectories) {
            ProjectDirectory directory = toEntity(projectConfiguration, eiDirectory);
            idToDirectory.put(directory.getSourceId(), directory);
            idToDirectory.put(directory.getId(), directory);
            directories.add(directory);
        }

        Map<UUID, UUID> reverseReplacementMap = reverseMap(importData.getReplacementMap());
        // 2. Set relationship for imported directories
        for (ProjectDirectory directory : directories) {
            // Set parent directories
            if (nonNull(directory.getParentDirectory())) {
                UUID parentId = directory.getParentDirectory().getId();
                ProjectDirectory parent = existingIdToDirectory.getOrDefault(
                        parentId,
                        existingIdToDirectory.get(reverseReplacementMap.get(parentId))
                );

                if (isNull(parent)) {
                    // If parent haven't been found in existing, search in import directories
                    parent = idToDirectory.get(parentId);
                }

                if (nonNull(parent)) {
                    directory.setParentDirectory(parent);
                } else {
                    throw new MiaImportParentDirectoryNotFound("directory", directory.getName());
                }
            }
        }

        return directories;
    }

    private void mergeImportDirectoriesWithExisting(
            ProjectConfiguration projectConfiguration,
            List<ProjectDirectory> importDirectories,
            Map<UUID, ProjectDirectory> existingIdToDirectory
    ) {
        // Map directory ID -> all Directory NAMES inside folder, for quick handle NAME conflicts
        Map<UUID, Map<String, ProjectDirectory>> folderIdWithEntryNames =
                ExportImportUtils.createMapParentIdToDirectoryNames(projectConfiguration);

        for (ProjectDirectory importDirectory : importDirectories) {
            mergeDirectory(projectConfiguration, importDirectory, existingIdToDirectory, folderIdWithEntryNames);
        }
    }

    private void mergeDirectory(ProjectConfiguration projectConfiguration,
                                ProjectDirectory importDirectory,
                                Map<UUID, ProjectDirectory> existingIdToDirectory,
                                Map<UUID, Map<String, ProjectDirectory>> folderIdWithEntryNames) {
        // 2. Check duplicate id and sourceId file in the whole project
        ProjectDirectory conflictingDirectory = existingIdToDirectory.getOrDefault(importDirectory.getId(),
                existingIdToDirectory.get(importDirectory.getSourceId())
        );

        if (nonNull(conflictingDirectory)) {
            // 2.a. Use existing directory, update all fields taken from import directory
            conflictingDirectory.setName(importDirectory.getName());
            conflictingDirectory.setSourceId(importDirectory.getSourceId());

            // If existing and imported directories have different parent, move existing
            Optional.ofNullable(conflictingDirectory.getParentDirectory())
                    .ifPresent(parent -> parent.getDirectories().remove(conflictingDirectory));
            Optional.ofNullable(importDirectory.getParentDirectory())
                    .ifPresent(directory -> directory.getDirectories().add(conflictingDirectory));

            conflictingDirectory.setParentDirectory(importDirectory.getParentDirectory());
        } else {
            validateDuplicateDirectory(projectConfiguration, importDirectory, folderIdWithEntryNames);

            // 4. Add directory link to its parent
            Optional.ofNullable(importDirectory.getParentDirectory())
                    .map(ProjectDirectory::getDirectories)
                    .ifPresent(directories -> directories.add(importDirectory));

            existingIdToDirectory.put(importDirectory.getId(), importDirectory);
            // 5. Add directory to existing
            projectConfiguration.getDirectories().add(importDirectory);
        }
    }

    private void validateDuplicateDirectory(
            ProjectConfiguration projectConfiguration,
            ProjectDirectory importDirectory,
            Map<UUID, Map<String, ProjectDirectory>> folderIdWithEntryNames
    ) {
        // 3. Check duplicate folder name in same directory
        // Get map of names in current folder: Directory Name -> ProjectDirectory
        UUID parentId = ExportImportUtils.getFolderId(importDirectory.getParentDirectory());
        Map<String, ProjectDirectory> namesInCurrentFolder = folderIdWithEntryNames.get(parentId);

        boolean folderExist = nonNull(namesInCurrentFolder);
        if (folderExist) {

            ProjectDirectory duplicateDirectory = namesInCurrentFolder.get(importDirectory.getName());
            boolean duplicateDirectoryInFolder = nonNull(duplicateDirectory);
            if (duplicateDirectoryInFolder) {

                // 3.a. We found entry with same name as imported, so renaming existing entry
                duplicateDirectory.setName(ExportImportUtils.generateCopyEntryName(duplicateDirectory.getName()));
                recursiveRenameFiles(duplicateDirectory);

                projectConfigurationService.synchronizeConfiguration(
                        projectConfiguration.getProjectId(),
                        () -> {
                            directoryConfigurationRepository.save(duplicateDirectory);
                            return projectConfiguration;
                        },
                        true
                );
            }
        }
    }

    private void recursiveRenameFiles(ProjectDirectory directory) {
        directory.getFiles().forEach(gridFsService::rename);
        directory.getDirectories().forEach(this::recursiveRenameFiles);
    }
}
