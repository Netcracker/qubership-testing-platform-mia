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

import static org.qubership.atp.mia.service.configuration.CompoundConfigurationService.filterCompounds;
import static org.qubership.atp.mia.service.configuration.ProcessConfigurationService.filterProcesses;
import static org.qubership.atp.mia.utils.Utils.correctPlaceInList;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.ws.Holder;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.qubership.atp.mia.controllers.api.dto.CompoundDto;
import org.qubership.atp.mia.controllers.api.dto.SectionDto;
import org.qubership.atp.mia.controllers.api.dto.SectionsDto;
import org.qubership.atp.mia.exceptions.configuration.CreateSectionException;
import org.qubership.atp.mia.exceptions.configuration.DeleteSectionException;
import org.qubership.atp.mia.exceptions.configuration.DuplicateSectionException;
import org.qubership.atp.mia.exceptions.configuration.SectionCyclicDependencyException;
import org.qubership.atp.mia.exceptions.configuration.SectionNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.UpdateSectionException;
import org.qubership.atp.mia.exceptions.history.MiaHistoryRevisionRestoreException;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.repo.configuration.SectionConfigurationRepository;
import org.qubership.atp.mia.service.history.impl.AbstractEntityHistoryService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class SectionConfigurationService extends AbstractEntityHistoryService<SectionConfiguration> {

    private final ModelMapper modelMapper;
    private final ProjectConfigurationService projectConfigurationService;
    private final SectionConfigurationRepository sectionConfigurationRepository;

    /**
     * Add section.
     *
     * @param projectConfiguration projectConfiguration
     * @param sectionDto           {@link SectionDto}
     * @return list of {@link SectionDto} from root
     */
    public List<SectionsDto> addSection(ProjectConfiguration projectConfiguration, SectionDto sectionDto) {
        log.info("Attempting to create section: '{}'", sectionDto);
        if (projectConfiguration.getAllSections().stream().anyMatch(s -> {
            if (s.getName().equals(sectionDto.getName())) {
                if (s.getParentSection() != null) {
                    return s.getParentSection().getId().equals(sectionDto.getParentSection());
                } else {
                    return sectionDto.getParentSection() == null;
                }
            }
            return false;
        })) {
            log.error("Duplicate section detected: '{}'", sectionDto.getName());
            throw new DuplicateSectionException(sectionDto.getName());
        }
        try {
            SectionConfiguration parentSection = sectionDto.getParentSection() == null
                    ? null
                    : getSectionById(projectConfiguration, sectionDto.getParentSection());
            SectionConfiguration sectionConfiguration = SectionConfiguration.builder()
                    .projectConfiguration(projectConfiguration)
                    .name(sectionDto.getName())
                    .parentSection(parentSection)
                    .sections(sectionDto.getSections() == null
                            ? new ArrayList<>()
                            : filterSections(
                            projectConfiguration.getAllSections(),
                            sectionDto.getSections().stream().map(s -> s.getId()).collect(Collectors.toList())))
                    .compounds(sectionDto.getCompounds() == null
                            ? new ArrayList<>()
                            : filterCompounds(
                            projectConfiguration.getCompounds(),
                            sectionDto.getCompounds().stream().map(c -> c.getId()).collect(Collectors.toList())))
                    .processes(sectionDto.getProcesses() == null
                            ? new ArrayList<>()
                            : filterProcesses(projectConfiguration.getProcesses(), sectionDto.getProcesses()))
                    .build();
            if (parentSection != null) {
                addSectionAtCorrectPlace(parentSection.getSections(), sectionConfiguration, sectionDto.getPlace());
            } else {
                addSectionAtCorrectPlace(projectConfiguration.getSections(),
                        sectionConfiguration, sectionDto.getPlace());
            }
            log.debug("Reordering sections after adding section '{}'", sectionConfiguration.getId());
            List<SectionConfiguration> sectionsToSync = reorderSections(sectionConfiguration, sectionDto.getPlace());
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        sectionConfigurationRepository.save(sectionConfiguration);
                        sectionConfigurationRepository.saveAll(sectionsToSync);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully created section '{}'", sectionConfiguration);
        } catch (Exception e) {
            log.error("Error while creating section '{}': {}", sectionDto.getName(), e.getMessage(), e);
            throw new CreateSectionException(e);
        }
        return sectionsDto(projectConfiguration.getRootSections());
    }

    /**
     * Delete section.
     *
     * @param projectConfiguration projectConfiguration
     * @param sectionId            section ID
     * @return list of {@link SectionDto} from root
     */
    public List<SectionsDto> deleteSection(ProjectConfiguration projectConfiguration, UUID sectionId) {
        log.info("Attempting to delete section with ID '{}'", sectionId);
        Optional<SectionConfiguration> optionalSectionConfiguration =
                projectConfiguration.getAllSections().stream().filter(s -> s.getId().equals(sectionId)).findAny();
        if (!optionalSectionConfiguration.isPresent()) {
            throw new SectionNotFoundException(sectionId);
        }
        try {
            SectionConfiguration sectionConfiguration = optionalSectionConfiguration.get();
            List<SectionConfiguration> siblingsToBeSaved = new ArrayList<>();
            projectConfiguration.removeSection(sectionConfiguration);
            if (sectionConfiguration.getParentSection() != null) {
                SectionConfiguration parentSectionConfiguration =
                        getSectionById(projectConfiguration, sectionConfiguration.getParentSection().getId());
                parentSectionConfiguration.getSections().remove(sectionConfiguration);
                parentSectionConfiguration.reorderChildSequentially();
                siblingsToBeSaved.addAll(parentSectionConfiguration.getSections());
            } else {
                siblingsToBeSaved.addAll(reorderSiblings(projectConfiguration.getRootSections()));
            }
            log.debug("Executing database delete for section '{}'", sectionConfiguration.getName());
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        sectionConfigurationRepository.saveAll(siblingsToBeSaved);
                        sectionConfigurationRepository.delete(sectionConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully deleted section '{}'", sectionConfiguration);
        } catch (Exception e) {
            log.error("Error while deleting section with ID '{}': {}", sectionId, e.getMessage(), e);
            throw new DeleteSectionException(e);
        }
        return sectionsDto(projectConfiguration.getRootSections());
    }

    /**
     * Filter sections by UUIDs.
     *
     * @param sections     section to be filter
     * @param sectionsUuid UUIDs of sections to filter
     * @return filtered sections.
     */
    public static List<SectionConfiguration> filterSections(List<SectionConfiguration> sections,
                                                            List<UUID> sectionsUuid) {
        log.trace("Filtering {} sections with {} section UUIDs", sections.size(), sectionsUuid.size());
        List<SectionConfiguration> filteredSections = new LinkedList<>();
        sectionsUuid.forEach(sectionUUID -> sections.stream().filter(s -> s.getId().equals(sectionUUID)).findAny()
                .ifPresent(filteredSections::add));
        return filteredSections;
    }

    /**
     * Filter sections by ID or source ID.
     *
     * @param sections     section to be filter
     * @param sectionsUuid UUIDs of sections to filter
     * @return filtered sections.
     */
    public static List<SectionConfiguration> filterSectionsByIdOrSourceId(List<SectionConfiguration> sections,
                                                                          List<UUID> sectionsUuid) {
        log.trace("Filtering sections by ID or source ID, input size: {}", sectionsUuid.size());
        List<SectionConfiguration> filteredSections = new LinkedList<>();
        sectionsUuid.forEach(sUuid -> sections.stream()
                .filter(s -> sUuid != null && (sUuid.equals(s.getId()) || sUuid.equals(s.getSourceId())))
                .findAny()
                .ifPresent(filteredSections::add));
        return filteredSections;
    }

    /**
     * find section from miaPath, if section is not present then create too.
     *
     * @param projectId project ID
     */
    public SectionConfiguration findSectionByPath(UUID projectId, String miaPath) {
        log.debug("Finding section by path '{}' in project '{}'", miaPath, projectId);
        ProjectConfiguration projectConfiguration = projectConfigurationService.getConfigByProjectId(projectId);
        String[] sectionNames = miaPath.split("/");
        Optional<SectionConfiguration> sectionConfig;
        SectionConfiguration section = null;
        List<SectionConfiguration> sectionByLevel = projectConfiguration.getRootSections();
        Holder<UUID> parentSectionId = new Holder<>(null);
        for (String sectionName : sectionNames) {
            sectionConfig = sectionByLevel.stream().filter(s -> {
                if (s.getParentSection() == null) {
                    return parentSectionId.value == null && s.getName().equalsIgnoreCase(sectionName);
                } else {
                    return s.getParentSection().getId().equals(parentSectionId.value)
                            && s.getName().equalsIgnoreCase(sectionName);
                }
            }).findAny();
            if (!sectionConfig.isPresent()) {
                throw new SectionNotFoundException(sectionName);
            } else {
                section = sectionConfig.get();
            }
            parentSectionId.value = section.getId();
            sectionByLevel = section.getSections();
        }
        if (section == null) {
            throw new SectionNotFoundException(miaPath);
        }
        log.debug("Found section '{}' for path '{}'", section.getName(), miaPath);
        return section;
    }

    /**
     * Map list of SectionConfiguration to list of SectionsDto.
     *
     * @param sectionConfigurationList list of {@link SectionConfiguration}
     * @return list of {@link SectionsDto}
     */
    public List<SectionsDto> sectionsDto(List<SectionConfiguration> sectionConfigurationList) {
        log.trace("Converting {} section configurations to DTOs",
                sectionConfigurationList != null ? sectionConfigurationList.size() : 0);
        if (sectionConfigurationList != null) {
            Type listType = new TypeToken<List<SectionsDto>>() {
            }.getType();
            return modelMapper.map(sectionConfigurationList, listType);
        }
        return new ArrayList<>();
    }

    /**
     * Update section.
     *
     * @param projectConfiguration projectConfiguration
     * @param sectionDto           {@link SectionDto}
     * @return list of {@link SectionDto} from root
     */
    public List<SectionsDto> updateSection(ProjectConfiguration projectConfiguration, SectionDto sectionDto) {
        log.info("Attempting to update section: '{}'", sectionDto);
        SectionConfiguration sectionConfiguration = getSectionById(projectConfiguration, sectionDto.getId());
        // Check for the cyclic section update
        if (sectionDto.getParentSection() != null
                && sectionConfiguration.getChildrenUuid().contains(sectionDto.getParentSection())) {
            throw new SectionCyclicDependencyException(sectionDto.getId(),
                    sectionDto.getParentSection());
        }
        try {
            sectionConfiguration.setName(sectionDto.getName());
            SectionConfiguration oldParent = sectionConfiguration.getParentSection();
            SectionConfiguration newParent = sectionDto.getParentSection() != null
                    ? getSectionById(projectConfiguration, sectionDto.getParentSection())
                    : null;
            List<SectionConfiguration> sectionsToSync = new ArrayList<>();
            log.debug("Checking if the old parent '{}' and new parent '{}' are correctly assigned",
                    oldParent != null ? oldParent.getName() : "N/A",
                    newParent != null ? newParent.getName() : "N/A");
            if (!Objects.equals(oldParent, newParent)) {
                if (oldParent != null) {
                    oldParent.getSections().remove(sectionConfiguration);
                    oldParent.reorderChildSequentially();
                    sectionsToSync.addAll(oldParent.getSections());
                } else {
                    projectConfiguration.removeSection(sectionConfiguration);
                    sectionsToSync.addAll(reorderSiblings(projectConfiguration.getRootSections()));
                }
                if (newParent != null) {
                    addSectionAtCorrectPlace(newParent.getSections(), sectionConfiguration, sectionDto.getPlace());
                } else {
                    addSectionAtCorrectPlace(projectConfiguration.getRootSections(),
                            sectionConfiguration, sectionDto.getPlace());
                }
                sectionConfiguration.setParentSection(newParent);
                sectionsToSync.addAll(reorderSections(sectionConfiguration, sectionDto.getPlace()));
            } else if (sectionConfiguration.getPlace() != sectionDto.getPlace()) {
                sectionsToSync.addAll(reorderSections(sectionConfiguration, sectionDto.getPlace()));
            } else {
                sectionsToSync.add(sectionConfiguration);
            }
            if (sectionDto.getSections() != null) {
                sectionConfiguration.setSections(filterSections(projectConfiguration.getAllSections(),
                        sectionDto.getSections().stream().map(SectionDto::getId).collect(Collectors.toList())));
            }
            if (sectionDto.getCompounds() != null) {
                sectionConfiguration.setCompounds(filterCompounds(projectConfiguration.getCompounds(),
                        sectionDto.getCompounds().stream().map(CompoundDto::getId).collect(Collectors.toList())));
            }
            if (sectionDto.getProcesses() != null) {
                sectionConfiguration.setProcesses(filterProcesses(projectConfiguration.getProcesses(),
                        sectionDto.getProcesses()));
            }
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        sectionConfigurationRepository.saveAll(sectionsToSync);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully updated section '{}'", sectionConfiguration);
        } catch (Exception e) {
            log.error("Error while updating section '{}': {}", sectionDto.getName(), e.getMessage());
            throw new UpdateSectionException(e);
        }
        return sectionsDto(projectConfiguration.getRootSections());
    }

    /**
     * Get section by ID.
     *
     * @param sectionId project section ID
     * @return {@link SectionConfiguration}
     */
    private SectionConfiguration getSectionById(ProjectConfiguration projectConfiguration, UUID sectionId) {
        return projectConfiguration.getAllSections().stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Section not found with ID '{}'", sectionId);
                    return new SectionNotFoundException(sectionId);
                });
    }

    /**
     * Reorders sections and updates their places.
     *
     * @param currentSection section being positioned
     * @param targetPlace    desired position for section
     * @return ordered list of sections with updated positions
     */
    private List<SectionConfiguration> reorderSections(SectionConfiguration currentSection, int targetPlace) {
        log.trace("Reordering section '{}' to position {}", currentSection.getName(), targetPlace);
        List<SectionConfiguration> siblingOrChildList = currentSection.getParentSection() == null
                ? currentSection.getProjectConfiguration().getRootSections()
                : currentSection.getParentSection().getSections();
        List<SectionConfiguration> orderedSections = new ArrayList<>();
        siblingOrChildList.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getId() != currentSection.getId())
                .forEach(s -> orderedSections.add(correctPlaceInList(orderedSections, s.getPlace()), s));
        orderedSections.add(correctPlaceInList(orderedSections, targetPlace), currentSection);
        IntStream.range(0, orderedSections.size())
                .forEach(place -> orderedSections.get(place).setPlace(place));
        return orderedSections;
    }

    /**
     * Reorders list of sibling sections sequentially starting from 0.
     *
     * @param sections list of sections to order
     * @return list of sections with updated positions
     */
    private List<SectionConfiguration> reorderSiblings(List<SectionConfiguration> sections) {
        log.trace("Reordering {} sibling sections", sections != null ? sections.size() : 0);
        if (sections == null || sections.isEmpty()) {
            return Collections.emptyList();
        }
        List<SectionConfiguration> orderedSections = new ArrayList<>();
        sections.stream().filter(Objects::nonNull).forEach(orderedSections::add);
        IntStream.range(0, orderedSections.size()).forEach(place -> orderedSections.get(place).setPlace(place));
        return orderedSections;
    }

    @Override
    public SectionConfiguration get(UUID id) {
        return sectionConfigurationRepository.findById(id).orElseThrow(MiaHistoryRevisionRestoreException::new);
    }

    @Override
    public SectionConfiguration restore(DateAuditorEntity entity) {
        log.info("Restoring section configuration '{}'", entity);
        SectionConfiguration sectionConfiguration = (SectionConfiguration) entity;
        Optional<SectionConfiguration> currentSectionOptional =
                sectionConfigurationRepository.findById(sectionConfiguration.getId());
        ProjectConfiguration projectConfiguration = sectionConfiguration.getProjectConfiguration();

        SectionConfiguration newParent = sectionConfiguration.getParentSection();
        SectionConfiguration oldParent = currentSectionOptional
                .map(SectionConfiguration::getParentSection).orElse(null);
        boolean parentChanged = !Objects.equals(oldParent, newParent);
        if (parentChanged) {
            if (newParent != null) {
                newParent.getSections().add(sectionConfiguration);
            }
            if (oldParent != null) {
                oldParent.getSections().remove(sectionConfiguration);
            }
        }

        projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                () -> {
                    sectionConfigurationRepository.save(sectionConfiguration);
                    return projectConfiguration;
                }, false);
        log.info("Successfully restored section configuration '{}'", sectionConfiguration.getName());
        return sectionConfiguration;
    }

    /**
     * Adds a section to the target list at the correct position, ensuring the place is within valid bounds.
     *
     * @param sections       list to add to
     * @param section        section to add
     * @param requestedPlace desired position
     */
    private void addSectionAtCorrectPlace(List<SectionConfiguration> sections,
                                          SectionConfiguration section, int requestedPlace) {
        int correctPlace = correctPlaceInList(sections, requestedPlace);
        section.setPlace(correctPlace);
        sections.add(correctPlace, section);
    }
}
