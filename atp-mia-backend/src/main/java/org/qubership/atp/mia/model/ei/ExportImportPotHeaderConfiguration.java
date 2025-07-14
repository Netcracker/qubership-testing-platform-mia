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

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.impl.executable.PotHeader;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
public class ExportImportPotHeaderConfiguration extends ExportImportIdentifier {

    private static final long serialVersionUID = -95403036578824001L;

    private List<PotHeader> headers = new ArrayList<>();

    /**
     * Create new one from PotHeaderConfiguration.
     *
     * @param c PotHeaderConfiguration
     */
    public ExportImportPotHeaderConfiguration(PotHeaderConfiguration c) {
        super(c.getProjectId(), "PotHeaderConfiguration", c.getProjectId(), c.getProjectId());
        this.headers = c.getHeaders();
    }
}
