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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.experimental.SuperBuilder;

/**
 * Class for prerequisite in Process.
 */
@SuperBuilder(toBuilder = true)
public class Prerequisite extends GeneralModel {

    private static final long serialVersionUID = -7300693894187291869L;
    @Nullable
    private List<String> referToInputName;
    @Nullable
    private List<String> referToCommandValue;

    /**
     * Clone object.
     *
     * @return Prerequisite instance
     */
    public Prerequisite clone() {
        PrerequisiteBuilder builder = this.toBuilder();
        builder.values(cloneValues());
        if (referToInputName != null) {
            builder.referToInputName(new ArrayList<>(referToInputName));
        }
        if (referToCommandValue != null) {
            builder.referToCommandValue(new ArrayList<>(referToCommandValue));
        }
        return builder.build();
    }

    /**
     * Creates {@code Prerequisite} instance without parameters.
     */
    public Prerequisite() {
        super();
    }

    /**
     * Creates {@code Prerequisite} instance with parameters.
     *
     * @param type   type
     * @param system system
     * @param values values
     */
    public Prerequisite(@Nonnull String type, @Nonnull String system, @Nonnull LinkedHashSet<String> values) {
        super(null, type, system, values);
    }

    /**
     * Creates {@code Prerequisite} instance with parameters.
     *
     * @param type   type
     * @param system system
     * @param value  value
     */
    public Prerequisite(@Nonnull String type, @Nonnull String system, @Nonnull String value) {
        super(null, type, system, value);
    }

    /**
     * Creates {@code Prerequisite} instance with parameters.
     *
     * @param referToInputName    referToInputName
     * @param referToCommandValue referToCommandValue
     */
    public Prerequisite(@Nullable List<String> referToInputName, @Nullable List<String> referToCommandValue) {
        this.referToInputName = referToInputName;
        this.referToCommandValue = referToCommandValue;
    }

    /**
     * Creates {@code Prerequisite} instance with parameters.
     *
     * @param name                name
     * @param type                type
     * @param system              system
     * @param values              values
     * @param referToInputName    referToInputName
     * @param referToCommandValue referToCommandValue
     */
    public Prerequisite(String name, String type, String system, LinkedHashSet<String> values,
                        @Nullable List<String> referToInputName, @Nullable List<String> referToCommandValue) {
        super(name, type, system, values);
        this.referToInputName = referToInputName;
        this.referToCommandValue = referToCommandValue;
    }

    /**
     * Creates {@code Prerequisite} instance with parameters.
     *
     * @param name                name
     * @param type                type
     * @param system              system
     * @param value               value
     * @param referToInputName    referToInputName
     * @param referToCommandValue referToCommandValue
     */
    public Prerequisite(String name, String type, String system, String value,
                        @Nullable List<String> referToInputName, @Nullable List<String> referToCommandValue) {
        super(name, type, system, value);
        this.referToInputName = referToInputName;
        this.referToCommandValue = referToCommandValue;
    }

    /**
     * Gets  referToInputName.
     *
     * @return referToInputName
     */
    @Nullable
    public List<String> getReferToInputName() {
        return referToInputName;
    }

    /**
     * Sets referToInputName.
     *
     * @param referToInputName referToInputName
     * @return {@code Prerequisite} instance
     */
    public Prerequisite setReferToInputName(@Nullable List<String> referToInputName) {
        this.referToInputName = referToInputName;
        return this;
    }

    @Nullable
    public List<String> getReferToCommandValue() {
        return referToCommandValue;
    }

    public void setReferToCommandValue(@Nullable List<String> referToCommandValue) {
        this.referToCommandValue = referToCommandValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Prerequisite that = (Prerequisite) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(getSystem(), that.getSystem())
                && Objects.equals(getValues(), that.getValues())
                && Objects.equals(referToInputName, that.referToInputName)
                && Objects.equals(referToCommandValue, that.referToCommandValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getSystem(), getValues(), referToInputName, referToCommandValue);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Prerequisite.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("type='" + getType() + "'")
                .add("system='" + getSystem() + "'")
                .add("value='" + getValue() + "'")
                .add("values='" + getValues() + "'")
                .add("referToInputName=" + referToInputName + "'")
                .add("referToCommandValue=" + referToCommandValue + "'")
                .toString();
    }
}
