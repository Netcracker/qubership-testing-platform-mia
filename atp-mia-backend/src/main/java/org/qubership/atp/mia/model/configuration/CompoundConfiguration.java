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
import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.model.DateAuditorEntity;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_compounds_configuration")
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class
CompoundConfiguration extends DateAuditorEntity {

    @Serial
    private static final long serialVersionUID = -4706896104344439606L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    @DiffInclude
    private UUID id;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "compound_name")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String name;
    @Column(name = "refer_to_input")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String referToInput;

    @ManyToMany(targetEntity = ProcessConfiguration.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "project_compound_process_configuration",
            joinColumns = @JoinColumn(name = "compound_id"),
            inverseJoinColumns = {@JoinColumn(name = "process_id")})
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OrderColumn(name = "place")
    @DiffInclude
    private List<ProcessConfiguration> processes = new ArrayList<>();

    @ManyToMany(mappedBy = "compounds", targetEntity = SectionConfiguration.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffInclude
    private List<SectionConfiguration> inSections = new ArrayList<>();

    @ManyToOne(targetEntity = ProjectConfiguration.class)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    /**
     * Add process.
     *
     * @param processConfiguration process
     */
    public void addProcess(ProcessConfiguration processConfiguration) {
        if (processes == null) {
            processes = new ArrayList<>();
        }
        processes.add(processConfiguration);
    }

    /**
     * Set all processes.
     *
     * @param processes list of processes
     */
    @JsonSetter
    public void setProcesses(final List<ProcessConfiguration> processes) {
        this.processes = processes;
    }
}
