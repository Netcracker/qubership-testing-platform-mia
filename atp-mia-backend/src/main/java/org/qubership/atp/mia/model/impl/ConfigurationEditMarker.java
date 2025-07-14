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

package org.qubership.atp.mia.model.impl;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationEditMarker {

    private String projectId;
    private String configurationName;
    private String configurationType; //PROCESS/COMPOUND/SECTION
    private String modified = null;
    private String lockedBy = null;
    private boolean isLockedForEdit = false;

    public String getKey() {
        return getProjectId() + "_" + getConfigurationType() + "_" + getConfigurationName().replaceAll(" ", "_");
    }
}
