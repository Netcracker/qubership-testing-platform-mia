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

import org.apache.logging.log4j.util.Strings;

import lombok.Data;

@Data
public class VariableFormat {

    public static final String VAR_NAME = "VARIABLE_NAME";
    public static final String DEFAULT_FORMAT = ":" + VAR_NAME;
    private final String[] split;
    private final String beforeVariableName;
    private final String afterVariableName;
    private final StringBuilder matches = new StringBuilder();

    /**
     * Get parameters of VariableFormat.
     *
     * @param variableFormat variable format
     */
    public VariableFormat(String variableFormat) {
        split = variableFormat.split(VariableFormat.VAR_NAME);
        beforeVariableName = !variableFormat.startsWith(VariableFormat.VAR_NAME) ? split[0] : "";
        afterVariableName = !variableFormat.endsWith(VariableFormat.VAR_NAME) ? split[split.length - 1] : "";
        matches.append("[^" + beforeVariableName + "]*");
        matches.append(beforeVariableName);
        if (Strings.isNotBlank(afterVariableName)) {
            matches.append("([^" + afterVariableName + "]*)");
        } else {
            matches.append("([^\\W]*)");
        }
        matches.append(afterVariableName);
    }

    public String getVariableAccordingFormat(String variable) {
        return beforeVariableName + variable + afterVariableName;
    }
}
