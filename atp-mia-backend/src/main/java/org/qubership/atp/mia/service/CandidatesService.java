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

package org.qubership.atp.mia.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.mia.controllers.api.dto.candidates.ExportCandidateDto;
import org.qubership.atp.mia.controllers.api.dto.candidates.ExportEntitiesCandidateDto;
import org.qubership.atp.mia.exceptions.configuration.ProjectNotFoundException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CandidatesService {

    private final ProjectConfigurationService projectConfigurationService;

    /**
     * Get candidates by project ID.
     *
     * @param projectId project ID
     * @return list candidates
     */
    public List<ExportCandidateDto> getCandidates(UUID projectId) {
        List<ExportCandidateDto> candidates = new ArrayList<>();
        Optional<ProjectConfiguration> projectConfigurationOptional =
                projectConfigurationService.findDetailedByProjectId(projectId);
        if (!projectConfigurationOptional.isPresent()) {
            throw new ProjectNotFoundException(projectId);
        }
        ProjectConfiguration projectConfiguration = projectConfigurationOptional.get();
        List<ExportEntitiesCandidateDto> sectionEntities = new ArrayList<>();
        projectConfiguration.getSections().forEach(s ->
                sectionEntities.add(new ExportEntitiesCandidateDto(s.getId(), s.getName(), "section", null)));
        candidates.add(new ExportCandidateDto(new UUID(0, 1), "Configuration", sectionEntities));
        List<ExportEntitiesCandidateDto> exportEntitiesCandidateDtos = new ArrayList<>();
        exportEntitiesCandidateDtos.add(new ExportEntitiesCandidateDto(
                new UUID(1, 1), "Common Configuration", "common_configuration", null));
        exportEntitiesCandidateDtos.add(new ExportEntitiesCandidateDto(
                new UUID(1, 2), "Header Configuration", "header_configuration", null));
        exportEntitiesCandidateDtos.add(new ExportEntitiesCandidateDto(
                new UUID(1, 3), "POT Header Configuration", "pot_header_configuration", null));
        candidates.add(new ExportCandidateDto(new UUID(0, 2), "Project Configuration", exportEntitiesCandidateDtos));
        candidates.add(new ExportCandidateDto(new UUID(0, 3), "Files",
                directoriesAndFiles(projectConfiguration.getRootDirectories())));
        return candidates;
    }

    private List<ExportEntitiesCandidateDto> directoriesAndFiles(List<ProjectDirectory> directories) {
        List<ExportEntitiesCandidateDto> directoriesAndFiles = new ArrayList<>();
        if (directories != null) {
            directories.forEach(d -> {
                if (d != null) {
                    List<ProjectDirectory> innerDirectories = d.getDirectories();
                    List<ExportEntitiesCandidateDto> directoriesAndFilesCandidates = new ArrayList<>();
                    if (innerDirectories != null && !innerDirectories.isEmpty()) {
                        directoriesAndFilesCandidates.addAll(directoriesAndFiles(innerDirectories));
                    }
                    List<ProjectFile> files = d.getFiles();
                    if (files != null && !files.isEmpty()) {
                        files.forEach(f -> directoriesAndFilesCandidates.add(new ExportEntitiesCandidateDto(
                                f.getId(), f.getName(), "file", null)));
                    }
                    directoriesAndFiles.add(new ExportEntitiesCandidateDto(
                            d.getId(), d.getName(), "directory", directoriesAndFilesCandidates));
                }
            });
        }
        return directoriesAndFiles;
    }
}
