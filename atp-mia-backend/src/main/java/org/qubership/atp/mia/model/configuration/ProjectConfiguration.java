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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.exceptions.configuration.CompoundNotFoundException;
import org.qubership.atp.mia.exceptions.configuration.ProcessNotFoundException;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.exception.MiaException;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.service.configuration.LazyConfigurationLoader;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
            orphanRemoval = true, fetch = FetchType.LAZY)
    @DiffInclude
    private CommonConfiguration commonConfiguration;

    @OneToOne(mappedBy = "projectConfiguration", targetEntity = HeaderConfiguration.class, cascade = CascadeType.MERGE,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @DiffInclude
    private HeaderConfiguration headerConfiguration;

    @OneToOne(mappedBy = "projectConfiguration", targetEntity = PotHeaderConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.LAZY)
    @DiffInclude
    private PotHeaderConfiguration potHeaderConfiguration;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = SectionConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 50)
    private List<SectionConfiguration> sections;

    /**
     * Lightweight references to processes (ID + name only).
     * Serialized to Hazelcast cache for quick lookups.
     * Not persisted to DB (@Transient).
     */
    @Transient
    @JsonIgnore
    @lombok.Builder.Default
    private List<ConfigurationReference> processRefs = new ArrayList<>();

    /**
     * Lightweight references to compounds (ID + name only).
     * Serialized to Hazelcast cache for quick lookups.
     * Not persisted to DB (@Transient).
     */
    @Transient
    @JsonIgnore
    @lombok.Builder.Default
    private List<ConfigurationReference> compoundRefs = new ArrayList<>();

    /**
     * Full process configurations.
     * Persisted to DB via JPA cascade.
     * NOT serialized to Hazelcast cache (transient keyword).
     */
    @OneToMany(mappedBy = "projectConfiguration", targetEntity = ProcessConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 50)
    @JsonIgnore  // Do not return directly in JSON, use DTOs
    private transient List<ProcessConfiguration> processes;

    /**
     * Full compound configurations.
     * Persisted to DB via JPA cascade.
     * NOT serialized to Hazelcast cache (transient keyword).
     */
    @OneToMany(mappedBy = "projectConfiguration", targetEntity = CompoundConfiguration.class,
            cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 50)
    @JsonIgnore  // Do not return directly in JSON, use DTOs
    private transient List<CompoundConfiguration> compounds;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = ProjectDirectory.class,
            cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 50)
    private List<ProjectDirectory> directories;

    @OneToMany(mappedBy = "projectConfiguration", targetEntity = ProjectFile.class,
            cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @BatchSize(size = 50)
    private List<ProjectFile> files;

    /**
     * Service for lazy loading of processes and compounds.
     * Injected via BeanPostProcessor after entity creation/deserialization.
     * Not persisted to DB (transient) and not serialized to JSON.
     */
    @JsonIgnore
    private transient LazyConfigurationLoader lazyLoader;

    /**
     * Set LazyConfigurationLoader for lazy loading of collections.
     * Called automatically after entity creation/deserialization.
     * 
     * @param lazyLoader service for loading
     */
    public void setLazyLoader(LazyConfigurationLoader lazyLoader) {
        this.lazyLoader = lazyLoader;
    }

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
     * Gets compound by name using optimized query.
     * First checks in refs for existence, then loads via LazyConfigurationLoader.
     *
     * @param compoundName compound name
     * @return Optional with compound
     */
    public Optional<CompoundConfiguration> getCompoundByNameSafe(String compoundName) {
        // First check in refs if compound exists
        Optional<ConfigurationReference> ref = compoundRefs.stream()
                .filter(r -> r.getName().equals(compoundName))
                .findFirst();
        
        if (ref.isPresent() && lazyLoader != null && projectId != null) {
            // Load full object by ID from ref
            log.debug("Loading compound '{}' by ID {} for project {}", compoundName, ref.get().getId(), projectId);
            return lazyLoader.loadCompoundById(projectId, ref.get().getId());
        }
        
        // Fallback: direct query by name
        if (lazyLoader != null && projectId != null) {
            log.debug("Lazy loading compound '{}' for project {}", compoundName, projectId);
            return lazyLoader.loadCompoundByName(projectId, compoundName);
        }
        
        // Last resort: search in already loaded collection
        return getCompounds().stream().filter(p -> p.getName().equals(compoundName)).findAny();
    }

    /**
     * Getter for compounds.
     * If compounds is null (after deserialization from cache), converts refs to lightweight objects.
     * This ensures API returns the same structure with id and name populated.
     *
     * @return compounds (lightweight if from cache, full if loaded from DB)
     */
    public List<CompoundConfiguration> getCompounds() {
        if (compounds == null) {
            // After deserialization from cache, convert refs to lightweight objects
            if (!compoundRefs.isEmpty()) {
                log.debug("Converting compoundRefs to lightweight CompoundConfiguration objects");
                compounds = ConfigurationReference.toCompoundConfigurations(compoundRefs);
            } else if (lazyLoader != null && projectId != null) {
                // If refs are empty but lazyLoader available, load from DB
                log.debug("Lazy loading compounds for project {}", projectId);
                compounds = lazyLoader.loadCompounds(projectId);
            } else {
                compounds = new ArrayList<>();
            }
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
     * Gets process by name using optimized query.
     * First checks in refs for existence, then loads via LazyConfigurationLoader.
     *
     * @param processName process name
     * @return Optional with process
     */
    public Optional<ProcessConfiguration> getProcessByNameSafe(String processName) {
        // First check in refs if process exists
        Optional<ConfigurationReference> ref = processRefs.stream()
                .filter(r -> r.getName().equals(processName))
                .findFirst();
        
        if (ref.isPresent() && lazyLoader != null && projectId != null) {
            // Load full object by ID from ref
            log.debug("Loading process '{}' by ID {} for project {}", processName, ref.get().getId(), projectId);
            return lazyLoader.loadProcessById(projectId, ref.get().getId());
        }
        
        // Fallback: direct query by name
        if (lazyLoader != null && projectId != null) {
            log.debug("Lazy loading process '{}' for project {}", processName, projectId);
            return lazyLoader.loadProcessByName(projectId, processName);
        }
        
        // Last resort: search in already loaded collection
        return getProcesses().stream().filter(p -> p.getName().equals(processName)).findAny();
    }

    /**
     * Getter for processes.
     * If processes is null (after deserialization from cache), converts refs to lightweight objects.
     * This ensures API returns the same structure with id and name populated.
     *
     * @return processes (lightweight if from cache, full if loaded from DB)
     */
    public List<ProcessConfiguration> getProcesses() {
        if (processes == null) {
            // After deserialization from cache, convert refs to lightweight objects
            if (!processRefs.isEmpty()) {
                log.debug("Converting processRefs to lightweight ProcessConfiguration objects");
                processes = ConfigurationReference.toProcessConfigurations(processRefs);
            } else if (lazyLoader != null && projectId != null) {
                // If refs are empty but lazyLoader available, load from DB
                log.debug("Lazy loading processes for project {}", projectId);
                processes = lazyLoader.loadProcesses(projectId);
            } else {
                processes = new ArrayList<>();
            }
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

    /**
     * Get list of process names (lightweight, from refs).
     * Does not load full process objects.
     *
     * @return list of process names
     */
    public List<String> getProcessNames() {
        return processRefs.stream()
                .map(ConfigurationReference::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get list of compound names (lightweight, from refs).
     * Does not load full compound objects.
     *
     * @return list of compound names
     */
    public List<String> getCompoundNames() {
        return compoundRefs.stream()
                .map(ConfigurationReference::getName)
                .collect(Collectors.toList());
    }

    /**
     * Get list of process IDs (lightweight, from refs).
     * Does not load full process objects.
     *
     * @return list of process IDs
     */
    public List<UUID> getProcessIds() {
        return processRefs.stream()
                .map(ConfigurationReference::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get list of compound IDs (lightweight, from refs).
     * Does not load full compound objects.
     *
     * @return list of compound IDs
     */
    public List<UUID> getCompoundIds() {
        return compoundRefs.stream()
                .map(ConfigurationReference::getId)
                .collect(Collectors.toList());
    }

    /**
     * Populate process refs from loaded processes.
     * Should be called after loading configuration from DB.
     *
     * @param processes list of processes
     */
    public void populateProcessRefs(List<ProcessConfiguration> processes) {
        if (processes != null) {
            this.processRefs = processes.stream()
                    .map(ConfigurationReference::fromProcess)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Populate compound refs from loaded compounds.
     * Should be called after loading configuration from DB.
     *
     * @param compounds list of compounds
     */
    public void populateCompoundRefs(List<CompoundConfiguration> compounds) {
        if (compounds != null) {
            this.compoundRefs = compounds.stream()
                    .map(ConfigurationReference::fromCompound)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Get process refs (lightweight references).
     *
     * @return list of process references
     */
    public List<ConfigurationReference> getProcessRefs() {
        if (processRefs == null) {
            processRefs = new ArrayList<>();
        }
        return processRefs;
    }

    /**
     * Get compound refs (lightweight references).
     *
     * @return list of compound references
     */
    public List<ConfigurationReference> getCompoundRefs() {
        if (compoundRefs == null) {
            compoundRefs = new ArrayList<>();
        }
        return compoundRefs;
    }
}
