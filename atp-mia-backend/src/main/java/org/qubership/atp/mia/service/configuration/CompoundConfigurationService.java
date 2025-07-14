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

import static org.qubership.atp.mia.service.configuration.ProcessConfigurationService.checkProcessesById;
import static org.qubership.atp.mia.service.configuration.ProcessConfigurationService.filterProcesses;
import static org.qubership.atp.mia.service.configuration.SectionConfigurationService.filterSections;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.qubership.atp.mia.controllers.api.dto.CompoundDto;
import org.qubership.atp.mia.controllers.api.dto.CompoundShortDto;
import org.qubership.atp.mia.exceptions.configuration.CompoundNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.CreateCompoundException;
import org.qubership.atp.mia.exceptions.configuration.DeleteCompoundException;
import org.qubership.atp.mia.exceptions.configuration.DuplicateCompoundException;
import org.qubership.atp.mia.exceptions.configuration.UpdateCompoundException;
import org.qubership.atp.mia.exceptions.history.MiaHistoryRevisionRestoreException;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.repo.configuration.CompoundConfigurationRepository;
import org.qubership.atp.mia.service.history.impl.AbstractEntityHistoryService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class CompoundConfigurationService extends AbstractEntityHistoryService<CompoundConfiguration> {

    private final CompoundConfigurationRepository compoundConfigurationRepository;
    private final ModelMapper modelMapper;
    private final ProjectConfigurationService projectConfigurationService;

    /**
     * Add compound.
     *
     * @param projectConfiguration projectConfiguration
     * @param compoundDto          {@link CompoundDto}
     * @return {@link CompoundDto}
     */
    public CompoundDto addCompound(ProjectConfiguration projectConfiguration, CompoundDto compoundDto) {
        log.info("Attempting to create compound: '{}'", compoundDto);
        if (projectConfiguration.getCompounds().stream()
                .anyMatch(cc -> cc.getName().equals(compoundDto.getName()))) {
            log.error("Duplicate compound detected: '{}'", compoundDto.getName());
            throw new DuplicateCompoundException(compoundDto.getName());
        }
        try {
            CompoundConfiguration compoundConfiguration = modelMapper.map(compoundDto, CompoundConfiguration.class);
            compoundConfiguration.setProjectConfiguration(projectConfiguration);
            projectConfiguration.getCompounds().add(compoundConfiguration);
            log.debug("Updating sections and processes for new compound '{}'", compoundDto.getName());
            syncCompoundSectionsAndProcesses(projectConfiguration, compoundConfiguration, compoundDto);
            compoundConfiguration.setModifiedWhen(new Timestamp(System.currentTimeMillis()));
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        compoundConfigurationRepository.save(compoundConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully created compound '{}'", compoundConfiguration);
            return toDto(compoundConfiguration);
        } catch (Exception e) {
            log.error("Error while creating compound '{}': {}", compoundDto.getName(), e.getMessage());
            throw new CreateCompoundException(e);
        }
    }

    /**
     * Map list of CompoundConfiguration to list of CompoundShortDto.
     *
     * @param compoundConfigurationList list of {@link CompoundConfiguration}
     * @return list of {@link CompoundShortDto}
     */
    public List<CompoundShortDto> compoundsDto(List<CompoundConfiguration> compoundConfigurationList) {
        log.trace("Converting {} compound configurations to DTOs", compoundConfigurationList.size());
        Type listType = new TypeToken<List<CompoundShortDto>>() {
        }.getType();
        return modelMapper.map(compoundConfigurationList, listType);
    }

    /**
     * Delete compound.
     *
     * @param projectConfiguration projectConfiguration
     * @param compoundId           compound ID
     * @return list of {@link CompoundDto}
     */
    public List<CompoundShortDto> deleteCompound(ProjectConfiguration projectConfiguration, UUID compoundId) {
        log.info("Attempting to delete compound with ID '{}'", compoundId);
        CompoundConfiguration compoundConfiguration = getCompoundById(projectConfiguration, compoundId);
        try {
            log.debug("Removing compound '{}' from sections and processes", compoundConfiguration.getName());
            projectConfiguration.getAllSections().forEach(s -> s.getCompounds().remove(compoundConfiguration));
            projectConfiguration.getProcesses().forEach(p -> {
                p.getInCompounds().remove(compoundConfiguration);
            });
            compoundConfiguration.setInSections(new ArrayList<>());
            compoundConfiguration.setProcesses(new ArrayList<>());
            projectConfiguration.getCompounds().remove(compoundConfiguration);
            log.debug("Executing database delete for compound '{}'", compoundConfiguration.getName());
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        compoundConfigurationRepository.delete(compoundConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully deleted compound '{}'", compoundConfiguration);
        } catch (Exception e) {
            log.error("Error while deleting compound with ID '{}': {}", compoundConfiguration.getId(), e.getMessage());
            throw new DeleteCompoundException(e);
        }
        return compoundsDto(projectConfiguration.getCompounds());
    }

    /**
     * Filter compounds by UUIDs.
     *
     * @param compounds     compounds
     * @param compoundsUuid compounds UUIDs to filter
     * @return filtered compound.
     */
    public static LinkedList<CompoundConfiguration> filterCompounds(List<CompoundConfiguration> compounds,
                                                                    List<UUID> compoundsUuid) {
        log.trace("Filtering {} compounds with {} compound UUIDs", compounds.size(), compoundsUuid.size());
        LinkedList<CompoundConfiguration> filteredCompounds = new LinkedList<>();
        compoundsUuid.forEach(compoundUUID -> compounds.stream().filter(s -> s.getId().equals(compoundUUID)).findAny()
                .ifPresent(filteredCompounds::add));
        return filteredCompounds;
    }

    /**
     * Map list of CompoundConfiguration to list of CompoundDto.
     *
     * @param compoundConfiguration list of {@link CompoundConfiguration}
     * @return list of {@link CompoundDto}
     */
    public CompoundDto toDto(CompoundConfiguration compoundConfiguration) {
        return modelMapper.map(compoundConfiguration, CompoundDto.class);
    }

    /**
     * Update compound.
     *
     * @param projectConfiguration projectConfiguration
     * @param compoundDto          {@link CompoundDto}
     * @return {@link CompoundDto}
     */
    public CompoundDto updateCompound(ProjectConfiguration projectConfiguration, CompoundDto compoundDto) {
        log.info("Attempting to update compound '{}'", compoundDto);
        CompoundConfiguration compoundConfiguration = projectConfiguration.getCompounds().stream()
                .filter(cf -> cf.getId().equals(compoundDto.getId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Compound not found for update with ID '{}'", compoundDto.getId());
                    return new CompoundNotFoundException(compoundDto.getId());
                });
        try {
            log.debug("Updating compound properties for '{}'", compoundDto.getName());
            compoundConfiguration.setName(compoundDto.getName());
            compoundConfiguration.setReferToInput(compoundDto.getReferToInput());
            syncCompoundSectionsAndProcesses(projectConfiguration, compoundConfiguration, compoundDto);
            compoundConfiguration.setModifiedWhen(new Timestamp(System.currentTimeMillis()));
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        compoundConfigurationRepository.save(compoundConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully updated compound '{}'", compoundDto.getName());
            return toDto(compoundConfiguration);
        } catch (Exception e) {
            log.error("Error while updating compound '{}': {}", compoundDto.getName(), e.getMessage());
            throw new UpdateCompoundException(e);
        }
    }

    /**
     * Get compound by ID.
     *
     * @param compoundId project compound ID
     * @return {@link SectionConfiguration}
     */
    public CompoundConfiguration getCompoundById(ProjectConfiguration projectConfiguration, UUID compoundId) {
        log.trace("Founding compound with ID '{}'", compoundId);
        return projectConfiguration.getCompounds().stream()
                .filter(c -> c.getId().equals(compoundId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Compound not found: ID '{}'", compoundId);
                    return new CompoundNotFoundException(compoundId);
                });
    }

    private void syncCompoundSectionsAndProcesses(ProjectConfiguration projectConfiguration,
                                                  CompoundConfiguration compoundConfiguration,
                                                  CompoundDto compoundDto) {
        if (compoundDto.getInSections() != null) {
            log.debug("Updating sections for compound '{}'", compoundConfiguration.getName());
            List<SectionConfiguration> sectionBefore = compoundConfiguration.getInSections();
            List<SectionConfiguration> sectionConfigurationList = filterSections(projectConfiguration.getAllSections(),
                    compoundDto.getInSections());
            compoundConfiguration.setInSections(sectionConfigurationList);
            sectionConfigurationList.forEach(s -> {
                if (!s.getCompounds().contains(compoundConfiguration)) {
                    s.getCompounds().add(compoundConfiguration);
                }
            });
            if (sectionBefore != null) {
                sectionBefore.stream().filter(s -> !sectionConfigurationList.contains(s))
                        .forEach(s -> s.getCompounds().remove(compoundConfiguration));
            }
            log.debug("Successfully updated sections for compound '{}'", compoundConfiguration.getName());
        }
        if (compoundDto.getProcesses() != null) {
            log.debug("Updating processes for compound '{}'", compoundConfiguration.getName());
            List<ProcessConfiguration> processConfigurationList = filterProcesses(projectConfiguration.getProcesses(),
                    compoundDto.getProcesses());
            compoundConfiguration.setProcesses(processConfigurationList);
            log.debug("Successfully updated processes for compound '{}'", compoundConfiguration.getName());
        }
    }

    @Override
    public CompoundConfiguration get(UUID id) {
        return compoundConfigurationRepository.findById(id).orElseThrow(MiaHistoryRevisionRestoreException::new);
    }

    @Override
    public CompoundConfiguration restore(DateAuditorEntity entity) {
        log.info("Restoring compound configuration '{}'", entity);
        CompoundConfiguration compoundConfiguration = (CompoundConfiguration) entity;
        ProjectConfiguration projectConfiguration = compoundConfiguration.getProjectConfiguration();
        checkProcessesById(projectConfiguration, compoundConfiguration.getProcesses());
        compoundConfiguration.setModifiedWhen(new Timestamp(System.currentTimeMillis()));
        projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                () -> {
                    compoundConfigurationRepository.save(compoundConfiguration);
                    return projectConfiguration;
                }, false);
        log.info("Successfully restored compound configuration '{}'", compoundConfiguration.getName());
        return compoundConfiguration;
    }
}
