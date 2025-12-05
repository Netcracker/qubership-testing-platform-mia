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
     * Find all processes for a project.
     *
     * @param projectId project ID
     * @return list of processes
     */
    @Query("SELECT p FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId")
    List<ProcessConfiguration> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find process by name for a project.
     *
     * @param projectId project ID
     * @param name process name
     * @return Optional with process
     */
    @Query("SELECT p FROM ProcessConfiguration p "
            + "WHERE p.projectConfiguration.projectId = :projectId AND p.name = :name")
    Optional<ProcessConfiguration> findByProjectIdAndName(
            @Param("projectId") UUID projectId, 
            @Param("name") String name);

    /**
     * Find process by ID for a project (with ownership check).
     *
     * @param projectId project ID
     * @param processId process ID
     * @return Optional with process
     */
    @Query("SELECT p FROM ProcessConfiguration p "
            + "WHERE p.projectConfiguration.projectId = :projectId AND p.id = :processId")
    Optional<ProcessConfiguration> findByProjectIdAndId(
            @Param("projectId") UUID projectId, 
            @Param("processId") UUID processId);

    /**
     * Get list of all process IDs for a project (lightweight query).
     *
     * @param projectId project ID
     * @return list of IDs
     */
    @Query("SELECT p.id FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Get list of all process names for a project (lightweight query).
     *
     * @param projectId project ID
     * @return list of names
     */
    @Query("SELECT p.name FROM ProcessConfiguration p WHERE p.projectConfiguration.projectId = :projectId")
    List<String> findNamesByProjectId(@Param("projectId") UUID projectId);

    /**
     * Get list of process ID and name pairs for a project (lightweight query for refs).
     * Returns Object[] where [0] = UUID id, [1] = String name.
     *
     * @param projectId project ID
     * @return list of Object arrays with ID and name
     */
    @Query("SELECT p.id, p.name FROM ProcessConfiguration p "
            + "WHERE p.projectConfiguration.projectId = :projectId")
    List<Object[]> findIdAndNameByProjectId(@Param("projectId") UUID projectId);

    /**
     * Get list of process ID and name pairs for a section (lightweight query for refs).
     * Queries the join table to find processes linked to a section.
     * Returns Object[] where [0] = UUID id, [1] = String name.
     *
     * @param sectionId section ID
     * @return list of Object arrays with ID and name
     */
    @Query(value = "SELECT p.id, p.name FROM process_configuration p "
            + "INNER JOIN project_section_process_configuration sp ON p.id = sp.process_id "
            + "WHERE sp.section_id = :sectionId ORDER BY sp.place", 
           nativeQuery = true)
    List<Object[]> findIdAndNameBySectionId(@Param("sectionId") UUID sectionId);
}
