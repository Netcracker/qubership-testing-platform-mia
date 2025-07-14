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

package org.qubership.atp.mia.model.file;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_directory")
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDirectory extends DateAuditorEntity {

    private static final long serialVersionUID = -1184948591984244690L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    protected UUID id;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "directory_name")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String name;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffInclude
    private ProjectDirectory parentDirectory;

    @OneToMany(mappedBy = "parentDirectory", targetEntity = ProjectDirectory.class, cascade = CascadeType.ALL)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<ProjectDirectory> directories = new ArrayList<>();

    @ManyToOne(targetEntity = ProjectConfiguration.class)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    @OneToMany(mappedBy = "directory", targetEntity = ProjectFile.class, cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<ProjectFile> files = new ArrayList<>();

    /**
     * Get UUID for all nested children.
     *
     * @return list of {@link UUID}
     */
    public List<UUID> getChildrenUuid() {
        List<UUID> childsUuid = new ArrayList<>();
        directories.forEach(s -> {
            childsUuid.add(s.getId());
            childsUuid.addAll(s.getChildrenUuid());
        });
        return childsUuid;
    }

    /**
     * Get Names for all nested children.
     *
     * @return list of {@link String}
     */
    public List<String> getChildrenNames() {
        List<String> childsNames = new ArrayList<>();
        directories.forEach(s -> {
            childsNames.add(s.getName());
            childsNames.addAll(s.getChildrenNames());
        });
        return childsNames;
    }

    /**
     * Get Names for all nested children.
     *
     * @return list of {@link String}
     */
    public List<String> getFilesNames() {
        List<String> filesNames = new ArrayList<>();
        files.forEach(s -> {
            filesNames.add(s.getName());
        });
        return filesNames;
    }

    /**
     * Get path for directory.
     *
     * @return full path for directory
     */
    public Path getPathDirectory() {
        Path path = Paths.get("");
        if (getParentDirectory() != null) {
            path = getParentDirectory().getPathDirectory();
        }
        path = path.resolve(getName());
        return path;
    }

    /**
     * Get section with all nested children.
     *
     * @return list of {@link ProjectDirectory}
     */
    public List<ProjectDirectory> getWithChildrenDirectories() {
        List<ProjectDirectory> dirs = new ArrayList<>();
        directories.forEach(d -> {
            dirs.add(d);
            dirs.addAll(d.getWithChildrenDirectories());
        });
        return dirs;
    }
}
