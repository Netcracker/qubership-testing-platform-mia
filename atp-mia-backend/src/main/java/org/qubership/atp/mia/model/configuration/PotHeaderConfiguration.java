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

package org.qubership.atp.mia.model.configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.mia.model.impl.executable.PotHeader;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "project_pot_header_configuration")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Value
public class PotHeaderConfiguration implements Serializable {

    private static final long serialVersionUID = -4051242360768974857L;
    @Id
    @Column(name = "project_id")
    private UUID projectId;
    @Column(name = "headers", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    @DiffInclude
    private List<PotHeader> headers = new ArrayList<>();
    @OneToOne(targetEntity = ProjectConfiguration.class, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;
}
