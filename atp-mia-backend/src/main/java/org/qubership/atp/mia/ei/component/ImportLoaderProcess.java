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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportProcess;
import org.qubership.atp.mia.service.configuration.SectionConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImportLoaderProcess extends ImportLoader<ExportImportEntities,
        ProcessConfiguration,
        ExportImportProcess> {

    public ImportLoaderProcess(@Autowired ObjectLoaderFromDiskService objectLoaderFromDiskService) {
        super(objectLoaderFromDiskService);
    }

    @Override
    public Class<ExportImportProcess> getClazz() {
        return ExportImportProcess.class;
    }

    @Override
    public ExportImportEntities getEntityType() {
        return ExportImportEntities.MIA_PROCESSES;
    }

    @Override
    public void importEntity(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path) {
        List<ProcessConfiguration> loadedProcesses = new ArrayList<>();
        List<ExportImportProcess> importProcesses = loadConfiguration(importData, path);
        importProcesses.forEach(p -> loadProcess(projectConfiguration, loadedProcesses, p, importData));
        loadedProcesses.forEach(p -> mergeProcessOrAdd(projectConfiguration, p));
    }

    @Override
    public ProcessConfiguration toEntity(ProjectConfiguration projectConfiguration,
                                         ExportImportProcess exportImportProcess) {
        return new ProcessConfiguration().toBuilder()
                .id(exportImportProcess.getId())
                .name(exportImportProcess.getName())
                .sourceId(exportImportProcess.getSourceId())
                .processSettings(exportImportProcess.getProcessSettings())
                .inSections(new ArrayList<>())
                .inCompounds(new ArrayList<>())
                .build();
    }

    @Override
    public List<UUID> validate(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path)
            throws MiaException {
        List<ExportImportProcess> importProcesses = loadConfiguration(importData, path);
        importProcesses.forEach(p -> loadProcess(projectConfiguration, new ArrayList<>(), p, importData));
        return importProcesses.stream().map(ExportImportProcess::getId).collect(Collectors.toList());
    }

    private void loadProcess(ProjectConfiguration projectConfiguration,
                             List<ProcessConfiguration> loadedProcesses,
                             ExportImportProcess importProcess,
                             ExportImportData importData) {
        if (loadedProcesses.stream().noneMatch(s -> importProcess.getId().equals(s.getId()))) {
            ProcessConfiguration process = toEntity(projectConfiguration, importProcess);
            List<SectionConfiguration> inSections = SectionConfigurationService
                    .filterSectionsByIdOrSourceId(projectConfiguration.getSections(),
                            importData.isInterProjectImport() || importData.isCreateNewProject()
                                    ? replaceIdsBack(importProcess.getInSections(), importData.getReplacementMap())
                                    : importProcess.getInSections());
            process.setInSections(inSections);
            loadedProcesses.add(process);
        }
    }

    private void mergeProcessOrAdd(ProjectConfiguration projectConfiguration,
                                   ProcessConfiguration process) {
        if (process.getSourceId() != null) {
            mergeProcessOrAdd(projectConfiguration, process, process.getSourceId());
        } else {
            mergeProcessOrAdd(projectConfiguration, process, process.getId());
        }
    }

    private void mergeProcessOrAdd(ProjectConfiguration projectConfiguration,
                                   ProcessConfiguration process,
                                   UUID processId) {
        Optional<ProcessConfiguration> realProcessOptional = projectConfiguration.getProcesses().stream()
                .filter(realS -> processId.equals(realS.getId())
                        || processId.equals(realS.getSourceId()))
                .findFirst();
        if (realProcessOptional.isPresent()) {
            // migrate with real section
            ProcessConfiguration realProcess = realProcessOptional.get();
            realProcess.setName(process.getName());
            realProcess.setSourceId(process.getSourceId());
            realProcess.setProcessSettings(process.getProcessSettings());
            process.getInSections().forEach(s -> s.addProcess(realProcess));
            //migrate sections
            Set<SectionConfiguration> sections = new HashSet<>(realProcess.getInSections());
            sections.addAll(process.getInSections());
            realProcess.setInSections(new ArrayList<>(sections));
        } else {
            //Add new process
            Optional<ProcessConfiguration> realProcessTheSameNameOptional = projectConfiguration.getProcesses().stream()
                    .filter(realS -> process.getName().equals(realS.getName())).findFirst();
            if (realProcessTheSameNameOptional.isPresent()) {
                process.setName(process.getName() + "_ei_" + UUID.randomUUID());
            }
            process.setProjectConfiguration(projectConfiguration);
            process.getInSections().forEach(s -> s.addProcess(process));
            projectConfiguration.getProcesses().add(process);
        }
    }
}
