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

package org.qubership.atp.mia.integration.sql;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.integration.utils.TestUtils;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.pot.db.table.DbTable;

//@Disabled("Temporarily disabled for refactoring")
public class PostgresConnectionTest extends BaseIntegrationTestConfiguration {

    @Test
    public void executeSqlRequestTest() {
        Server dbServer = TestUtils.preparePostgresServer(postgresJdbcUrl);
        String createTable = "DROP TABLE IF EXISTS gparams;"
                + " CREATE table IF NOT EXISTS MIA_TABLE(id serial, name varchar)",
                insertTable = "INSERT INTO MIA_TABLE (name) VALUES ('some_name');",
                selectTable = "SELECT * FROM MIA_TABLE;";
        Assert.assertEquals(0, queryDriverFactory.get().getDriver(dbServer).executeUpdate(dbServer, createTable));
        Assert.assertEquals(1, queryDriverFactory.get().getDriver(dbServer).executeUpdate(dbServer, insertTable));
        DbTable dbTable = queryDriverFactory.get().getDriver(dbServer).executeQuery(dbServer, selectTable);
        Assert.assertNotNull(dbTable);
        Assert.assertEquals(2, dbTable.getColumns().size());
    }
}
