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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.atp.mia.TestConstants.SYS_DATE_VALUE;
import static org.qubership.atp.mia.integration.utils.TestUtils.preparePostgresServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.SqlIntegrationTestConfiguration;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;

//@Disabled("Temporarily disabled for refactoring")
public class PostgresIntegrationTest extends SqlIntegrationTestConfiguration {

    @BeforeEach
    public void cleanPostgresDbBeforeTest() {
        Server server = preparePostgresServer(postgresJdbcUrl);
        String createTable = "DROP TABLE IF EXISTS gparams;"
                + " CREATE table IF NOT EXISTS gparams(id serial, name varchar);",
                insertTable = "INSERT INTO gparams(name) VALUES ('" + SYS_DATE_VALUE + "');";
        postgreSqlDriver.get().executeUpdate(server, createTable);
        postgreSqlDriver.get().executeUpdate(server, insertTable);
    }

    @Test
    public void deleteQueryInPostgres() {
        String sqlCommand = "delete from gparams where name like '" + SYS_DATE_VALUE + "'";
        ExecutionResponse response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        assertEquals("Affected rows: 1", response.getCommandResponse().getSqlResponse().getDescription());
        //select to check db state
        sqlCommand = "SELECT * FROM gparams";
        response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks2
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        assertEquals(0, sqlResponse.getData().getData().size());
    }

    @Test
    public void insertQueryInPostgres() {
        String insertedValue = "12121212";
        String sqlCommand = "INSERT INTO gparams(name) VALUES ('" + insertedValue + "');";
        ExecutionResponse response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        assertEquals("Affected rows: 1", response.getCommandResponse().getSqlResponse().getDescription());
        //select to check db state
        sqlCommand = "SELECT * FROM gparams";
        response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks2
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        assertEquals(2, sqlResponse.getData().getData().size());
        assertEquals(2, sqlResponse.getData().getData().get(1).size());
        assertEquals("2", sqlResponse.getData().getData().get(1).get(0));
        assertEquals(insertedValue, sqlResponse.getData().getData().get(1).get(1));
    }

    @Test
    public void selectQueryInPostgres() {
        String sqlCommand = "select * from gparams where name like '" + SYS_DATE_VALUE + "'";
        executeDbQuery(postgresJdbcUrl, sqlCommand);
        ExecutionResponse response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        assertEquals(1, sqlResponse.getData().getData().size());
        assertEquals(2, sqlResponse.getData().getData().get(0).size());
        assertEquals("1", sqlResponse.getData().getData().get(0).get(0));
        assertEquals(SYS_DATE_VALUE, sqlResponse.getData().getData().get(0).get(1));
    }

    @Test
    public void updateQueryInPostgres() {
        String updatedValue = "343343343";
        String sqlCommand = "UPDATE gparams set name='" + updatedValue + "' where name like '" + SYS_DATE_VALUE + "'";
        ExecutionResponse response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        assertEquals("Affected rows: 1", response.getCommandResponse().getSqlResponse().getDescription());
        //select to check db state
        sqlCommand = "SELECT * FROM gparams";
        response = executeDbQuery(postgresJdbcUrl, sqlCommand);
        //checks2
        assertNotNull(response);
        assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        assertEquals(1, sqlResponse.getData().getData().size());
        assertEquals(2, sqlResponse.getData().getData().get(0).size());
        assertEquals("1", sqlResponse.getData().getData().get(0).get(0));
        assertEquals(updatedValue, sqlResponse.getData().getData().get(0).get(1));
    }
}
