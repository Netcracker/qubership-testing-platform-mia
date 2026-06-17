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

package org.qubership.atp.mia.model.file;

import java.io.Serial;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "project_file")
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProjectFile extends DateAuditorEntity {

    @Serial
    private static final long serialVersionUID = 8428162091529756975L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;
    @Column(name = "source_id")
    private UUID sourceId;
    @Column(name = "file_name")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private String name;
    @Column(name = "gridfs_object_id")
    @EqualsAndHashCode.Exclude
    private String gridFsObjectId;
    @Column(name = "last_update_when")
    @EqualsAndHashCode.Exclude
    private LocalDateTime lastUpdateWhen;
    @Column(name = "last_update_by")
    @EqualsAndHashCode.Exclude
    private String lastUpdateBy;
    @Column(name = "size")
    @EqualsAndHashCode.Exclude
    @DiffInclude
    private Long size;
    @ManyToOne(targetEntity = ProjectDirectory.class)
    @JoinColumn(name = "directory_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffInclude
    private ProjectDirectory directory;
    @ManyToOne(targetEntity = ProjectConfiguration.class)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    /**
     * Get path for project file.
     *
     * @return full path for project file
     */
    public Path getPathFile() {
        Path pathFile = Path.of("");
        if (directory != null) {
            pathFile = pathFile.resolve(directory.getPathDirectory());
        }
        return pathFile.resolve(name);
    }
}
