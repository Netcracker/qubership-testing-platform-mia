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

package org.qubership.atp.mia.repo.configuration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface ProcessConfigurationRepository extends CrudRepository<ProcessConfiguration, UUID> {

    /**
     * Найти все процессы для проекта
     * 
     * @param projectId ID проекта
     * @return список процессов
     */
    @Query("SELECT p FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId")
    List<ProcessConfiguration> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Найти процесс по имени для проекта
     * 
     * @param projectId ID проекта
     * @param name имя процесса
     * @return Optional с процессом
     */
    @Query("SELECT p FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId AND p.name = :name")
    Optional<ProcessConfiguration> findByProjectIdAndName(@Param("projectId") UUID projectId, @Param("name") String name);

    /**
     * Найти процесс по ID для проекта (с проверкой принадлежности)
     * 
     * @param projectId ID проекта
     * @param processId ID процесса
     * @return Optional с процессом
     */
    @Query("SELECT p FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId AND p.id = :processId")
    Optional<ProcessConfiguration> findByProjectIdAndId(@Param("projectId") UUID projectId, @Param("processId") UUID processId);

    /**
     * Получить список ID всех процессов проекта (легковесный запрос)
     * 
     * @param projectId ID проекта
     * @return список ID
     */
    @Query("SELECT p.id FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Получить список имен всех процессов проекта (легковесный запрос)
     * 
     * @param projectId ID проекта
     * @return список имен
     */
    @Query("SELECT p.name FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId")
    List<String> findNamesByProjectId(@Param("projectId") UUID projectId);
}
