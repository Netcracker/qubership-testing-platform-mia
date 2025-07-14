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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_processes_configuration")
@TypeDef(name = "json", typeClass = JsonType.class)
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcessConfiguration extends DateAuditorEntity {

    private static final long serialVersionUID = -8870451957638368825L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @DiffInclude
    private UUID id;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "process_name")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String name;

    @Column(name = "file_name")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String pathToFile;

    @Column(name = "process_settings", columnDefinition = "json")
    @Type(type = "json")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private ProcessSettings processSettings;

    @ManyToMany(mappedBy = "processes", targetEntity = CompoundConfiguration.class, cascade = CascadeType.MERGE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<CompoundConfiguration> inCompounds = new ArrayList<>();

    @ManyToMany(mappedBy = "processes", targetEntity = SectionConfiguration.class, cascade = CascadeType.MERGE)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<SectionConfiguration> inSections = new ArrayList<>();

    @ManyToOne(targetEntity = ProjectConfiguration.class)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;
}
