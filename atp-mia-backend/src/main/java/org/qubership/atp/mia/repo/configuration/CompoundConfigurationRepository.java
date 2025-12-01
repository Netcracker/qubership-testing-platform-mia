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
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface CompoundConfigurationRepository extends CrudRepository<CompoundConfiguration, UUID> {

    /**
     * Найти все компаунды для проекта
     * 
     * @param projectId ID проекта
     * @return список компаундов
     */
    @Query("SELECT c FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId")
    List<CompoundConfiguration> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Найти компаунд по имени для проекта
     * 
     * @param projectId ID проекта
     * @param name имя компаунда
     * @return Optional с компаундом
     */
    @Query("SELECT c FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId AND c.name = :name")
    Optional<CompoundConfiguration> findByProjectIdAndName(@Param("projectId") UUID projectId, @Param("name") String name);

    /**
     * Найти компаунд по ID для проекта (с проверкой принадлежности)
     * 
     * @param projectId ID проекта
     * @param compoundId ID компаунда
     * @return Optional с компаундом
     */
    @Query("SELECT c FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId AND c.id = :compoundId")
    Optional<CompoundConfiguration> findByProjectIdAndId(@Param("projectId") UUID projectId, @Param("compoundId") UUID compoundId);

    /**
     * Получить список ID всех компаундов проекта (легковесный запрос)
     * 
     * @param projectId ID проекта
     * @return список ID
     */
    @Query("SELECT c.id FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Получить список имен всех компаундов проекта (легковесный запрос)
     * 
     * @param projectId ID проекта
     * @return список имен
     */
    @Query("SELECT c.name FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId")
    List<String> findNamesByProjectId(@Param("projectId") UUID projectId);
}
