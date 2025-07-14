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
import java.util.List;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString
public class Validation extends GeneralModel {

    private static final long serialVersionUID = -9044822576162327101L;
    private String tableName;
    @Nullable
    private List<String> referToCommandExecution;
    @Nullable
    private TableMarker tableMarker;
    @Nullable
    private HashMap<String, String> exportVariables;

    private boolean saveToWordFile = true;
    private boolean saveToZipFile = false;

    /**
     * Clone object.
     *
     * @return Validation instance
     */
    public Validation clone() {
        ValidationBuilder builder = this.toBuilder();
        builder.values(cloneValues());
        if (referToCommandExecution != null) {
            builder.referToCommandExecution(new ArrayList<>(referToCommandExecution));
        }
        if (tableMarker != null) {
            builder.tableMarker(tableMarker.clone());
        }
        if (exportVariables != null) {
            builder.exportVariables(new HashMap<>(exportVariables));
        }
        return builder.build();
    }

    /**
     * Creates {@code Validation} instance without parameters.
     */
    public Validation() {
        super();
    }

    /**
     * Creates {@code Validation} instance with parameters.
     *
     * @param type   type of validation
     * @param system system of validation
     * @param value  value of validation
     */
    public Validation(@Nonnull String type, @Nonnull String system, @Nonnull String value) {
        super(null, type, system, value);
    }

    @Nonnull
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets tableName.
     *
     * @param tableName tableName
     * @return {@code Validation} instance
     */
    public Validation setTableName(@Nonnull String tableName) {
        this.tableName = tableName;
        return this;
    }

    @Nullable
    public List<String> getReferToCommandExecution() {
        return referToCommandExecution;
    }

    /**
     * Sets referToCommandExecution.
     *
     * @param referToCommandExecution referToCommandExecution
     * @return {@code Validation} instance
     */
    public Validation setReferToCommandExecution(@Nullable List<String> referToCommandExecution) {
        this.referToCommandExecution = referToCommandExecution;
        return this;
    }

    @Nullable
    public TableMarker getTableMarker() {
        return tableMarker;
    }

    /**
     * Sets marker.
     *
     * @param tableMarker tableMarker
     * @return {@code Command} instance
     */
    public Validation setTableMarker(@Nullable TableMarker tableMarker) {
        this.tableMarker = tableMarker;
        return this;
    }

    @Nullable
    public HashMap<String, String> getExportVariables() {
        return exportVariables;
    }

    /**
     * Sets marker.
     *
     * @param exportVariables exportVariables
     * @return {@code Validation} instance
     */
    public Validation setExportVariables(@Nullable HashMap<String, String> exportVariables) {
        this.exportVariables = exportVariables;
        return this;
    }

    public boolean isSaveToWordFile() {
        return saveToWordFile;
    }

    public void setSaveToWordFile(@Nullable boolean saveToWordFile) {
        this.saveToWordFile = Boolean.parseBoolean(String.valueOf(saveToWordFile));
    }

    public boolean isSaveToZipFile() {
        return saveToZipFile;
    }

    public void setSaveToZipFile(@Nullable boolean saveToZipFile) {
        this.saveToZipFile = Boolean.parseBoolean(String.valueOf(saveToZipFile));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Validation.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("type='" + getType() + "'")
                .add("system='" + getSystem() + "'")
                .add("value='" + getValue() + "'")
                .add("values='" + getValues() + "'")
                .add("tableName='" + tableName + "'")
                .add("referToCommandExecution=" + referToCommandExecution + "'")
                .add("tableMarker=" + tableMarker + "'")
                .add("exportVariables=" + exportVariables + "'")
                .add("saveToWordFile=" + saveToWordFile + "'")
                .add("saveToZipFile=" + saveToZipFile + "'")
                .toString();
    }
}
