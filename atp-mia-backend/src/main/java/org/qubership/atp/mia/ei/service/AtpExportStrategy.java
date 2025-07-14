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

package org.qubership.atp.mia.ei.service;

import static org.qubership.atp.mia.ei.component.ExportImportUtils.createMapIdToExistingDirectory;
import static org.qubership.atp.mia.ei.component.ExportImportUtils.createMapIdToExistingFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.mia.exceptions.ei.MiaExportCopyToFileException;
import org.qubership.atp.mia.exceptions.ei.MiaExportFileNotFoundException;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportCommonConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportCompound;
import org.qubership.atp.mia.model.ei.ExportImportDirectory;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportFile;
import org.qubership.atp.mia.model.ei.ExportImportHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportPotHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportProcess;
import org.qubership.atp.mia.model.ei.ExportImportSection;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.service.configuration.CompoundConfigurationService;
import org.qubership.atp.mia.service.configuration.ProcessConfigurationService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.configuration.SectionConfigurationService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.springframework.stereotype.Service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtpExportStrategy implements ExportStrategy {

    private final FileService fileService;
    private final GridFsService gridFsService;
    private final ObjectSaverToDiskService objectSaverToDiskService;
    private final ProjectConfigurationService projectConfigurationService;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.ATP;
    }

    @Override
    public void export(ExportImportData exportData, Path path) {
        try {
            ProjectConfiguration projectConfiguration =
                    projectConfigurationService.getConfigByProjectId(exportData.getProjectId());
            exportSections(projectConfiguration, exportData, path);
            exportProcesses(projectConfiguration, exportData, path);
            exportCompound(projectConfiguration, exportData, path);
            exportGeneralConfiguration(projectConfiguration, exportData, path);
            exportFiles(projectConfiguration, exportData, path);
        } catch (Exception e) {
            log.error("Export failed with exception {}", e.getMessage(), e);
            throw e;
        }
    }

    private void exportCompound(ProjectConfiguration projectConfiguration, ExportImportData exportData, Path path) {
        List<UUID> exportCompoundIds = exportData.getExportScope().getEntities()
                .getOrDefault(ExportImportEntities.MIA_COMPOUNDS.getValue(), new HashSet<>())
                .stream().map(UUID::fromString).collect(Collectors.toList());
        List<CompoundConfiguration> compounds =
                CompoundConfigurationService.filterCompounds(projectConfiguration.getCompounds(), exportCompoundIds)
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        // export compound
        compounds.forEach(c -> objectSaverToDiskService.exportAtpEntity(c.getId(), new ExportImportCompound(c),
                path.resolve(ExportImportEntities.MIA_SECTION.getValue())));
        // export processes of compound
        compounds.stream()
                .flatMap(c -> c.getProcesses().stream())
                .filter(Objects::nonNull)
                .forEach(p -> objectSaverToDiskService.exportAtpEntity(p.getId(), new ExportImportProcess(p),
                        path.resolve(ExportImportEntities.MIA_SECTION.getValue())));
        log.info("Compounds exported");
    }

    private void exportProcesses(ProjectConfiguration projectConfiguration, ExportImportData exportData, Path path) {
        List<UUID> exportProcessIds = exportData.getExportScope().getEntities()
                .getOrDefault(ExportImportEntities.MIA_PROCESSES.getValue(), new HashSet<>())
                .stream().map(UUID::fromString).collect(Collectors.toList());
        ProcessConfigurationService.filterProcess(projectConfiguration.getProcesses(), exportProcessIds).stream()
                .filter(Objects::nonNull)
                .forEach(p -> objectSaverToDiskService.exportAtpEntity(p.getId(), new ExportImportProcess(p),
                        path.resolve(ExportImportEntities.MIA_SECTION.getValue())));
        log.info("Processes exported");
    }

    private void exportSections(ProjectConfiguration projectConfiguration, ExportImportData exportData, Path path) {
        List<UUID> exportSectionIds = exportData.getExportScope().getEntities()
                .getOrDefault(ExportImportEntities.MIA_SECTION.getValue(), new HashSet<>())
                .stream().map(UUID::fromString).collect(Collectors.toList());
        SectionConfigurationService.filterSections(projectConfiguration.getSections(), exportSectionIds).stream()
                .filter(Objects::nonNull)
                .forEach(s -> objectSaverToDiskService.exportAtpEntity(s.getId(), new ExportImportSection(s),
                        path.resolve(ExportImportEntities.MIA_SECTION.getValue())));
        log.info("Sections exported");
    }

    private void exportGeneralConfiguration(ProjectConfiguration projectConfiguration,
                                            ExportImportData exportData,
                                            Path path) {
        Map<String, Set<String>> entities = exportData.getExportScope().getEntities();
        boolean isCommonConfiguration = entities.containsKey(ExportImportEntities.MIA_COMMON_CONFIGURATION.getValue())
                && !entities.get(ExportImportEntities.MIA_COMMON_CONFIGURATION.getValue()).isEmpty();
        boolean isHeaderConfiguration = entities.containsKey(ExportImportEntities.MIA_HEADER_CONFIGURATION.getValue())
                && !entities.get(ExportImportEntities.MIA_HEADER_CONFIGURATION.getValue()).isEmpty();
        boolean isPotConfiguration = entities.containsKey(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION.getValue())
                && !entities.get(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION.getValue()).isEmpty();

        if (isCommonConfiguration) {
            objectSaverToDiskService.exportAtpEntity(
                    projectConfiguration.getProjectId(),
                    new ExportImportCommonConfiguration(projectConfiguration.getCommonConfiguration()),
                    path.resolve(ExportImportEntities.MIA_PROJECT_CONFIGURATION.getValue())
            );
            log.info("CommonConfiguration exported");
        }
        if (isHeaderConfiguration) {
            objectSaverToDiskService.exportAtpEntity(
                    projectConfiguration.getProjectId(),
                    new ExportImportHeaderConfiguration(projectConfiguration.getHeaderConfiguration()),
                    path.resolve(ExportImportEntities.MIA_PROJECT_CONFIGURATION.getValue())
            );
            log.info("HeaderConfiguration exported");
        }
        if (isPotConfiguration) {
            objectSaverToDiskService.exportAtpEntity(
                    projectConfiguration.getProjectId(),
                    new ExportImportPotHeaderConfiguration(projectConfiguration.getPotHeaderConfiguration()),
                    path.resolve(ExportImportEntities.MIA_PROJECT_CONFIGURATION.getValue())
            );
            log.info("PotHeaderConfiguration exported");
        }
    }

    private void exportFiles(ProjectConfiguration projectConfiguration, ExportImportData exportData, Path path) {
        // For quick access
        Map<UUID, ProjectDirectory> idToDirectory = createMapIdToExistingDirectory(projectConfiguration);
        // Export directories
        exportData.getExportScope().getEntities()
                .getOrDefault(ExportImportEntities.MIA_DIRECTORY.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .map(idToDirectory::get)
                .forEach(dir -> exportForDirectories(dir, path));

        // For quick access
        Map<UUID, ProjectFile> idToFile = createMapIdToExistingFile(projectConfiguration);
        // Collect files to export and export
        exportData.getExportScope().getEntities()
                .getOrDefault(ExportImportEntities.MIA_FILES.getValue(), new HashSet<>())
                .stream()
                .map(UUID::fromString)
                .map(idToFile::get)
                .forEach(file -> exportForFiles(path, file));
        log.info("Files exported");
    }

    private void exportForFiles(Path path, ProjectFile exportFile) {
        GridFSFile gridFsFile = gridFsService.getFile(exportFile.getGridFsObjectId())
                .orElseThrow(() ->
                        new MiaExportFileNotFoundException(exportFile.getName(), exportFile.getGridFsObjectId())
                );

        // Export PostgresDb record
        ExportImportFile eiFile = new ExportImportFile(exportFile);
        objectSaverToDiskService.exportAtpEntity(
                eiFile.getId(),
                eiFile,
                path.resolve(ExportImportEntities.MIA_FILES.getValue())
        );

        // Create directory and file to copy gridFs file in it
        Path miaFilePath = fileService.createDirectory(
                path.resolve(ExportImportEntities.MIA_FILES.getValue())
                        .resolve(ExportImportEntities.GRID_FS_FILE.getValue())
                        .resolve(eiFile.getSourceId().toString())
        );
        Path targetPath = fileService.createFile(exportFile.getName(), miaFilePath);

        // Copy file from gridFs to newly created export file
        byte[] fileStream = gridFsService.getByteArrayFromGridFsFile(gridFsFile);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileStream)) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MiaExportCopyToFileException(exportFile.getName(), e);
        }
    }

    private void exportForDirectories(ProjectDirectory directory, Path path) {
        ExportImportDirectory exportImportDirectory = new ExportImportDirectory(directory);
        objectSaverToDiskService.exportAtpEntity(
                exportImportDirectory.getId(),
                exportImportDirectory,
                path.resolve(ExportImportEntities.MIA_FILES.getValue())
        );
        log.info("Directories exported");
    }
}
