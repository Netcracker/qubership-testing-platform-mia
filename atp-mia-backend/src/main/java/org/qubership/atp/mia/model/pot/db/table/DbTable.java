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

package org.qubership.atp.mia.model.pot.db.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonSerialize(using = TableSerializer.class)
@JsonDeserialize(using = TableDeserializer.class)
@Data
@NoArgsConstructor
public class DbTable {

    private List<String> columns;
    private List<List<String>> data;
    private int actualDataSizeBeforeLimit;

    public DbTable(List<String> columns, List<List<String>> data) {
        this.columns = columns;
        this.data = data;
    }

    /**
     * DbTable with headers and rows.
     *
     * @param headers headers
     * @param rows    rows
     */
    public DbTable(Collection<String> headers,
                   Stream<List<String>> rows) {
        this.columns = Lists.newArrayList(headers);
        this.data = rows.collect(Collectors.toList());
    }

    /**
     * Add data into table.
     *
     * @param data record
     */
    public void addData(List<String> data) {
        if (this.data == null) {
            this.data = new ArrayList<>();
        }
        this.data.add(data);
    }
}
