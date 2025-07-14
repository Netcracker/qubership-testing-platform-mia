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

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ExportImportProcess extends ExportImportIdentifier {

    private static final long serialVersionUID = 5179060695381416613L;

    private List<UUID> inCompounds;
    private List<UUID> inSections;
    private ProcessSettings processSettings;

    /**
     * Create new one from ProcessConfiguration.
     *
     * @param p ProcessConfiguration
     */
    public ExportImportProcess(ProcessConfiguration p) {
        super(p.getId(), p.getName(), p.getProjectConfiguration().getProjectId(), p.getSourceId());
        this.processSettings = p.getProcessSettings();

        if (p.getInSections() != null) {
            this.inSections = p.getInSections().stream()
                    .filter(Objects::nonNull)
                    .map(SectionConfiguration::getId)
                    .collect(Collectors.toList());
        }

        if (p.getInCompounds() != null) {
            this.inCompounds = p.getInCompounds().stream()
                    .filter(Objects::nonNull)
                    .map(CompoundConfiguration::getId)
                    .collect(Collectors.toList());
        }
    }
}
