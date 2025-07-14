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

package org.qubership.atp.mia.model.environment;

import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class Connection extends AbstractConfiguratorModel {

    private UUID systemId;
    private Map<String, String> parameters;
    private UUID sourceTemplateId;
    private String connectionType;

    /**
     * SourceTemplateId from ENVIRONMENT tool.
     */
    public enum SourceTemplateId {
        HTTP(UUID.fromString("2a0eab16-0fe7-4a12-8155-78c0c151abdf")),
        DB(UUID.fromString("46ca25d6-058e-471a-9b5e-c13e4b481227")),
        SSH(UUID.fromString("24136d83-5ffb-487f-9bb4-e73be3a89aa2"));

        public final UUID id;

        SourceTemplateId(UUID id) {
            this.id = id;
        }
    }
}

