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

public enum DbType {
    CASSANDRA,
    ORACLE,
    POSTGRESQL,
    UNDEFINED;

    private static Map<DbType, String> driverNames = new HashMap<DbType, String>() {
        {
            put(CASSANDRA, "org.apache.cassandra.cql.jdbc.CassandraDriver");
            put(ORACLE, "oracle.jdbc.driver.OracleDriver");
            put(POSTGRESQL, "org.postgresql.Driver");
            put(UNDEFINED, "com.mysql.jdbc.Driver");
        }
    };

    public static String getDriverName(DbType dbType) {
        return driverNames.get(dbType);
    }
}
