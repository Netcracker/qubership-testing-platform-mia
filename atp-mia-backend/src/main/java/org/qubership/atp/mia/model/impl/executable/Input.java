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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Slf4j
public class Input extends GeneralModel {

    private static final long serialVersionUID = -1703685321343901847L;
    @Nonnull
    private String label;
    private int maxLength;
    private String mask;
    private String maskPattern;
    private boolean required;
    @Nullable
    private List<HashMap<String, String>> referToInput;

    /**
     * Clone object.
     *
     * @return Input instance
     */
    public Input clone() {
        if (label == null) {
            label = "Label should be defined!";
            log.error("Label is not defined! {}", this);
        }
        InputBuilder builder = this.toBuilder();
        if (referToInput != null) {
            builder.referToInput(new ArrayList<>(referToInput));
        }
        builder.values(cloneValues());
        return builder.build();
    }

    /**
     * Creates {@code Input} instance without parameters.
     */

    public Input() {
        super();
    }

    /**
     * Creates {@code Input} instance with parameters.
     *
     * @param name   name
     * @param type   type
     * @param values values
     */

    public Input(@Nonnull String name, @Nonnull String type, @Nullable LinkedHashSet<String> values) {
        super(name, type, null, values);
    }

    /**
     * Creates {@code Input} instance with parameters.
     *
     * @param name  name
     * @param type  type
     * @param value value
     */
    public Input(@Nonnull String name, @Nonnull String type, @Nullable String value) {
        super(name, type, null, value);
    }

    @Nonnull
    public String getLabel() {
        return label;
    }

    /**
     * Sets label.
     *
     * @param label label
     * @return {@code Input} instance
     */
    public Input setLabel(@Nonnull String label) {
        this.label = label;
        return this;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Sets  maxLength.
     *
     * @param maxLength maxLength
     * @return {@code Input} instance
     */
    public Input setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public String getMask() {
        return mask;
    }

    public Input setMask(String mask) {
        this.mask = mask;
        return this;
    }

    public String getMaskPattern() {
        return maskPattern;
    }

    public void setMaskPattern(String maskPattern) {
        this.maskPattern = maskPattern;
    }

    public boolean getRequired() {
        return required;
    }

    public Input setRequired(boolean required) {
        this.required = required;
        return this;
    }

    @Nullable
    public List<HashMap<String, String>> getReferToInput() {
        return referToInput;
    }

    /**
     * Sets marker.
     *
     * @param referToInput referToInput
     * @return {@code Input} instance
     */

    public Input setReferToInput(@Nullable List<HashMap<String, String>> referToInput) {
        this.referToInput = referToInput;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Input.class.getSimpleName() + "[", "]")
                .add("label='" + label + "'")
                .add("maxLength=" + maxLength)
                .add("mask='" + mask + "'")
                .add("maskPattern='" + maskPattern + "'")
                .add("required=" + required)
                .add("referToInput=" + referToInput)
                .add("name='" + getName() + "'")
                .add("type='" + getType() + "'")
                .add("system='" + getSystem() + "'")
                .add("value='" + getValue() + "'")
                .add("values='" + getValues() + "'")
                .toString();
    }
}
