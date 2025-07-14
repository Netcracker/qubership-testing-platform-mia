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

import static org.qubership.atp.mia.utils.Utils.correctPlaceInList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PostRemove;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.model.DateAuditorEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_section_configuration")
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SectionConfiguration extends DateAuditorEntity {

    private static final long serialVersionUID = -640501254431769898L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    @DiffInclude
    protected UUID id;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "section_name", nullable = false)
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String name;

    @Column(name = "place", nullable = false)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @DiffInclude
    private int place = 0;

    @ManyToOne(targetEntity = SectionConfiguration.class, cascade = CascadeType.MERGE)
    @JoinColumn(name = "parent_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffInclude
    private SectionConfiguration parentSection;

    @OneToMany(mappedBy = "parentSection", cascade = CascadeType.ALL)
    @OrderColumn(name = "place", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @EqualsAndHashCode.Exclude
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<SectionConfiguration> sections;

    @ManyToMany(targetEntity = CompoundConfiguration.class, cascade = CascadeType.MERGE)
    @JoinTable(name = "project_section_compound_configuration",
            joinColumns = @JoinColumn(name = "section_id"),
            inverseJoinColumns = {@JoinColumn(name = "compound_id")})
    @OrderColumn(name = "place")
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<CompoundConfiguration> compounds = new ArrayList<>();

    @ManyToMany(targetEntity = ProcessConfiguration.class, cascade = CascadeType.MERGE)
    @JoinTable(name = "project_section_process_configuration",
            joinColumns = @JoinColumn(name = "section_id"),
            inverseJoinColumns = {@JoinColumn(name = "process_id")})
    @OrderColumn(name = "place")
    @Fetch(FetchMode.SUBSELECT)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<ProcessConfiguration> processes = new ArrayList<>();

    @ManyToOne(targetEntity = ProjectConfiguration.class)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    /**
     * Add compound.
     *
     * @param compoundConfiguration compound
     */
    public void addCompound(CompoundConfiguration compoundConfiguration) {
        if (compounds == null) {
            compounds = new ArrayList<>();
        }
        if (compounds.stream().noneMatch(c -> c.getId() != null && c.getId().equals(compoundConfiguration.getId()))) {
            compounds.add(compoundConfiguration);
        }
    }

    /**
     * Add process.
     *
     * @param processConfiguration process
     */
    public void addProcess(ProcessConfiguration processConfiguration) {
        if (processes == null) {
            processes = new ArrayList<>();
        }
        if (processes.stream().noneMatch(p -> p.getId() != null && p.getId().equals(processConfiguration.getId()))) {
            processes.add(processConfiguration);
        }
    }

    /**
     * Add section.
     *
     * @param sectionConfiguration section
     */
    public void addSection(SectionConfiguration sectionConfiguration) {
        if (sections == null) {
            sections = new ArrayList<>();
        }
        if (sections.stream().noneMatch(s -> s.getId() != null && s.getId().equals(sectionConfiguration.getId()))) {
            sections.add(sectionConfiguration);
        }
    }

    /**
     * Get UUID for all nested children.
     *
     * @return list of {@link UUID}
     */
    public Set<UUID> getChildrenUuid() {
        Set<UUID> childsUuid = new HashSet<>();
        getSections().forEach(s -> {
            if (s != null) {
                childsUuid.add(s.getId());
                childsUuid.addAll(s.getChildrenUuid());
            }
        });
        return childsUuid;
    }

    /**
     * Getter for sections.
     *
     * @return list of {@link SectionConfiguration}
     */
    public List<SectionConfiguration> getSections() {
        if (sections == null) {
            sections = new ArrayList<>();
        }
        return sections;
    }

    /**
     * Get section with all nested children.
     *
     * @return list of {@link SectionConfiguration}
     */
    public List<SectionConfiguration> getWithChildrenSections() {
        List<SectionConfiguration> sections = new ArrayList<>();
        getSections().forEach(s -> {
            if (s != null) {
                sections.add(s);
                sections.addAll(s.getWithChildrenSections());
            }
        });
        return sections;
    }

    /**
     * Reorders child sections (if present) sequentially starting from position 0.
     */
    @PrePersist
    @PreUpdate
    @PostRemove
    public void reorderChildSequentially() {
        if (this.sections != null) {
            List<SectionConfiguration> orderedSections = new ArrayList<>();
            this.sections.stream()
                    .filter(Objects::nonNull)
                    .forEach(s -> orderedSections.add(correctPlaceInList(orderedSections, s.place), s));
            IntStream.range(0, orderedSections.size()).forEach(place -> orderedSections.get(place).setPlace(place));
        }
    }
}
