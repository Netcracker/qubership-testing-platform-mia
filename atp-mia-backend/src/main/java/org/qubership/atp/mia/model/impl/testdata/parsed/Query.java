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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.pot.db.DbType;
import org.qubership.atp.mia.model.pot.db.table.DbTable;

import lombok.Data;

@Data
public class Query {

    private final String query;
    private LinkedHashSet<ValidateValue> validateValue;
    private DbType type;

    /**
     * Constructor with query and query type.
     *
     * @param query query
     * @param type  query type
     */
    public Query(String query, String type) {
        this.query = query;
        try {
            this.type = DbType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.type = DbType.ORACLE;
        }
        this.validateValue = new LinkedHashSet<>();
    }

    /**
     * Add validate and validated columns.
     *
     * @param validateName  validate column name
     * @param validatedName validated column name
     */
    public void addValidateValue(String validateName, String validatedName) {
        if (this.validateValue == null) {
            this.validateValue = new LinkedHashSet<>();
        }
        this.validateValue.add(new ValidateValue(validateName.toUpperCase(), validatedName.toUpperCase()));
    }

    /**
     * Update results from list of commandResponses.
     *
     * @param commandResponses list of CommandResponses
     */
    public void updateResultsFromListCommandResponses(List<CommandResponse> commandResponses) {
        if (commandResponses == null || commandResponses.size() < 1
                || commandResponses.get(0).getSqlResponse() == null
                || commandResponses.get(0).getSqlResponse().getData() == null) {
            validateValue.stream().filter(v -> v.getValue() == null).forEach(v -> v.setValue("ERROR: NO QUERY RESULT"));
        } else {
            final DbTable table = commandResponses.get(0).getSqlResponse().getData();
            for (int colId = 0; colId < table.getColumns().size(); colId++) {
                final String colName = table.getColumns().get(colId);
                final String colValue =
                        table.getData() != null
                                && table.getData().size() > 0
                                && table.getData().get(0).size() >= colId
                                ? table.getData().get(0).get(colId)
                                : "ERROR: NO COLUMN RESULT";
                Optional<ValidateValue> validation =
                        validateValue.stream().filter(v -> v.getValidatedName().equalsIgnoreCase(colName)).findAny();
                if (validation.isPresent()) {
                    validation.get().setValue(colValue);
                }
            }
            validateValue.stream().filter(v -> v.getValue() == null).forEach(v ->
                    v.setValue("ERROR: COLUMN NOT PRESENT IN QUERY"));
        }
    }

    /**
     * Update results with string.
     *
     * @param string string
     */
    public void updateResultsWithString(String string) {
        validateValue.stream().filter(v -> v.getValue() == null).forEach(v ->
                v.setValue(string));
    }
}
