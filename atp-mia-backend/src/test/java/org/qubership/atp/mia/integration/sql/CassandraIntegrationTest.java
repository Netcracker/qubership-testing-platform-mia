/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import static org.qubership.atp.mia.TestConstants.SYS_DATE_VALUE;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.CassandraIntegrationTestConfiguration;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;

//@Disabled("Temporarily disabled for refactoring")
public class CassandraIntegrationTest extends CassandraIntegrationTestConfiguration {

    @Test
    public void deleteQueryInCassandra() {
        String sqlCommand = "delete from gparams where id = '1'";
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getProcessStatus());
        Assertions.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        // select to check db state
        sqlCommand = "select * from gparams";
        response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks2
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getProcessStatus());
        Assertions.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assertions.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assertions.assertEquals(0, sqlResponse.getData().getData().size());
    }

    @Test
    public void insertQueryWithVariableInCassandra() {
        String expectedValue = String.valueOf(UUID.randomUUID()),
                sqlCommand = "INSERT INTO gparams(id, name) VALUES (':idParameter', '" + expectedValue + "');";
        miaContext.getFlowData().addParameter("idParameter", "2");
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getProcessStatus());
        Assertions.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        // select to check db state
        sqlCommand = "select * from gparams";
        response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks2
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getProcessStatus());
        Assertions.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assertions.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assertions.assertEquals(2, sqlResponse.getData().getData().size());
        Assertions.assertEquals(2, sqlResponse.getData().getData().getFirst().size());
        Assertions.assertEquals("2", sqlResponse.getData().getData().getFirst().get(0));
        Assertions.assertEquals(expectedValue, sqlResponse.getData().getData().getFirst().get(1));
    }

    @Test
    public void selectQueryInCassandra() {
        String sqlCommand = "select * from gparams where id = '1'";
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        //checks
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assertions.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assertions.assertEquals(1, sqlResponse.getData().getData().size());
        Assertions.assertEquals(2, sqlResponse.getData().getData().getFirst().size());
        Assertions.assertEquals("1", sqlResponse.getData().getData().getFirst().get(0));
        Assertions.assertEquals(SYS_DATE_VALUE, sqlResponse.getData().getData().getFirst().get(1));
    }

    @Test
    public void updateQueryInCassandra() {
        String updatedValue = String.valueOf(UUID.randomUUID()),
                sqlCommand = "UPDATE gparams set name='" + updatedValue + "' where id = '1'";
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getProcessStatus());
        Assertions.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        // select to check db state
        sqlCommand = "select * from gparams";
        response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks2
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getProcessStatus());
        Assertions.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assertions.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assertions.assertEquals(1, sqlResponse.getData().getData().size());
        Assertions.assertEquals(2, sqlResponse.getData().getData().getFirst().size());
        Assertions.assertEquals("1", sqlResponse.getData().getData().getFirst().get(0));
        Assertions.assertEquals(updatedValue, sqlResponse.getData().getData().getFirst().get(1));
    }
}
