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
import java.util.LinkedHashSet;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * GeneralModel abstract class for Input, Command, Validation and Prerequisite classes.
 */
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode
public abstract class GeneralModel implements Serializable {

    private static final long serialVersionUID = -3562160622292686270L;
    private String name;
    private String type;
    private String system;
    private LinkedHashSet<String> values;
    protected String value;

    /**
     * Creates an instance of GeneralModel class.
     *
     * @param name   name
     * @param type   type
     * @param system system
     * @param values values
     */
    public GeneralModel(String name, String type, String system, LinkedHashSet<String> values) {
        this.name = name;
        this.type = type;
        this.system = system;
        this.values = values;
    }

    /**
     * Creates an instance of GeneralModel class.
     *
     * @param name   name
     * @param type   type
     * @param system system
     * @param value  value
     */
    public GeneralModel(String name, String type, String system, String value) {
        this.name = name;
        this.type = type;
        this.system = system;
        addValue(value);
    }

    /**
     * Clone values.
     *
     * @return {@code LinkedHashSet} of values otherwise null.
     */
    @Nullable
    public LinkedHashSet<String> cloneValues() {
        if (values != null) {
            return new LinkedHashSet<>(values);
        }
        return null;
    }

    /**
     * Gets name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets type.
     *
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets system.
     *
     * @return system
     */
    public String getSystem() {
        return system;
    }

    /**
     * Sets system.
     *
     * @param system system
     */
    public void setSystem(String system) {
        this.system = system;
    }

    /**
     * Sets system name if it is NULL.
     *
     * @param system system name
     */
    public void setSystemIfNull(@Nonnull String system) {
        if (this.system == null) {
            this.system = system;
        }
    }

    /**
     * Gets values.
     *
     * @return values
     */
    public LinkedHashSet<String> getValues() {
        return values;
    }

    /**
     * Sets values.
     *
     * @param values values
     */
    public void setValues(LinkedHashSet<String> values) {
        this.values = values;
    }

    /**
     * Gets values as string.
     *
     * @return value
     */
    public String getValue() {
        if (this.values == null || this.values.isEmpty() || values.contains(null)) {
            return null;
        }
        return this.values.stream().findFirst().get();
    }

    /**
     * Sets the value.
     *
     * @param value value
     */
    public void setValue(String value) {
        if (value != null) {
            this.values = new LinkedHashSet<>();
            this.values.add(value);
        }
    }

    /**
     * Adds a value.
     *
     * @param value value
     */
    public void addValue(String value) {
        if (value != null) {
            if (this.values == null) {
                this.values = new LinkedHashSet<>();
            }
            this.values.add(value);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("type='" + type + "'")
                .add("system='" + system + "'")
                .add("value='" + value + "'")
                .add("values='" + values + "'")
                .toString();
    }
}
