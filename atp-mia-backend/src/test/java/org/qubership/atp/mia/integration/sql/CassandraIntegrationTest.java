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

import static org.qubership.atp.mia.TestConstants.SYS_DATE_VALUE;

import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
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
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        // select to check db state
        sqlCommand = "select * from gparams";
        response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks2
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assert.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assert.assertEquals(0, sqlResponse.getData().getData().size());
    }

    @Test
    public void insertQueryWithVariableInCassandra() {
        String expectedValue = String.valueOf(UUID.randomUUID()),
                sqlCommand = "INSERT INTO gparams(id, name) VALUES (':idParameter', '" + expectedValue + "');";
        miaContext.getFlowData().addParameter("idParameter", "2");
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        // select to check db state
        sqlCommand = "select * from gparams";
        response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks2
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assert.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assert.assertEquals(2, sqlResponse.getData().getData().size());
        Assert.assertEquals(2, sqlResponse.getData().getData().get(0).size());
        Assert.assertEquals("2", sqlResponse.getData().getData().get(0).get(0));
        Assert.assertEquals(expectedValue, sqlResponse.getData().getData().get(0).get(1));
    }

    @Test
    public void selectQueryInCassandra() {
        String sqlCommand = "select * from gparams where id = '1'";
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        //checks
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assert.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assert.assertEquals(1, sqlResponse.getData().getData().size());
        Assert.assertEquals(2, sqlResponse.getData().getData().get(0).size());
        Assert.assertEquals("1", sqlResponse.getData().getData().get(0).get(0));
        Assert.assertEquals(SYS_DATE_VALUE, sqlResponse.getData().getData().get(0).get(1));
    }

    @Test
    public void updateQueryInCassandra() {
        String updatedValue = String.valueOf(UUID.randomUUID()),
                sqlCommand = "UPDATE gparams set name='" + updatedValue + "' where id = '1'";
        ExecutionResponse response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        // select to check db state
        sqlCommand = "select * from gparams";
        response = executeDbQuery(cassandraUrl, sqlCommand);
        // checks2
        Assert.assertNotNull(response);
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
        SqlResponse sqlResponse = response.getCommandResponse().getSqlResponse();
        Assert.assertNotNull(sqlResponse);
        checkColumnNames(sqlResponse.getData().getColumns());
        Assert.assertEquals(1, sqlResponse.getData().getData().size());
        Assert.assertEquals(2, sqlResponse.getData().getData().get(0).size());
        Assert.assertEquals("1", sqlResponse.getData().getData().get(0).get(0));
        Assert.assertEquals(updatedValue, sqlResponse.getData().getData().get(0).get(1));
    }
}
