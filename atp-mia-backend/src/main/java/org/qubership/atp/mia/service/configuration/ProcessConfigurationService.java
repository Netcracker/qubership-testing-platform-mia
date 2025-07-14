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

import static org.qubership.atp.mia.service.configuration.SectionConfigurationService.filterSections;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.qubership.atp.mia.controllers.api.dto.ProcessDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessShortDto;
import org.qubership.atp.mia.exceptions.configuration.CompoundNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.CreateProcessException;
import org.qubership.atp.mia.exceptions.configuration.DeleteProcessException;
import org.qubership.atp.mia.exceptions.configuration.DuplicateProcessException;
import org.qubership.atp.mia.exceptions.configuration.ProcessNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.UpdateProcessException;
import org.qubership.atp.mia.exceptions.history.MiaHistoryRevisionRestoreException;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.repo.configuration.ProcessConfigurationRepository;
import org.qubership.atp.mia.service.history.impl.AbstractEntityHistoryService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProcessConfigurationService extends AbstractEntityHistoryService<ProcessConfiguration> {

    private final ModelMapper modelMapper;
    private final ProcessConfigurationRepository processConfigurationRepository;
    private final ProjectConfigurationService projectConfigurationService;

    /**
     * Add process.
     *
     * @param projectConfiguration projectConfiguration
     * @param processDto           {@link ProcessDto}
     * @return {@link ProcessDto}
     */
    public ProcessDto addProcess(ProjectConfiguration projectConfiguration, ProcessDto processDto) {
        log.info("Attempting to create process: '{}'", processDto);
        if (projectConfiguration.getProcesses().stream().anyMatch(pc -> pc.getName().equals(processDto.getName()))) {
            log.error("Duplicate process detected: '{}'", processDto.getName());
            throw new DuplicateProcessException(processDto.getName());
        }
        try {
            final ProcessConfiguration processConfiguration = modelMapper.map(processDto, ProcessConfiguration.class);
            processConfiguration.setProjectConfiguration(projectConfiguration);
            projectConfiguration.getProcesses().add(processConfiguration);
            processConfiguration.setPathToFile(processDto.getName() + ".json");
            processConfiguration.setProcessSettings(
                    modelMapper.map(processDto.getProcessSettings(), ProcessSettings.class));
            processConfiguration.getProcessSettings().setName(processDto.getName());
            syncProcessSections(projectConfiguration, processConfiguration, processDto);
            if (processDto.getInCompounds() != null) {
                log.debug("Processing compound associations for process '{}'", processDto.getName());
                List<CompoundConfiguration> compoundConfigurationList = processDto.getInCompounds().stream()
                        .map(uuid -> projectConfiguration.getCompounds().stream()
                                .filter(c -> c.getId().equals(uuid)).findFirst()
                                .orElseThrow(() -> {
                                    log.error("Compound not found with ID '{}'", uuid);
                                    return new CompoundNotFoundException(uuid);
                                }))
                        .collect(Collectors.toList());
                processConfiguration.setInCompounds(compoundConfigurationList);
                compoundConfigurationList.forEach(s -> {
                    s.getProcesses().add(processConfiguration);
                });
            }
            processConfiguration.setModifiedWhen(new Timestamp(System.currentTimeMillis()));
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        processConfigurationRepository.save(processConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully created process '{}'", processConfiguration);
            return toDto(processConfiguration);
        } catch (Exception e) {
            log.error("Error while creating process '{}': {}", processDto.getName(), e.getMessage(), e);
            throw new CreateProcessException(e);
        }
    }

    /**
     * Delete process.
     *
     * @param projectConfiguration projectConfiguration
     * @param processId            process ID
     * @return list of {@link ProcessDto}
     */
    public List<ProcessShortDto> deleteProcess(ProjectConfiguration projectConfiguration, UUID processId) {
        log.info("Attempting to delete process with ID '{}'", processId);
        ProcessConfiguration processConfiguration = getProcessById(projectConfiguration, processId);
        try {
            projectConfiguration.getAllSections().forEach(s -> s.getProcesses().remove(processConfiguration));
            projectConfiguration.getCompounds().forEach(c -> {
                c.getProcesses().remove(processConfiguration);
            });
            log.debug("Clearing associations for process '{}'", processConfiguration.getName());
            processConfiguration.setInSections(new ArrayList<>());
            processConfiguration.setInCompounds(new ArrayList<>());
            projectConfiguration.getProcesses().remove(processConfiguration);
            log.debug("Executing database delete for process '{}'", processConfiguration.getName());
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        processConfigurationRepository.delete(processConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully deleted process '{}'", processConfiguration);
        } catch (Exception e) {
            log.error("Error while deleting process with ID '{}': {}", processId, e.getMessage(), e);
            throw new DeleteProcessException(e);
        }
        return processesDto(projectConfiguration.getProcesses());
    }

    /**
     * Filter processes.
     *
     * @param processes    processes.
     * @param processesDto list processesDto to filter.
     * @return filtered processes.
     */
    public static LinkedList<ProcessConfiguration> filterProcesses(List<ProcessConfiguration> processes,
                                                                   List<ProcessDto> processesDto) {
        log.trace("Filtering {} processes with {} processesDto", processes.size(), processesDto.size());
        LinkedList<ProcessConfiguration> filteredProcesses = new LinkedList<>();
        processesDto.forEach(processDto -> processes.stream().filter(p -> p.getId().equals(processDto.getId()))
                .findAny().ifPresent(filteredProcesses::add));
        return filteredProcesses;
    }

    /**
     * Filter processes.
     *
     * @param processes     processes.
     * @param processesUuid UUIDs of processes to filter.
     * @return filtered processes.
     */
    public static List<ProcessConfiguration> filterProcess(List<ProcessConfiguration> processes,
                                                           List<UUID> processesUuid) {
        log.trace("Filtering {} processes by {} UUIDs", processes.size(), processesUuid.size());
        List<ProcessConfiguration> filteredProcesses = new ArrayList<>();
        processesUuid.forEach(processUuid -> processes.stream().filter(p -> p.getId().equals(processUuid)).findAny()
                .ifPresent(filteredProcesses::add));
        return filteredProcesses;
    }

    /**
     * Filter process by ID or source ID.
     *
     * @param processes     process to be filter
     * @param processesUuid UUIDs of process to filter
     * @return filtered sections.
     */
    public static List<ProcessConfiguration> filterProcessesByIdOrSourceId(List<ProcessConfiguration> processes,
                                                                           List<UUID> processesUuid) {
        log.trace("Filtering processes by ID or source ID, input size: {}", processesUuid.size());
        List<ProcessConfiguration> filteredProcesses = new LinkedList<>();
        processesUuid.forEach(pUuid -> processes.stream()
                .filter(s -> pUuid != null && (pUuid.equals(s.getId()) || pUuid.equals(s.getSourceId())))
                .findAny()
                .ifPresent(filteredProcesses::add));
        return filteredProcesses;
    }

    /**
     * Get process by ID.
     *
     * @param processes project processes
     */
    public static void checkProcessesById(ProjectConfiguration projectConfiguration,
                                          List<ProcessConfiguration> processes) {
        processes.forEach(process -> {
            projectConfiguration.getProcesses().stream()
                    .filter(proc -> proc.getId().equals(process.getId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Process not found with ID '{}'", process.getId());
                        return new ProcessNotFoundException(process.getId());
                    });
        });
    }

    /**
     * Get process by ID.
     *
     * @param processId project process ID
     * @return {@link SectionConfiguration}
     */
    public ProcessConfiguration getProcessById(ProjectConfiguration projectConfiguration, UUID processId) {
        return projectConfiguration.getProcesses().stream()
                .filter(proc -> proc.getId().equals(processId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Process not found with ID '{}'", processId);
                    return new ProcessNotFoundException(processId);
                });
    }

    /**
     * Map list of ProcessConfiguration to list of ProcessShortDto.
     *
     * @param processConfigurationList list of {@link ProcessConfiguration}
     * @return list of {@link ProcessShortDto}
     */
    public List<ProcessShortDto> processesDto(List<ProcessConfiguration> processConfigurationList) {
        if (processConfigurationList != null) {
            Type listType = new TypeToken<List<ProcessShortDto>>() {
            }.getType();
            return modelMapper.map(processConfigurationList, listType);
        }
        return new ArrayList<>();
    }

    /**
     * Map list of ProcessConfiguration to list of ProcessDto.
     *
     * @param processConfiguration list of {@link ProcessConfiguration}
     * @return list of {@link ProcessDto}
     */
    public ProcessDto toDto(ProcessConfiguration processConfiguration) {
        ProcessDto retValue = modelMapper.map(processConfiguration, ProcessDto.class);
        //model mapper don't support LinkedHashMap map to HashMap and keep correct order
        //in that case set fields with LinkedHashMap manually
        retValue.getProcessSettings().getCommand().setAtpValues(
                processConfiguration.getProcessSettings().getCommand().getAtpValues());
        retValue.getProcessSettings().getCommand().setVariablesToExtractFromLog(
                processConfiguration.getProcessSettings().getCommand().getVariablesToExtractFromLog());
        return retValue;
    }

    /**
     * Update process.
     *
     * @param projectConfiguration projectConfiguration
     * @param processDto           {@link ProcessDto}
     * @return {@link ProcessDto}
     */
    public ProcessDto updateProcess(ProjectConfiguration projectConfiguration, ProcessDto processDto) {
        log.info("Attempting to update process: '{}'", processDto);
        ProcessConfiguration processConfiguration = projectConfiguration.getProcesses().stream()
                .filter(proc -> proc.getId().equals(processDto.getId())).findFirst()
                .orElseThrow(() -> {
                    log.error("Process not found for update with ID '{}'", processDto.getId());
                    return new ProcessNotFoundException(processDto.getId());
                });
        try {
            processConfiguration.setName(processDto.getName());
            if (processConfiguration.getPathToFile() == null || processConfiguration.getPathToFile().isEmpty()) {
                processConfiguration.setPathToFile(processDto.getName() + ".json");
            }
            syncProcessSections(projectConfiguration, processConfiguration, processDto);
            log.debug("Updating process settings for '{}'", processConfiguration.getName());
            processConfiguration.setProcessSettings(
                    modelMapper.map(processDto.getProcessSettings(), ProcessSettings.class));
            processConfiguration.getProcessSettings().setName(processDto.getName());
            processConfiguration.setModifiedWhen(new Timestamp(System.currentTimeMillis()));
            projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                    () -> {
                        processConfigurationRepository.save(processConfiguration);
                        return projectConfiguration;
                    }, false);
            log.info("Successfully updated process '{}'", processConfiguration);
            return toDto(processConfiguration);
        } catch (Exception e) {
            log.error("Error while updating process '{}': '{}'", processConfiguration.getName(), e.getMessage(), e);
            throw new UpdateProcessException(e);
        }
    }

    private void syncProcessSections(ProjectConfiguration projectConfiguration,
                                     ProcessConfiguration processConfiguration,
                                     ProcessDto processDto) {
        if (processDto.getInSections() != null) {
            log.debug("Updating sections for process '{}'", processConfiguration.getName());
            List<SectionConfiguration> newSectionConfigurationList =
                    filterSections(projectConfiguration.getAllSections(), processDto.getInSections());
            List<SectionConfiguration> oldSectionConfigurationList = processConfiguration.getInSections();
            if (!newSectionConfigurationList.equals(oldSectionConfigurationList)) {
                newSectionConfigurationList.forEach(newS -> {
                    if (!oldSectionConfigurationList.contains(newS)) {
                        newS.getProcesses().add(processConfiguration);
                    }
                });
                oldSectionConfigurationList.forEach(oldS -> {
                    if (!newSectionConfigurationList.contains(oldS)) {
                        oldS.getProcesses().remove(processConfiguration);
                    }
                });
                processConfiguration.setInSections(newSectionConfigurationList);
                log.debug("Successfully updated sections for process '{}'", processConfiguration.getName());
            }
        }
    }

    @Override
    public ProcessConfiguration get(UUID id) {
        return processConfigurationRepository.findById(id).orElseThrow(MiaHistoryRevisionRestoreException::new);
    }

    @Override
    public ProcessConfiguration restore(DateAuditorEntity entity) {
        log.info("Restoring process configuration '{}'", entity);
        ProcessConfiguration processConfiguration = (ProcessConfiguration) entity;
        ProjectConfiguration projectConfiguration = processConfiguration.getProjectConfiguration();
        processConfiguration.setModifiedWhen(new Timestamp(System.currentTimeMillis()));
        projectConfigurationService.synchronizeConfiguration(projectConfiguration.getProjectId(),
                () -> {
                    processConfigurationRepository.save(processConfiguration);
                    return projectConfiguration;
                }, false);
        log.info("Successfully restored process configuration '{}'", processConfiguration.getName());
        return processConfiguration;
    }
}
