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

package org.qubership.atp.mia.model.impl.testdata.parsed;

import static org.qubership.atp.mia.model.impl.testdata.parsed.ValidatedParameters.State.PASSED;
import static org.qubership.atp.mia.model.impl.testdata.parsed.ValidatedParameters.State.SKIPPED;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.mia.model.impl.testdata.TestDataWorkbook;
import org.qubership.atp.mia.service.MiaContext;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Description {

    private String name;
    private LinkedHashMap<String, String> eventParams;
    private LinkedHashMap<String, String> validationParams;
    private LinkedList<ValidatedParameters> validatedParams;
    private LinkedHashMap<String, String> otherParams;
    private boolean status = false;
    private boolean validated = false;
    private boolean onMainSheet = true;

    /**
     * Constructor with sort params of description.
     *
     * @param name        name of description
     * @param onMainSheet is Description on Main or TestData sheet
     * @param params      all params
     * @param miaContext  miaContext
     */
    public Description(String name, boolean onMainSheet, Map<String, String> params, MiaContext miaContext) {
        this(name, onMainSheet, params, miaContext, true);
    }

    /**
     * Constructor with sort params of description.
     *
     * @param name       name of description
     * @param params     all params
     * @param miaContext miaContext
     */
    public Description(String name, Map<String, String> params, MiaContext miaContext) {
        this(name, true, params, miaContext, true);
    }

    /**
     * Constructor with sort or not of params of description.
     *
     * @param name        name of description
     * @param onMainSheet is Description on Main or TestData sheet
     * @param params      all params
     * @param miaContext  miaContext
     * @param isSort      sort is needed or not
     */
    public Description(String name, boolean onMainSheet, Map<String, String> params, MiaContext miaContext,
                       boolean isSort) {
        this.name = name;
        this.onMainSheet = onMainSheet;
        if (isSort) {
            params.forEach((k, v) -> {
                final String key = k.toUpperCase();
                final String value = miaContext.evaluate(v);
                if (key.startsWith("EVENT_")) {
                    this.addEventParam(key, value);
                } else if (key.startsWith("VALIDATE_")) {
                    this.addValidationParam(key, value);
                } else if (key.startsWith("VALIDATED_")) {
                    this.addValidatedParam(key, value);
                } else {
                    this.addOtherParam(key, value);
                }
            });
        } else {
            this.otherParams = new LinkedHashMap<>();
            otherParams.putAll(params);
        }
    }

    /**
     * Add event parameter.
     *
     * @param key   key
     * @param value value
     */
    private void addEventParam(String key, String value) {
        if (eventParams == null) {
            this.eventParams = new LinkedHashMap<>();
        }
        eventParams.put(key, value);
    }

    /**
     * Add validation parameter.
     *
     * @param key   key
     * @param value value
     */
    private void addValidationParam(String key, String value) {
        if (validationParams == null) {
            this.validationParams = new LinkedHashMap<>();
        }
        validationParams.put(key, value);
    }

    /**
     * Add validated parameter.
     *
     * @param key   key
     * @param value value
     */
    public void addValidatedParam(String key, String value) {
        if (validatedParams == null) {
            this.validatedParams = new LinkedList<>();
        }
        validatedParams.add(new ValidatedParameters(key, value));
    }

    /**
     * Add validated parameter.
     *
     * @param key           key
     * @param value         value
     * @param state         state of validation
     * @param expectedValue Expected, where as value contains actualValue
     */
    public void updateValidatedParam(String key, String value, ValidatedParameters.State state, String expectedValue) {
        if (validatedParams == null || !validatedParams.stream().anyMatch(v -> v.getKey().equals(key))) {
            addValidatedParam(key, value);
        }
        ValidatedParameters validParam = validatedParams.stream().filter(v -> v.getKey().equals(key)).findAny().get();
        if (ValidatedParameters.State.FAILED == state) {
            value = "[AR : " + value + "]\n[ER : " + expectedValue + "]";
        }
        log.debug("Set validated param '{}' with value '{}' and state '{}'", key, value, state);
        validParam.setValue(value);
        validParam.setState(state);
    }

    /**
     * Add other parameter.
     *
     * @param key   key
     * @param value value
     */
    private void addOtherParam(String key, String value) {
        if (otherParams == null) {
            this.otherParams = new LinkedHashMap<>();
        }
        if (Strings.isEmpty(value)) {
            otherParams.putIfAbsent(key, value);
        } else {
            otherParams.put(key, value);
        }
    }

    /**
     * Add other parameter (and add to each scenario).
     *
     * @param key              key
     * @param value            value
     * @param testDataWorkbook testDataWorkbook
     */
    public void addOtherParam(String key, String value, TestDataWorkbook testDataWorkbook) {
        if (otherParams == null) {
            this.otherParams = new LinkedHashMap<>();
        }
        otherParams.put(key, value);
        if (this.onMainSheet) {
            testDataWorkbook.getMainSheet().getScenarios()
                    .forEach(s -> s.getDescriptions().forEach(d -> d.addOtherParam(key, "")));
        } else {
            testDataWorkbook.getTestDataSheet().getScenarios()
                    .forEach(s -> s.getDescriptions().forEach(d -> d.addOtherParam(key, "")));
        }
    }

    /**
     * Gets all parameters.
     *
     * @return map of all parameters
     */
    public LinkedHashMap<String, String> getAllParams() {
        LinkedHashMap<String, String> returnMap = new LinkedHashMap<>();
        if (eventParams != null) {
            returnMap.putAll(eventParams);
        }
        if (validationParams != null) {
            returnMap.putAll(validationParams);
        }
        if (otherParams != null) {
            returnMap.putAll(otherParams);
        }
        return returnMap;
    }

    /**
     * Gets state of validation.
     *
     * @return true if all validation is passed
     */
    public boolean isPassed() {
        if (!validated) {
            this.status = validatedParams.stream().allMatch(vp ->
                    PASSED == vp.getState() || SKIPPED == vp.getState());
            this.validated = true;
        }
        return this.status;
    }
}
