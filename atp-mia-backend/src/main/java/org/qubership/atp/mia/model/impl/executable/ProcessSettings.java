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

package org.qubership.atp.mia.model.impl.executable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@Slf4j
@NoArgsConstructor
public class ProcessSettings implements Serializable {

    private static final long serialVersionUID = -5699515696527707001L;
    private String name;
    private List<Input> inputs;
    private List<Validation> currentStatement;
    private String referToInput;
    private List<Prerequisite> prerequisites;
    //@Nonnull
    private Command command;
    private List<Validation> validations;
    private HashMap<String, String> globalVariables;

    /**
     * Creates instance of Process.
     *
     * @param name name of Process
     */
    public ProcessSettings(@Nonnull String name) {
        this.name = name;
    }

    /**
     * Check correct processSetting model.
     */
    public boolean check() {
        return this.name != null && command != null;
    }

    /**
     * Adds Prerequisites.
     */
    public void addPrerequisites(Prerequisite prerequisite) {
        if (this.prerequisites == null) {
            this.prerequisites = new ArrayList<>();
        }
        this.prerequisites.add(prerequisite);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProcessSettings processSettings = (ProcessSettings) o;
        return Objects.equals(name, processSettings.name)
                && Objects.equals(inputs, processSettings.inputs)
                && Objects.equals(prerequisites, processSettings.prerequisites)
                && Objects.equals(command, processSettings.command)
                && Objects.equals(validations, processSettings.validations)
                && Objects.equals(globalVariables, processSettings.globalVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, inputs, prerequisites, command, validations);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ProcessSettings.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("inputs=" + inputs)
                .add("prerequisites=" + prerequisites)
                .add("command=" + command)
                .add("validations=" + validations)
                .add("globalVariables=" + globalVariables)
                .toString();
    }
}
