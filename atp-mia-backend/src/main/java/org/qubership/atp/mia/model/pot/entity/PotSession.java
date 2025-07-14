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

package org.qubership.atp.mia.model.pot.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "pot_session")
public class PotSession {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToMany(mappedBy = "potSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @EqualsAndHashCode.Exclude
    private final List<PotExecutionStep> potExecutionSteps = new ArrayList<>();

    @Column(name = "created_at")
    @EqualsAndHashCode.Exclude
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "created_by")
    @EqualsAndHashCode.Exclude
    private String createdBy;
    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    /**
     * Creates {@code RecordingSession} instance.
     *
     * @param id                   id
     * @param projectConfiguration projectConfiguration
     * @param user                 user
     */
    public PotSession(UUID id, ProjectConfiguration projectConfiguration, String user) {
        this.id = id;
        this.projectId = projectConfiguration.getProjectId();
        this.createdBy = user;
    }

    /**
     * Adds execution step to Recording session.
     * It's executed for every process/compound after a user clicked 'Start POT`
     *
     * @return Recording session where saved the step.
     */
    public PotSession addExecutionStep(PotExecutionStep potExecutionStep) {
        potExecutionSteps.add(potExecutionStep);
        potExecutionStep.setPotSession(this);
        return this;
    }
}
