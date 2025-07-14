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

import java.util.LinkedHashMap;

import org.qubership.atp.mia.model.configuration.CommandPrefix;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode()
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
public class ExportImportCommandPrefix {

    private LinkedHashMap<String, String> prefixes;
    private String system;

    /**
     * Create new one from CommandPrefix.
     *
     * @param c CommandPrefix
     */
    public ExportImportCommandPrefix(CommandPrefix c) {
        this.system = c.getSystem();
        this.prefixes = c.getPrefixes();
    }

    /**
     * To entity.
     *
     * @param projectConfiguration ProjectConfiguration
     * @param eiCommandPrefix      ExportImportCommandPrefix
     * @param commonConfiguration  CommonConfiguration
     * @return CommandPrefix
     */
    public static CommandPrefix toEntity(ProjectConfiguration projectConfiguration,
                                         ExportImportCommandPrefix eiCommandPrefix,
                                         CommonConfiguration commonConfiguration) {
        return CommandPrefix.builder()
                .projectId(projectConfiguration.getProjectId())
                .system(eiCommandPrefix.getSystem())
                .prefixes(eiCommandPrefix.getPrefixes())
                .commonConfiguration(commonConfiguration)
                .build();
    }
}
