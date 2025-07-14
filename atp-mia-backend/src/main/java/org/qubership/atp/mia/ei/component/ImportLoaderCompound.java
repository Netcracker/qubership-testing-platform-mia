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

import static org.qubership.atp.mia.service.configuration.ProcessConfigurationService.filterProcessesByIdOrSourceId;

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
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportCompound;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.service.configuration.SectionConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImportLoaderCompound extends ImportLoader<
        ExportImportEntities,
        CompoundConfiguration,
        ExportImportCompound> {

    public ImportLoaderCompound(@Autowired ObjectLoaderFromDiskService objectLoaderFromDiskService) {
        super(objectLoaderFromDiskService);
    }

    @Override
    public Class<ExportImportCompound> getClazz() {
        return ExportImportCompound.class;
    }

    @Override
    public ExportImportEntities getEntityType() {
        return ExportImportEntities.MIA_COMPOUNDS;
    }

    @Override
    public void importEntity(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path) {
        List<CompoundConfiguration> loadedCompounds = new ArrayList<>();
        List<ExportImportCompound> importCompounds = loadConfiguration(importData, path);
        importCompounds.forEach(c -> loadCompound(projectConfiguration, loadedCompounds, c, importData));
        loadedCompounds.forEach(c -> mergeCompoundOrAdd(projectConfiguration, c));
    }

    @Override
    public CompoundConfiguration toEntity(ProjectConfiguration projectConfiguration,
                                          ExportImportCompound exportImportCompound) {
        return new CompoundConfiguration().toBuilder()
                .id(exportImportCompound.getId())
                .name(exportImportCompound.getName())
                .sourceId(exportImportCompound.getSourceId())
                .referToInput(exportImportCompound.getReferToInput())
                .processes(new ArrayList<>())
                .inSections(new ArrayList<>())
                .build();
    }

    @Override
    public List<UUID> validate(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path)
            throws MiaException {
        List<ExportImportCompound> loadedCompounds = loadConfiguration(importData, path);
        loadedCompounds.forEach(c -> loadCompound(projectConfiguration, new ArrayList<>(), c, importData));
        return loadedCompounds.stream().map(ExportImportCompound::getId).collect(Collectors.toList());
    }

    private void loadCompound(ProjectConfiguration projectConfiguration,
                              List<CompoundConfiguration> loadedCompounds,
                              ExportImportCompound importCompound,
                              ExportImportData importData) {
        if (loadedCompounds.stream().noneMatch(s -> importCompound.getId().equals(s.getId()))) {
            CompoundConfiguration compound = toEntity(projectConfiguration, importCompound);
            List<SectionConfiguration> inSections = SectionConfigurationService
                    .filterSectionsByIdOrSourceId(projectConfiguration.getSections(),
                            importData.isInterProjectImport() || importData.isCreateNewProject()
                            ? replaceIdsBack(importCompound.getInSections(), importData.getReplacementMap())
                            : importCompound.getInSections());
            compound.setProcesses(
                    filterProcessesByIdOrSourceId(projectConfiguration.getProcesses(),
                            importData.isInterProjectImport() || importData.isCreateNewProject()
                                    ? replaceIdsBack(importCompound.getProcesses(), importData.getReplacementMap())
                                    : importCompound.getProcesses()));
            compound.setInSections(inSections);
            loadedCompounds.add(compound);
        }
    }

    private void mergeCompoundOrAdd(ProjectConfiguration projectConfiguration,
                                    CompoundConfiguration process) {
        if (process.getSourceId() != null) {
            mergeCompoundOrAdd(projectConfiguration, process, process.getSourceId());
        } else {
            mergeCompoundOrAdd(projectConfiguration, process, process.getId());
        }
    }

    private void mergeCompoundOrAdd(ProjectConfiguration projectConfiguration,
                                    CompoundConfiguration compound,
                                    UUID compoundId) {
        Optional<CompoundConfiguration> realCompoundOptional = projectConfiguration.getCompounds().stream()
                .filter(realC -> compoundId.equals(realC.getId()) || compoundId.equals(realC.getSourceId()))
                .findFirst();
        if (realCompoundOptional.isPresent()) {
            // migrate with real section
            CompoundConfiguration realCompound = realCompoundOptional.get();
            realCompound.setName(compound.getName());
            realCompound.setSourceId(compound.getSourceId());
            realCompound.setReferToInput(compound.getReferToInput());
            //migrate sections
            Set<SectionConfiguration> sections = new HashSet<>(realCompound.getInSections());
            compound.getInSections().forEach(s -> {
                s.addCompound(realCompound);
                sections.add(s);
            });
            realCompound.setInSections(new ArrayList<>(sections));
            //migrate processes
            compound.setProcesses(realCompound.getProcesses());
            realCompound.getProcesses().forEach(p -> {
                if (!p.getInCompounds().contains(realCompound)) {
                    p.getInCompounds().add(realCompound);
                }
            });

        } else {
            //Add new compound
            if (projectConfiguration.getCompounds().stream().anyMatch(c -> compound.getName().equals(c.getName()))) {
                compound.setName(compound.getName() + "_ei_" + UUID.randomUUID());
            }
            compound.setProjectConfiguration(projectConfiguration);
            //migrate sections
            compound.getInSections().forEach(s -> s.addCompound(compound));
            //migrate processes
            compound.getProcesses().forEach(p -> p.getInCompounds().add(compound));
            projectConfiguration.getCompounds().add(compound);
        }
    }
}
