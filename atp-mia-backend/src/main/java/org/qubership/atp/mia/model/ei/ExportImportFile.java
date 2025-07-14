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

package org.qubership.atp.mia.model.ei;

import java.util.UUID;

import org.qubership.atp.mia.model.file.ProjectFile;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ExportImportFile extends ExportImportIdentifier {

    private static final long serialVersionUID = -7741694660485382873L;

    private UUID directory;
    private String gridFsId;

    /**
     * Create new one from ProjectFile.
     *
     * @param file ProjectFile
     */
    public ExportImportFile(ProjectFile file) {
        super(file.getId(), file.getName(), file.getProjectConfiguration().getProjectId(), file.getSourceId());
        this.directory = file.getDirectory() == null ? null : file.getDirectory().getId();
        this.gridFsId = file.getGridFsObjectId();
    }
}
