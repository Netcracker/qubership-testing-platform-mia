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

package org.qubership.atp.mia.model.pot.db;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;
import org.qubership.atp.mia.service.MiaContext;

import lombok.Data;

@Data
public class SqlResponse {

    private DbTable data;
    private String tableName;
    private String query;
    private String description;
    private int records;
    private String limitRecordsMessage;
    @Nullable
    private TableMarkerResult tableMarkerResult;
    @Nullable
    private String internalPathToFile;
    private Link link;
    private boolean saveToWordFile = true;
    private boolean saveToZipFile = false;
    private Map<String, String> connectionInfo;

    public SqlResponse() {
    }

    public SqlResponse(Server server) {
        addConnectionInfo(server);
    }

    @Nullable
    public TableMarkerResult getTableMarkerResult() {
        return tableMarkerResult;
    }

    public void setTableMarkerResult(@Nullable TableMarkerResult tableMarkerResult) {
        this.tableMarkerResult = tableMarkerResult;
    }

    @Nullable
    public String getInternalPathToFile() {
        return internalPathToFile;
    }

    /**
     * Sets internalPathToFile.
     *
     * @param internalPathToFile internalPathToFile
     */
    public void setInternalPathToFile(@Nullable String internalPathToFile, MiaContext miaContext) {
        this.internalPathToFile = internalPathToFile;
        if (internalPathToFile != null) {
            this.link = miaContext.getLogLinkOnUi(internalPathToFile);
        }
    }

    /**
     * Add connectionInfo.
     *
     * @param server Server
     */
    public void addConnectionInfo(Server server) {
        if (server != null) {
            if (this.connectionInfo == null) {
                this.connectionInfo = new HashMap<>();
            }
            this.connectionInfo.put("connection string", server.getProperty("jdbc_url"));
            this.connectionInfo.put("user", server.getUser());
        }
    }
}
