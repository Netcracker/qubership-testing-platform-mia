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
     * Find all compounds for a project.
     *
     * @param projectId project ID
     * @return list of compounds
     */
    @Query("SELECT c FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId")
    List<CompoundConfiguration> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find compound by name for a project.
     *
     * @param projectId project ID
     * @param name compound name
     * @return Optional with compound
     */
    @Query("SELECT c FROM CompoundConfiguration c "
            + "WHERE c.projectConfiguration.projectId = :projectId AND c.name = :name")
    Optional<CompoundConfiguration> findByProjectIdAndName(
            @Param("projectId") UUID projectId, 
            @Param("name") String name);

    /**
     * Find compound by ID for a project (with ownership check).
     *
     * @param projectId project ID
     * @param compoundId compound ID
     * @return Optional with compound
     */
    @Query("SELECT c FROM CompoundConfiguration c "
            + "WHERE c.projectConfiguration.projectId = :projectId AND c.id = :compoundId")
    Optional<CompoundConfiguration> findByProjectIdAndId(
            @Param("projectId") UUID projectId, 
            @Param("compoundId") UUID compoundId);

    /**
     * Get list of all compound IDs for a project (lightweight query).
     *
     * @param projectId project ID
     * @return list of IDs
     */
    @Query("SELECT c.id FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Get list of all compound names for a project (lightweight query).
     *
     * @param projectId project ID
     * @return list of names
     */
    @Query("SELECT c.name FROM CompoundConfiguration c WHERE c.projectConfiguration.projectId = :projectId")
    List<String> findNamesByProjectId(@Param("projectId") UUID projectId);

    /**
     * Get list of compound ID and name pairs for a project (lightweight query for refs).
     * Returns Object[] where [0] = UUID id, [1] = String name.
     *
     * @param projectId project ID
     * @return list of Object arrays with ID and name
     */
    @Query("SELECT c.id, c.name FROM CompoundConfiguration c "
            + "WHERE c.projectConfiguration.projectId = :projectId")
    List<Object[]> findIdAndNameByProjectId(@Param("projectId") UUID projectId);

    /**
     * Get list of compound ID and name pairs for a section (lightweight query for refs).
     * Queries the join table to find compounds linked to a section.
     * Returns Object[] where [0] = UUID id, [1] = String name.
     *
     * @param sectionId section ID
     * @return list of Object arrays with ID and name
     */
    @Query(value = "SELECT c.id, c.name FROM compound_configuration c "
            + "INNER JOIN project_section_compound_configuration sc ON c.id = sc.compound_id "
            + "WHERE sc.section_id = :sectionId ORDER BY sc.place",
           nativeQuery = true)
    List<Object[]> findIdAndNameBySectionId(@Param("sectionId") UUID sectionId);
}
