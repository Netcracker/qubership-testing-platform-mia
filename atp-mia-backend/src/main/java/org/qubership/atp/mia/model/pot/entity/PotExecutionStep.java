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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.PotSessionException;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.db.SqlResponse;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "pot_execution_step")
@TypeDef(name = "json", typeClass = JsonType.class)
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
    @Type(type = "json")
    @EqualsAndHashCode.Exclude
    private ProcessStatus processStatus;

    @Column(name = "links", columnDefinition = "json")
    @Type(type = "json")
    @EqualsAndHashCode.Exclude
    private List<Link> links;

    @Column(name = "validations", columnDefinition = "json")
    @Type(type = "json")
    @EqualsAndHashCode.Exclude
    private List<SqlResponse> validations;

    @Column(name = "errors", columnDefinition = "json")
    @Type(type = "json")
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
