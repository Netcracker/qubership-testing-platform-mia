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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExportImportEntities {
    MIA_CONFIGURATION("miaConfiguration"),
    MIA_SECTION("miaSections"),
    MIA_COMPOUNDS("miaCompounds"),
    MIA_PROCESSES("miaProcesses"),
    MIA_PROJECT_CONFIGURATION("miaProjectConfiguration"),
    MIA_COMMON_CONFIGURATION("miaCommonConfiguration"),
    MIA_HEADER_CONFIGURATION("miaHeaderConfiguration"),
    MIA_POT_HEADER_CONFIGURATION("miaPotHeaderConfiguration"),
    MIA_FILES("miaFiles"),
    MIA_DIRECTORY("miaDirectories"),
    GRID_FS_FILE("gridFsFile");

    private final String value;

    public boolean equals(String value) {
        return this.value.equals(value);
    }

    public String getValue() {
        return value;
    }
}
