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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.testdata.TestDataWorkbook;
import org.qubership.atp.mia.service.MiaContext;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString(doNotUseGetters = true)
@Data
@NoArgsConstructor
public class FlowData {

    private UUID projectId;
    private String projectName;
    private UUID sessionId;
    private Map<String, String> parameters = new HashMap<>();
    private TestDataWorkbook testDataWorkbook;
    private Environment environment;

    /**
     * Constructor.
     *
     * @param projectId   project ID
     * @param projectName project name
     * @param sessionId   session ID
     */
    public FlowData(UUID projectId, String projectName, UUID sessionId) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.sessionId = sessionId;
    }

    /**
     * Gets custom parameter from flowData.
     *
     * @param parameter custom parameter
     * @return custom parameter
     */
    public String getCustom(Constants.CustomParameters parameter, MiaContext miaContext) {
        if (Strings.isNotBlank(parameters.get(parameter.toString()))) {
            parameters.put(parameter.toString(), miaContext.evaluate(parameters.get(parameter.toString())));
        }
        return parameters.get(parameter.toString());
    }

    /**
     * Add parameters.
     *
     * @param parameters parameters to add
     */
    public void addParameters(Map<String, String> parameters) {
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }

    /**
     * Add custom parameter.
     *
     * @param parameterKey   parameterKey
     * @param parameterValue parameterValue
     */
    public void addParameter(Constants.CustomParameters parameterKey, String parameterValue) {
        addParameter(parameterKey.toString(), parameterValue);
    }

    /**
     * Adds parameter.
     *
     * @param parameterKey   parameterKey
     * @param parameterValue parameterValue
     */
    public void addParameter(String parameterKey, String parameterValue) {
        parameters.put(parameterKey, parameterValue);
    }

    /**
     * Removes parameter.
     *
     * @param parameterKey parameterKey
     */
    public void removeParameter(String parameterKey) {
        this.parameters.remove(parameterKey);
    }

    /**
     * Get parameters.
     *
     * @return flow data parameters
     */
    public Map<String, String> getParameters() {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        return this.parameters;
    }

    /**
     * Set parameters.
     *
     * @param parameters parameters.
     */
    public void setParameters(Map<String, String> parameters) {
        if (parameters != null) {
            if (this.parameters != null && !this.parameters.isEmpty()) {
                this.parameters.forEach(parameters::putIfAbsent);
            }
            this.parameters = parameters;
        } else {
            this.parameters = new HashMap<>();
        }
    }

    /**
     * Gets System by name.
     *
     * @return System instance
     */
    public System getSystem(String systemName) {
        return environment.getSystem(systemName);
    }
}
