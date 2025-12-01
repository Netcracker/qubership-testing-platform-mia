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

import java.util.Optional;
import java.util.UUID;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@JaversSpringDataAuditable
public interface ProjectConfigurationRepository extends CrudRepository<ProjectConfiguration, UUID> {

    void deleteByProjectId(UUID projectId);

    @EntityGraph(attributePaths = {
            "commonConfiguration",
            "commonConfiguration.commandShellPrefixes",
            "headerConfiguration",
            "potHeaderConfiguration"
    })
    Optional<ProjectConfiguration> findDetailedByProjectId(UUID projectId);

    @EntityGraph(attributePaths = {
            "commonConfiguration",
            "commonConfiguration.commandShellPrefixes",
            "headerConfiguration",
            "potHeaderConfiguration"
    })
    Optional<ProjectConfiguration> findGeneralByProjectId(UUID projectId);

    @Modifying
    @Query(value = "SET session_replication_role = 'replica'", nativeQuery = true)
    void setReplicationRoleReplica();

    @Modifying
    @Query(value = "SET session_replication_role = 'origin'", nativeQuery = true)
    void setReplicationRoleOrigin();
}
