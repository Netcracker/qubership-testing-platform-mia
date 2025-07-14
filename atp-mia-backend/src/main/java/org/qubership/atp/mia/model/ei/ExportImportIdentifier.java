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

import java.io.Serializable;
import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class ExportImportIdentifier implements Serializable {

    protected UUID id;
    protected String name;
    @EqualsAndHashCode.Exclude
    protected UUID projectId;
    @EqualsAndHashCode.Exclude
    protected UUID sourceId;

    /**
     * Class contains common fields for Export/Import models.
     * This constructor called as super() from its children.
     *
     * @param id - id of object
     * @param name - name of object
     * @param projectId - projectId
     * @param sourceId - id to identify that it the same object
     */
    public ExportImportIdentifier(UUID id, String name, UUID projectId, UUID sourceId) {
        this.id = id;
        this.name = name;
        this.projectId = projectId;
        this.sourceId = sourceId == null ? id : sourceId;
    }
}
