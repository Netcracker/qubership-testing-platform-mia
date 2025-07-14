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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.exceptions.configuration.CompoundNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.ProcessNotFoundException;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.exception.MiaException;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "project_configuration")
@Slf4j
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProjectConfiguration extends DateAuditorEntity {

    private static final long serialVersionUID = -1488230148100162341L;
    @Id
    @Column(name = "project_id")
    @DiffInclude
    private UUID projectId;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "git_url")
    private String gitUrl;

    @Column(name = "validation_result")
    @DiffInclude
    private String validationResult;

    @Column(name = "primary_migration_done")
    private boolean primaryMigrationDone;

    @Column(name = "last_loaded_when")
    private LocalDateTime lastLoadedWhen;

    @OneToOne(mappedBy = "projectConfiguration", targetEntity = CommonConfiguration.class, cascade = CascadeType.MERGE,
            orphanRemoval = true)
    @DiffInclude
    private CommonConfiguration commonConfiguration;

    @OneToOne(mappedBy = "projectConfiguration", targetEntity = HeaderConfiguration.class, cascade = CascadeType.MERGE,
            orphanRemoval = true)
    @DiffInclude
    private HeaderConfiguration headerConfiguration;

    @OneToOne(mappedBy = "projectConfiguration", targetEntity = PotHeaderConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true)
    @DiffInclude
    private PotHeaderConfiguration potHeaderConfiguration;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = SectionConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<SectionConfiguration> sections;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = ProcessConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ProcessConfiguration> processes;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = CompoundConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<CompoundConfiguration> compounds;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = ProjectDirectory.class,
            cascade = CascadeType.MERGE, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ProjectDirectory> directories;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = ProjectFile.class,
            cascade = CascadeType.MERGE, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<ProjectFile> files;

    /**
     * Gets root sections.
     *
     * @return list of {@link SectionConfiguration}
     */
    public List<ProjectDirectory> getAllDirectories() {
        List<ProjectDirectory> allDirectories = new ArrayList<>();
        getDirectories().stream().forEach(d -> {
            allDirectories.add(d);
            allDirectories.addAll(d.getWithChildrenDirectories());
        });
        return allDirectories;
    }

    /**
     * Gets all sections.
     *
     * @return list of {@link SectionConfiguration}
     */
    public List<SectionConfiguration> getAllSections() {
        List<SectionConfiguration> allSections = new ArrayList<>();
        getSections().stream().forEach(s -> {
            if (s != null) {
                allSections.add(s);
                allSections.addAll(s.getWithChildrenSections());
            }
        });
        return allSections;
    }

    /**
     * Gets common configuration object.
     *
     * @return CommonConfiguration
     */
    public CommonConfiguration getCommonConfiguration() {
        if (commonConfiguration == null) {
            commonConfiguration = new CommonConfiguration();
        }
        return commonConfiguration;
    }

    /**
     * Gets compound by name.
     *
     * @param compoundName compound name
     * @return Compound
     * @throws MiaException in case compound not found
     */
    public CompoundConfiguration getCompoundByName(String compoundName) {
        return getCompoundByNameSafe(compoundName)
                .orElseThrow(() -> new CompoundNotFoundException(compoundName));
    }

    /**
     * Gets compound by name.
     *
     * @param compoundName compound name
     * @return Compound
     */
    public Optional<CompoundConfiguration> getCompoundByNameSafe(String compoundName) {
        return compounds.stream().filter(p -> p.getName().equals(compoundName)).findAny();
    }

    /**
     * Getter for compounds.
     *
     * @return compounds
     */
    public List<CompoundConfiguration> getCompounds() {
        if (compounds == null) {
            compounds = new ArrayList<>();
        }
        return compounds;
    }

    /**
     * Getter for directories.
     *
     * @return directories
     */
    public List<ProjectDirectory> getDirectories() {
        if (directories == null) {
            directories = new ArrayList<>();
        }
        return directories;
    }

    /**
     * Getter for files.
     *
     * @return files
     */
    public List<ProjectFile> getFiles() {
        if (files == null) {
            files = new ArrayList<>();
        }
        return files;
    }

    /**
     * Return PotHeaderConfiguration.
     *
     * @return potHeaderConfiguration pot Header Configuration was returned
     */
    public HeaderConfiguration getHeaderConfiguration() {
        if (headerConfiguration == null) {
            headerConfiguration = new HeaderConfiguration();
        }
        return headerConfiguration;
    }

    /**
     * Return PotHeaderConfiguration.
     *
     * @return potHeaderConfiguration pot Header Configuration was returned
     */
    public PotHeaderConfiguration getPotHeaderConfiguration() {
        if (potHeaderConfiguration == null) {
            potHeaderConfiguration = new PotHeaderConfiguration();
        }
        return potHeaderConfiguration;
    }

    /**
     * Gets process by name.
     *
     * @param processName process name
     * @return Process
     * @throws MiaException in case process not found
     */
    public ProcessConfiguration getProcessByName(String processName) {
        return getProcessByNameSafe(processName)
                .orElseThrow(() -> new ProcessNotFoundException(processName));
    }

    /**
     * Gets process by name.
     *
     * @param processName process name
     * @return Process
     */
    public Optional<ProcessConfiguration> getProcessByNameSafe(String processName) {
        return processes.stream().filter(p -> p.getName().equals(processName)).findAny();
    }

    /**
     * Getter for processes.
     *
     * @return processes
     */
    public List<ProcessConfiguration> getProcesses() {
        if (processes == null) {
            processes = new ArrayList<>();
        }
        return processes;
    }

    /**
     * Gets root directories.
     *
     * @return {@link ProjectDirectory}
     */
    public List<ProjectDirectory> getRootDirectories() {
        if (directories == null) {
            return new ArrayList<>();
        }
        return directories.stream().filter(d -> d.getParentDirectory() == null).collect(Collectors.toList());
    }

    /**
     * Gets root files.
     *
     * @return {@link ProjectDirectory}
     */
    public List<ProjectFile> getRootFiles() {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream().filter(f -> f.getDirectory() == null).collect(Collectors.toList());
    }

    /**
     * Gets root sections.
     *
     * @return list of {@link SectionConfiguration}
     */
    public LinkedList<SectionConfiguration> getRootSections() {
        LinkedList<SectionConfiguration> sectionAsRoot = new LinkedList<>();
        if (sections != null) {
            sections.stream().sorted(Comparator.comparing(SectionConfiguration::getPlace)).forEach(section -> {
                if (section.getParentSection() == null) {
                    sectionAsRoot.add(correctPlaceInList(sectionAsRoot, section.getPlace()), section);
                }
            });
        }
        return sectionAsRoot;
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
     * Remove section.
     *
     * @param section {@link SectionConfiguration}
     */
    public void removeSection(SectionConfiguration section) {
        if (sections != null) {
            sections.remove(section);
        }
    }
}
