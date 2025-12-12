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

package org.qubership.atp.mia.model.impl.testdata;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelUnknownCells;
import lombok.Data;

@Data
public class MainSheetRecord {

    public static final String SCENARIO = "Scenario";
    public static final String DESCRIPTION = "Description";

    @ExcelCellName(SCENARIO)
    private String scenario;

    @ExcelCellName(DESCRIPTION)
    private String description;

    @ExcelUnknownCells
    private HashMap<String, String> otherCells;

    /**
     * Gets otherCells.
     *
     * @param allKeysForOtherKeys keys should be in otherCells
     * @return LinkedHashMap
     */
    public LinkedHashMap<String, String> getOtherCells(Set<String> allKeysForOtherKeys) {
        final LinkedHashMap<String, String> otherCellsReturn = new LinkedHashMap<>();
        if (otherCells == null) {
            otherCells = new HashMap<>();
        }
        allKeysForOtherKeys.forEach(k -> otherCellsReturn.put(k, otherCells.getOrDefault(k, "")));
        return otherCellsReturn;
    }
}
