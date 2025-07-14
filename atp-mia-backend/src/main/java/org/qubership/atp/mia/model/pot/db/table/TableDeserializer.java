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
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("it")
public class TableDeserializer extends JsonDeserializer<DbTable> {

    /**
     * For integration test purposes when we receive response from endpoint, this method works.
     *
     * @param jp   parser
     * @param ctxt context
     * @return DbTable object, if exception occurred DbTable with null fields.
     */
    @Override
    public DbTable deserialize(JsonParser jp, DeserializationContext ctxt) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectCodec oc = jp.getCodec();
            JsonNode node = oc.readTree(jp);
            List<String> columns = new ArrayList<>(); // header
            // TODO: fix get(0)
            ArrayList list = objectMapper.convertValue(node.get("header").get("rows").get(0).get("columns"),
                    ArrayList.class);
            for (Object obj : list) {
                columns.add(objectMapper.convertValue(obj, Map.class).get("value").toString());
            }
            List<List<String>> data = new ArrayList<>(); // body
            list = objectMapper.convertValue(node.get("body").get("rows"), ArrayList.class);
            for (Object obj : list) {
                List<String> insideData = new ArrayList<>();
                ArrayList listForInside = objectMapper.convertValue(
                        objectMapper.convertValue(obj, Map.class).get("columns"), ArrayList.class);
                for (Object insObj : listForInside) {
                    insideData.add(objectMapper.convertValue(insObj, Map.class).get("value").toString());
                }
                data.add(insideData);
            }
            return new DbTable(columns, data);
        } catch (Exception e) {
            log.error("Can't deserialize object [{}]", e.getMessage());
            return new DbTable();
        }
    }
}
