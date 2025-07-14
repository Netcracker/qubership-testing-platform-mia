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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.qubership.atp.mia.model.ei.ExportImportFile;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.repo.configuration.FileConfigurationRepository;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImportLoaderFile extends ImportLoader<ExportImportEntities, ProjectFile, ExportImportFile> {

    private final GridFsService gridFsService;
    private final AtpUserService atpUserService;
    private final FileConfigurationRepository fileConfigurationRepository;
    private final ProjectConfigurationService projectConfigurationService;

    /**
     * For ImportLoaderFile a constructor was extended with gridFsService to save/get gridFsFile and
     * AtpUserService to set lastUpdatedBy field of the file.
     *
     * @param objectLoaderFromDiskService EI service to load entities for import.
     * @param gridFsService               to save/get gridFsFile.
     * @param atpUserService              to set lastUpdatedBy field of the file.
     * @param fileConfigurationRepository repository to save files
     * @param projectConfigurationService service to update configurations
     */
    public ImportLoaderFile(@Autowired ObjectLoaderFromDiskService objectLoaderFromDiskService,
                            GridFsService gridFsService,
                            AtpUserService atpUserService,
                            FileConfigurationRepository fileConfigurationRepository,
                            ProjectConfigurationService projectConfigurationService) {
        super(objectLoaderFromDiskService);
        this.gridFsService = gridFsService;
        this.fileConfigurationRepository = fileConfigurationRepository;
        this.atpUserService = atpUserService;
        this.projectConfigurationService = projectConfigurationService;
    }

    @Override
    public Class<ExportImportFile> getClazz() {
        return ExportImportFile.class;
    }

    @Override
    public ExportImportEntities getEntityType() {
        return ExportImportEntities.MIA_FILES;
    }

    @Override
    public ProjectFile toEntity(ProjectConfiguration projectConfiguration, ExportImportFile exportImportFile) {
        return ProjectFile.builder()
                .id(exportImportFile.getId())
                .sourceId(exportImportFile.getSourceId())
                .name(exportImportFile.getName())
                .lastUpdateWhen(LocalDateTime.now())
                .lastUpdateBy(atpUserService.getAtpUser())
                .projectConfiguration(projectConfiguration)
                .build();
    }

    @Override
    public List<UUID> validate(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path)
            throws MiaException {
        List<UUID> validationIds = new ArrayList<>();

        // Map existing directory with its id and sourceId for quick access
        Map<UUID, ProjectDirectory> existingIdToDirectory =
                ExportImportUtils.createMapIdToExistingDirectory(projectConfiguration);

        Map<UUID, UUID> reverseReplacementMap = ExportImportUtils.reverseMap(importData.getReplacementMap());
        Set<UUID> importIds = objectLoaderFromDiskService.getListOfObjects(path, ExportImportDirectory.class).keySet();

        List<ExportImportFile> eiFiles = loadConfiguration(importData, path);
        for (ExportImportFile eiFile : eiFiles) {
            try {
                loadFile(projectConfiguration, eiFile, existingIdToDirectory, reverseReplacementMap);
            } catch (MiaImportParentDirectoryNotFound e) {
                // To handle situations when file has directory which will be newly imported,
                // as result exception that directory not found is thrown.
                if (!importIds.contains(eiFile.getDirectory())) {
                    throw e;
                }
            } finally {
                validationIds.add(eiFile.getId());
            }
        }

        return validationIds;
    }

    private ProjectFile loadFile(ProjectConfiguration projectConfiguration,
                                 ExportImportFile exportImportFile,
                                 Map<UUID, ProjectDirectory> existingIdToDirectory,
                                 Map<UUID, UUID> reverseReplaceMap) {
        // 1. Find current directory by sourceId or id
        ProjectDirectory parent = null;

        UUID parentId = exportImportFile.getDirectory();
        if (nonNull(parentId)) {
            parent = existingIdToDirectory.getOrDefault(
                    parentId,
                    existingIdToDirectory.get(reverseReplaceMap.get(parentId))
            );

            if (isNull(parent)) {
                throw new MiaImportParentDirectoryNotFound("file", exportImportFile.getName());
            }
        }

        ProjectFile result = toEntity(projectConfiguration, exportImportFile);
        result.setDirectory(parent);
        return result;
    }

    private ProjectFile validateDuplicateFile(
            ProjectConfiguration projectConfiguration,
            ProjectFile importFile,
            Map<UUID, Map<String, ProjectFile>> folderIdWithEntryNames
    ) {
        // 3. Check duplicate file name in same directory
        // Get map of names in current folder: File Name -> ProjectFile
        UUID parentId = ExportImportUtils.getFolderId(importFile.getDirectory());
        Map<String, ProjectFile> namesInCurrentFolder = folderIdWithEntryNames.get(parentId);

        boolean folderExist = nonNull(namesInCurrentFolder);
        if (folderExist) {

            ProjectFile duplicateFile = namesInCurrentFolder.get(importFile.getName());
            boolean duplicateFileInFolder = nonNull(duplicateFile);
            if (duplicateFileInFolder) {

                // 3.a. We found entry with same name as imported, so renaming existing entry
                duplicateFile.setName(ExportImportUtils.generateCopyEntryName(duplicateFile.getName()));
                projectConfigurationService.synchronizeConfiguration(
                        projectConfiguration.getProjectId(),
                        () -> {
                            gridFsService.rename(duplicateFile);
                            fileConfigurationRepository.save(duplicateFile);
                            return projectConfiguration;
                        },
                        true
                );
            }
        }

        return importFile;
    }

    @Transactional
    @Override
    public void importEntity(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path) {

        List<ProjectFile> loadedFiles = loadFiles(projectConfiguration, importData, path);
        mergeImportedFilesWithExisting(projectConfiguration, loadedFiles);
    }

    @NotNull
    private List<ProjectFile> loadFiles(ProjectConfiguration projectConfiguration,
                                        ExportImportData importData,
                                        Path path) {
        // Map existing directory with its id and sourceId for quick access
        Map<UUID, ProjectDirectory> existingIdToDirectory =
                ExportImportUtils.createMapIdToExistingDirectory(projectConfiguration);

        Map<UUID, UUID> reverseReplacementMap = ExportImportUtils.reverseMap(importData.getReplacementMap());

        // Map directory ID -> all file NAMES inside folder, for quick handle NAME conflicts
        Map<UUID, Map<String, ProjectFile>> folderIdWithEntryNames =
                ExportImportUtils.createMapParentIdToFileNames(projectConfiguration);

        return loadConfiguration(importData, path).stream()
                .map(eiFile -> loadFile(projectConfiguration, eiFile, existingIdToDirectory, reverseReplacementMap))
                .map(importFile -> validateDuplicateFile(projectConfiguration, importFile, folderIdWithEntryNames))
                .map(importFile -> saveImportedFileToGridFs(projectConfiguration.getProjectId(), importFile, path))
                .collect(Collectors.toList());
    }

    private void mergeImportedFilesWithExisting(
            ProjectConfiguration projectConfiguration,
            List<ProjectFile> loadedFiles
    ) {
        // Map existing file with its id and sourceId for quick access
        Map<UUID, ProjectFile> existingIdToFile = ExportImportUtils.createMapIdToExistingFile(projectConfiguration);

        for (ProjectFile importFile : loadedFiles) {
            mergeFile(projectConfiguration, importFile, existingIdToFile);
        }
    }

    private void mergeFile(ProjectConfiguration projectConfiguration,
                           final ProjectFile importFile,
                           Map<UUID, ProjectFile> existingIdToFile
    ) {

        // 2. Check duplicate id and sourceId file in the whole project
        ProjectFile conflictingFile = existingIdToFile.getOrDefault(importFile.getId(),
                existingIdToFile.get(importFile.getSourceId()));

        if (nonNull(conflictingFile)) {
            // 2.a. Use existing File, update all fields taken from import File. Update gridFS
            conflictingFile.setName(importFile.getName());
            conflictingFile.setSourceId(importFile.getSourceId());
            conflictingFile.setLastUpdateBy(importFile.getLastUpdateBy());
            conflictingFile.setLastUpdateWhen(importFile.getLastUpdateWhen());
            conflictingFile.setSize(importFile.getSize());
            conflictingFile.setGridFsObjectId(importFile.getGridFsObjectId());

            Optional.ofNullable(conflictingFile.getDirectory())
                    .ifPresent(parent -> parent.getFiles().remove(conflictingFile));
            Optional.ofNullable(importFile.getDirectory())
                    .ifPresent(parent -> parent.getFiles().add(conflictingFile));

            conflictingFile.setDirectory(importFile.getDirectory());
        } else {
            // 4. Set link to file in existing directory
            Optional.ofNullable(importFile.getDirectory())
                    .map(ProjectDirectory::getFiles)
                    .ifPresent(f -> f.add(importFile));
            // 5. Add file to existing files
            projectConfiguration.getFiles().add(importFile);
        }
    }

    private ProjectFile saveImportedFileToGridFs(UUID projectId, ProjectFile file, Path path) {
        // 0. Upload import file to gridFS
        FileMetaData metaData = new FileMetaData(
                projectId,
                Paths.get(ProjectFileType.MIA_FILE_TYPE_PROJECT.name()).resolve(file.getPathFile()).toString(),
                ProjectFileType.MIA_FILE_TYPE_PROJECT
        );

        File miaFilePath = path.resolve(ExportImportEntities.GRID_FS_FILE.getValue())
                .resolve(Optional.ofNullable(file.getSourceId()).orElse(file.getId()).toString())
                .resolve(file.getName())
                .toFile();

        String gridFsId = gridFsService.uploadFile(metaData, miaFilePath).toString();
        file.setGridFsObjectId(gridFsId);

        // calculate size by fetching save file
        long size = gridFsService.getFile(file.getGridFsObjectId())
                .map(GridFSFile::getLength)
                .orElse(0L);
        file.setSize(size);

        return file;
    }
}
