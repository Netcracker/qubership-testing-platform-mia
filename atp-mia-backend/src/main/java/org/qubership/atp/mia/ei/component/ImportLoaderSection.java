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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.mia.ei.service.AtpImportStrategy;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.configuration.SectionNotFoundException;
import org.qubership.atp.mia.exceptions.ei.MiaImportSectionException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImportLoaderSection extends
        ImportLoader<ExportImportEntities, SectionConfiguration, ExportImportSection> {

    public ImportLoaderSection(@Autowired ObjectLoaderFromDiskService objectLoaderFromDiskService) {
        super(objectLoaderFromDiskService);
    }

    @Override
    public Class<ExportImportSection> getClazz() {
        return ExportImportSection.class;
    }

    @Override
    public ExportImportEntities getEntityType() {
        return ExportImportEntities.MIA_SECTION;
    }

    @Override
    public void importEntity(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path) {
        List<SectionConfiguration> loadedSections = new ArrayList<>();
        List<ExportImportSection> importSections = loadConfiguration(importData, path);
        importSections.forEach(s -> loadSection(projectConfiguration, loadedSections, importSections, s));
        loadedSections.forEach(s -> mergeSectionOrAdd(projectConfiguration, s, new ArrayList<>()));
    }

    @Override
    public SectionConfiguration toEntity(ProjectConfiguration projectConfiguration,
                                         ExportImportSection exportImportSection) {
        return new SectionConfiguration().toBuilder()
                .id(exportImportSection.getId())
                .name(exportImportSection.getName())
                .sourceId(exportImportSection.getSourceId())
                .parentSection(null)
                .sections(new ArrayList<>())
                .processes(new ArrayList<>())
                .compounds(new ArrayList<>())
                .build();
    }

    @Override
    public List<UUID> validate(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path)
            throws MiaException {
        List<ExportImportSection> importSections = loadConfiguration(importData, path);
        importSections.forEach(s -> loadSection(projectConfiguration, new ArrayList<>(), importSections, s));
        return importSections.stream().map(ExportImportSection::getId).collect(Collectors.toList());
    }

    private void loadSection(ProjectConfiguration projectConfiguration,
                             List<SectionConfiguration> loadedSections,
                             List<ExportImportSection> importSections,
                             ExportImportSection importSection) {
        if (loadedSections.stream().noneMatch(s -> importSection.getId().equals(s.getId()))) {

            if (importSection.getParentSection() != null) {
                Optional<SectionConfiguration> parentSectionOptional = loadedSections.stream()
                        .filter(s -> importSection.getParentSection().equals(s.getId())).findAny();

                if (parentSectionOptional.isPresent()) {
                    SectionConfiguration loadedSection = toEntity(projectConfiguration, importSection);
                    loadedSection.setParentSection(parentSectionOptional.get());
                    parentSectionOptional.get().addSection(loadedSection);
                    loadedSections.add(loadedSection);
                } else {
                    Optional<ExportImportSection> importSectionOptional = importSections.stream()
                            .filter(s -> importSection.getParentSection().equals(s.getId())).findAny();

                    if (importSectionOptional.isPresent()) {
                        loadSection(projectConfiguration, loadedSections, importSections, importSectionOptional.get());

                        parentSectionOptional = loadedSections.stream()
                                .filter(s -> importSection.getParentSection().equals(s.getId())).findAny();
                        if (parentSectionOptional.isPresent()) {
                            SectionConfiguration loadedSection = toEntity(projectConfiguration, importSection);
                            loadedSection.setParentSection(parentSectionOptional.get());
                            parentSectionOptional.get().addSection(loadedSection);
                            loadedSections.add(loadedSection);
                        }
                    }

                    if (!parentSectionOptional.isPresent()) {
                        parentSectionOptional = projectConfiguration.getSections().stream()
                                .filter(s -> importSection.getParentSection().equals(s.getId()))
                                .findAny();
                        if (parentSectionOptional.isPresent()) {
                            SectionConfiguration loadedSection = toEntity(projectConfiguration, importSection);
                            loadedSection.setParentSection(parentSectionOptional.get());
                            loadedSections.add(loadedSection);
                        } else {
                            throw new MiaImportSectionException(importSection);
                        }
                    }
                }
            } else {
                loadedSections.add(toEntity(projectConfiguration, importSection));
            }
        }
    }

    private SectionConfiguration mergeSectionOrAdd(ProjectConfiguration projectConfiguration,
                                                   SectionConfiguration section,
                                                   List<SectionConfiguration> mergedSections) {
        return mergedSections.stream()
                .filter(s -> s.getId().equals(section.getId()))
                .findFirst()
                .orElseGet(() ->
                        section.getSourceId() != null
                                ? mergeSectionOrAdd(
                                projectConfiguration, section, section.getSourceId(), mergedSections)
                                : mergeSectionOrAdd(
                                projectConfiguration, section, section.getId(), mergedSections));
    }

    private SectionConfiguration mergeSectionOrAdd(ProjectConfiguration projectConfiguration,
                                                   SectionConfiguration section,
                                                   UUID sectionId,
                                                   List<SectionConfiguration> mergedSections) {
        Optional<SectionConfiguration> realSectionOptional = projectConfiguration.getSections().stream()
                .filter(realS -> sectionId.equals(realS.getId())
                        || sectionId.equals(realS.getSourceId()))
                .findFirst();
        if (realSectionOptional.isPresent()) {
            // migrate with real section
            SectionConfiguration realSection = realSectionOptional.get();
            realSection.setName(section.getName());
            realSection.setSourceId(section.getSourceId());
            if (section.getParentSection() != null) {
                SectionConfiguration parentSectionBefore = realSection.getParentSection();
                SectionConfiguration parentSectionAfter =
                        mergeSectionOrAdd(projectConfiguration, section.getParentSection(), mergedSections);
                parentSectionAfter = parentSectionAfter != null
                        ? parentSectionAfter
                        : projectConfiguration.getSections().stream().filter(
                                s -> section.getParentSection().getId().equals(s.getId()))
                        .findAny()
                        .orElseThrow(() -> new SectionNotFoundException(section.getParentSection().getId()));
                if (parentSectionBefore != null && !parentSectionBefore.getId().equals(parentSectionAfter.getId())) {
                    realSection.setParentSection(parentSectionAfter);
                    parentSectionAfter.addSection(realSection);
                    parentSectionBefore.getSections().remove(realSection);
                }
            } else {
                realSection.setParentSection(null);
            }
            mergedSections.add(realSection);
            return realSection;
        } else {
            //Add new section
            section.setProjectConfiguration(projectConfiguration);
            projectConfiguration.getSections().add(section);
            mergedSections.add(section);
            boolean duplicateName;
            if (section.getParentSection() != null) {
                SectionConfiguration parentSection =
                        mergeSectionOrAdd(projectConfiguration, section.getParentSection(), mergedSections);
                parentSection = parentSection != null
                        ? parentSection
                        : projectConfiguration.getSections().stream().filter(
                                s -> section.getParentSection().getId().equals(s.getId()))
                        .findAny().orElseThrow(() -> new SectionNotFoundException(section.getParentSection().getId()));
                parentSection.addSection(section);
                duplicateName = parentSection.getSections()
                        .stream().anyMatch(s -> section.getName().equals(s.getName())
                                && !section.getId().equals(s.getId()));
            } else {
                duplicateName = projectConfiguration.getRootSections()
                        .stream().anyMatch(s -> section.getName().equals(s.getName())
                                && !section.getId().equals(s.getId()));
            }
            if (duplicateName) {
                section.setName(section.getName() + EI_CONFLICT + AtpImportStrategy.IMPORT_TIMESTAMP.get());
            }
            return section;
        }
    }
}
