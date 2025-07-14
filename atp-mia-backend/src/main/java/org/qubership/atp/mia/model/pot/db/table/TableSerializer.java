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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class TableSerializer extends JsonSerializer<DbTable> {

    @Override
    public void serialize(DbTable table, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        buildHeader(jsonGenerator, table.getColumns());
        buildBody(jsonGenerator, table.getData());
        jsonGenerator.writeEndObject();
    }

    private void buildHeader(JsonGenerator jsonGenerator, List<String> columns) throws IOException {
        jsonGenerator.writeFieldName("header");
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("rows");
        jsonGenerator.writeStartArray();
        buildColumns(jsonGenerator, columns);
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    private void buildBody(JsonGenerator jsonGenerator, List<List<String>> rows) throws IOException {
        jsonGenerator.writeFieldName("body");
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("rows");
        jsonGenerator.writeStartArray();
        for (List<String> row : rows) {
            buildColumns(jsonGenerator, row);
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }

    private void buildColumns(JsonGenerator jsonGenerator, List<String> columns) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeFieldName("columns");
        jsonGenerator.writeStartArray();
        for (String column : columns) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("value", column);
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
    }
}
