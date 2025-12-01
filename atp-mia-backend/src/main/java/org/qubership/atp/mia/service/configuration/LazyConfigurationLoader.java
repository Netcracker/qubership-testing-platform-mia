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

import org.qubership.atp.mia.model.CacheKeys;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.repo.configuration.CompoundConfigurationRepository;
import org.qubership.atp.mia.repo.configuration.ProcessConfigurationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для ленивой загрузки процессов и компаундов.
 * Использует кэширование для минимизации обращений к БД.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LazyConfigurationLoader {
    
    private final ProcessConfigurationRepository processRepository;
    private final CompoundConfigurationRepository compoundRepository;
    
    /**
     * Загрузить все процессы для проекта с кэшированием
     * 
     * @param projectId ID проекта
     * @return список процессов
     */
    @Cacheable(value = CacheKeys.Constants.PROCESSES_LIST_KEY, key = "#projectId", 
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<ProcessConfiguration> loadProcesses(UUID projectId) {
        log.debug("Loading processes for project {} from database", projectId);
        return processRepository.findByProjectId(projectId);
    }
    
    /**
     * Загрузить все компаунды для проекта с кэшированием
     * 
     * @param projectId ID проекта
     * @return список компаундов
     */
    @Cacheable(value = CacheKeys.Constants.COMPOUNDS_LIST_KEY, key = "#projectId",
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<CompoundConfiguration> loadCompounds(UUID projectId) {
        log.debug("Loading compounds for project {} from database", projectId);
        return compoundRepository.findByProjectId(projectId);
    }
    
    /**
     * Загрузить процесс по имени с кэшированием
     * 
     * @param projectId ID проекта
     * @param processName имя процесса
     * @return Optional с процессом
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
     * Загрузить компаунд по имени с кэшированием
     * 
     * @param projectId ID проекта
     * @param compoundName имя компаунда
     * @return Optional с компаундом
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
     * Загрузить процесс по ID с кэшированием
     * 
     * @param projectId ID проекта
     * @param processId ID процесса
     * @return Optional с процессом
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
     * Загрузить компаунд по ID с кэшированием
     * 
     * @param projectId ID проекта
     * @param compoundId ID компаунда
     * @return Optional с компаундом
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
     * Получить только ID процессов (легковесный запрос, без полной загрузки entity)
     * 
     * @param projectId ID проекта
     * @return список ID
     */
    @Cacheable(value = CacheKeys.Constants.PROCESS_IDS_KEY, key = "#projectId",
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<UUID> loadProcessIds(UUID projectId) {
        log.debug("Loading process IDs for project {} from database", projectId);
        return processRepository.findIdsByProjectId(projectId);
    }
    
    /**
     * Получить только ID компаундов (легковесный запрос, без полной загрузки entity)
     * 
     * @param projectId ID проекта
     * @return список ID
     */
    @Cacheable(value = CacheKeys.Constants.COMPOUND_IDS_KEY, key = "#projectId",
               condition = "#projectId != null")
    @Transactional(readOnly = true)
    public List<UUID> loadCompoundIds(UUID projectId) {
        log.debug("Loading compound IDs for project {} from database", projectId);
        return compoundRepository.findIdsByProjectId(projectId);
    }
}


