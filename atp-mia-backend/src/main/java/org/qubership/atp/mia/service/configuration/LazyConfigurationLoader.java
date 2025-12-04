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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.CacheKeys;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ConfigurationReference;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.repo.configuration.CompoundConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.ProcessConfigurationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for lazy loading of processes and compounds.
 * Uses caching to minimize database queries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LazyConfigurationLoader {
    
    private final ProcessConfigurationRepository processRepository;
    private final CompoundConfigurationRepository compoundRepository;
    
    /**
     * Load all processes for a project with caching
     * 
     * @param projectId project ID
     * @return list of processes
     */
    @Cacheable(value = CacheKeys.Constants.PROCESSES_LIST_KEY, key = "#projectId", 
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<ProcessConfiguration> loadProcesses(UUID projectId) {
        log.debug("Loading processes for project {} from database", projectId);
        return processRepository.findByProjectId(projectId);
    }
    
    /**
     * Load all compounds for a project with caching
     * 
     * @param projectId project ID
     * @return list of compounds
     */
    @Cacheable(value = CacheKeys.Constants.COMPOUNDS_LIST_KEY, key = "#projectId",
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<CompoundConfiguration> loadCompounds(UUID projectId) {
        log.debug("Loading compounds for project {} from database", projectId);
        return compoundRepository.findByProjectId(projectId);
    }
    
    /**
     * Load process by name with caching
     * 
     * @param projectId project ID
     * @param processName process name
     * @return Optional with process
     */
    @Cacheable(value = CacheKeys.Constants.PROCESS_BY_NAME_KEY, 
               key = "#projectId + '_' + #processName",
               condition = "#projectId != null && #processName != null")
    @Transactional(readOnly = true)
    public Optional<ProcessConfiguration> loadProcessByName(UUID projectId, String processName) {
        log.debug("Loading process {} for project {} from database", processName, projectId);
        return processRepository.findByProjectIdAndName(projectId, processName);
    }
    
    /**
     * Load compound by name with caching
     * 
     * @param projectId project ID
     * @param compoundName compound name
     * @return Optional with compound
     */
    @Cacheable(value = CacheKeys.Constants.COMPOUND_BY_NAME_KEY,
               key = "#projectId + '_' + #compoundName",
               condition = "#projectId != null && #compoundName != null")
    @Transactional(readOnly = true)
    public Optional<CompoundConfiguration> loadCompoundByName(UUID projectId, String compoundName) {
        log.debug("Loading compound {} for project {} from database", compoundName, projectId);
        return compoundRepository.findByProjectIdAndName(projectId, compoundName);
    }
    
    /**
     * Load process by ID with caching
     * 
     * @param projectId project ID
     * @param processId process ID
     * @return Optional with process
     */
    @Cacheable(value = CacheKeys.Constants.PROCESS_BY_ID_KEY,
               key = "#projectId + '_' + #processId",
               condition = "#projectId != null && #processId != null")
    @Transactional(readOnly = true)
    public Optional<ProcessConfiguration> loadProcessById(UUID projectId, UUID processId) {
        log.debug("Loading process with ID {} for project {} from database", processId, projectId);
        return processRepository.findByProjectIdAndId(projectId, processId);
    }
    
    /**
     * Load compound by ID with caching
     * 
     * @param projectId project ID
     * @param compoundId compound ID
     * @return Optional with compound
     */
    @Cacheable(value = CacheKeys.Constants.COMPOUND_BY_ID_KEY,
               key = "#projectId + '_' + #compoundId",
               condition = "#projectId != null && #compoundId != null")
    @Transactional(readOnly = true)
    public Optional<CompoundConfiguration> loadCompoundById(UUID projectId, UUID compoundId) {
        log.debug("Loading compound with ID {} for project {} from database", compoundId, projectId);
        return compoundRepository.findByProjectIdAndId(projectId, compoundId);
    }
    
    /**
     * Get only process IDs (lightweight query, without full entity loading)
     * 
     * @param projectId project ID
     * @return list of IDs
     */
    @Cacheable(value = CacheKeys.Constants.PROCESS_IDS_KEY, key = "#projectId",
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<UUID> loadProcessIds(UUID projectId) {
        log.debug("Loading process IDs for project {} from database", projectId);
        return processRepository.findIdsByProjectId(projectId);
    }
    
    /**
     * Get only compound IDs (lightweight query, without full entity loading)
     * 
     * @param projectId project ID
     * @return list of IDs
     */
    @Cacheable(value = CacheKeys.Constants.COMPOUND_IDS_KEY, key = "#projectId",
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<UUID> loadCompoundIds(UUID projectId) {
        log.debug("Loading compound IDs for project {} from database", projectId);
        return compoundRepository.findIdsByProjectId(projectId);
    }
    
    /**
     * Load process references (ID + name only) for a project.
     * Lightweight version that doesn't load full entities.
     * 
     * @param projectId project ID
     * @return list of configuration references
     */
    @Transactional(readOnly = true)
    public List<ConfigurationReference> loadProcessRefs(UUID projectId) {
        log.debug("Loading process refs for project {} from database", projectId);
        return processRepository.findIdAndNameByProjectId(projectId).stream()
                .map(arr -> new ConfigurationReference((UUID) arr[0], (String) arr[1]))
                .collect(Collectors.toList());
    }
    
    /**
     * Load compound references (ID + name only) for a project.
     * Lightweight version that doesn't load full entities.
     * 
     * @param projectId project ID
     * @return list of configuration references
     */
    @Transactional(readOnly = true)
    public List<ConfigurationReference> loadCompoundRefs(UUID projectId) {
        log.debug("Loading compound refs for project {} from database", projectId);
        return compoundRepository.findIdAndNameByProjectId(projectId).stream()
                .map(arr -> new ConfigurationReference((UUID) arr[0], (String) arr[1]))
                .collect(Collectors.toList());
    }
    
    /**
     * Load process references (ID + name only) for a section.
     * Lightweight version that doesn't load full entities.
     * 
     * @param sectionId section ID
     * @return list of configuration references
     */
    @Transactional(readOnly = true)
    public List<ConfigurationReference> loadSectionProcessRefs(UUID sectionId) {
        log.debug("Loading process refs for section {} from database", sectionId);
        return processRepository.findIdAndNameBySectionId(sectionId).stream()
                .map(arr -> new ConfigurationReference((UUID) arr[0], (String) arr[1]))
                .collect(Collectors.toList());
    }
    
    /**
     * Load compound references (ID + name only) for a section.
     * Lightweight version that doesn't load full entities.
     * 
     * @param sectionId section ID
     * @return list of configuration references
     */
    @Transactional(readOnly = true)
    public List<ConfigurationReference> loadSectionCompoundRefs(UUID sectionId) {
        log.debug("Loading compound refs for section {} from database", sectionId);
        return compoundRepository.findIdAndNameBySectionId(sectionId).stream()
                .map(arr -> new ConfigurationReference((UUID) arr[0], (String) arr[1]))
                .collect(Collectors.toList());
    }
}


