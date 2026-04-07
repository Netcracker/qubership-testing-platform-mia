/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.PotSessionException;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.db.SqlResponse;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "pot_execution_step")
public class PotExecutionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @EqualsAndHashCode.Exclude
    private String stepName;

    @Column(name = "environment_name")
    @EqualsAndHashCode.Exclude
    private String environmentName;

    @EqualsAndHashCode.Exclude
    private String executedCommand;

    @Column(name = "process_status", columnDefinition = "json")
    @Type(JsonType.class)
    @EqualsAndHashCode.Exclude
    private ProcessStatus processStatus;

    @Column(name = "links", columnDefinition = "json")
    @Type(JsonType.class)
    @EqualsAndHashCode.Exclude
    private List<Link> links;

    @Column(name = "validations", columnDefinition = "json")
    @Type(JsonType.class)
    @EqualsAndHashCode.Exclude
    private List<SqlResponse> validations;

    @Column(name = "errors", columnDefinition = "json")
    @Type(JsonType.class)
    @EqualsAndHashCode.Exclude
    private List<PotSessionException> errors;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pot_session_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private PotSession potSession;

    public void setErrors(List<Exception> errors) {
        this.errors = CollectionUtils.emptyIfNull(errors).stream()
                .map(PotSessionException::new)
                .collect(Collectors.toList());
    }

    public PotExecutionStep setPotSession(PotSession recordingSession) {
        this.potSession = recordingSession;
        return this;
    }
}
