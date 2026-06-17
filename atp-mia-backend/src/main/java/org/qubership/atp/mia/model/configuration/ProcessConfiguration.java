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

package org.qubership.atp.mia.model.configuration;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.Type;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.converters.ListConverter;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_processes_configuration")
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProcessConfiguration extends DateAuditorEntity {

    @Serial
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
    @Type(JsonType.class)
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private ProcessSettings processSettings;

    @ManyToMany(mappedBy = "processes", targetEntity = CompoundConfiguration.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    @DiffIgnore
    private List<CompoundConfiguration> inCompounds = new ArrayList<>();

    /**
     * For History change usage. DO NOT use in restore!
     * is using in
     */
    @Column(name = "in_compounds", columnDefinition = "TEXT")
    @Builder.Default
    @Convert(converter = ListConverter.class)
    @DiffInclude
    private List<String> compounds = new ArrayList<>();

    @ManyToMany(mappedBy = "processes", targetEntity = SectionConfiguration.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    @DiffIgnore
    private List<SectionConfiguration> inSections = new ArrayList<>();

    /**
     * For History change usage. DO NOT use in restore!
     * is using in
     */
    @Column(name = "in_sections", columnDefinition = "TEXT")
    @DiffInclude
    @Builder.Default
    @Convert(converter = ListConverter.class)
    private List<String> sections = new ArrayList<>();

    @ManyToOne(targetEntity = ProjectConfiguration.class)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    /**
     * Set all processes.
     */
    @JsonSetter
    public void setInCompounds(final List<CompoundConfiguration> compounds) {
        this.inCompounds = compounds;
        this.compounds = compounds != null ? compounds.stream().map(CompoundConfiguration::getName)
                .collect(Collectors.toCollection(LinkedList::new)) : null;
    }

    /**
     * Set all processes.
     */
    @JsonSetter
    public void setInSections(final List<SectionConfiguration> sections) {
        this.inSections = sections;
        this.sections = sections != null ? sections.stream().map(SectionConfiguration::getName)
                .collect(Collectors.toCollection(LinkedList::new)) : null;
    }
}
