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

import java.util.LinkedList;

import lombok.Data;

@Data
public class Scenario {

    private final String name;
    private LinkedList<Description> descriptions;

    public Scenario(String name) {
        this.name = name;
        this.descriptions = new LinkedList<>();
    }

    /**
     * Add description.
     *
     * @param description description
     */
    public void addDescription(Description description) {
        if (descriptions == null) {
            this.descriptions = new LinkedList<>();
        }
        descriptions.add(description);
    }

    /**
     * Gets number of descriptions which are passed.
     *
     * @return number of descriptions
     */
    public long getPassedValidatedNumber() {
        return descriptions.stream().filter(Description::isPassed).count();
    }

    public long getFailedValidatedNumber() {
        return descriptions.size() - descriptions.stream().filter(Description::isPassed).count();
    }
}
