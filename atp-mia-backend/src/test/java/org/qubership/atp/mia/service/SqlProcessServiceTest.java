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

package org.qubership.atp.mia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.integration.utils.TestUtils.preparePostgresServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlConnectionFailException;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;

import com.google.common.collect.ImmutableList;

@ExtendWith(SkipTestInJenkins.class)
public class SqlProcessServiceTest extends ConfigTestBean {

    private final String systemName = "Billing System";
    private final String command = "command";
    private final ThreadLocal<SqlExecutionHelperService> sqlExecutionHelperService = new ThreadLocal<>();
    private final ThreadLocal<Server> server = new ThreadLocal<>();

    @BeforeEach
    public void beforeSqlExecutionHelperServiceTest() {
        server.set(preparePostgresServer("jdbc:postgresql://" + System.getProperty("POSTGRES_IP") + ":5432/mia"));
        sqlExecutionHelperService.set(spy(new SqlExecutionHelperService(
                miaContext.get(),
                miaFileService.get(),
                queryDriverFactory.get(),
                "")));
    }

    @Test
    public void executeCommand_whenSingleQuerySelect() {
        DbTable mockTable = new DbTable(new ArrayList<String>(), new ArrayList<List<String>>());
        doReturn(mockTable).when(postgreSqlDriver.get()).executeQuery(any(), any(), anyInt());
        String query = "select :param1 from table where :param2";
        String evaluatedQuery = "select value1 from table where value2";
        miaContext.get().getFlowData().addParameter("param1", "value1");
        miaContext.get().getFlowData().addParameter("param2", "value2");
        sqlExecutionHelperService.get().executeCommand(query, systemName);
        Mockito.verify(sqlExecutionHelperService.get(), times(1)).executeCommand(eq(query), eq(systemName));
        Mockito.verify(postgreSqlDriver.get(), times(1)).executeQuery(any(Server.class), eq(evaluatedQuery), anyInt());
    }

    @Test
    public void executeValidations_whenNoRefer_thenExecute() {
        final SqlResponse sqlResponse = new SqlResponse();
        assertEquals(ImmutableList.of(sqlResponse), executeValidation(null, sqlResponse));
    }

    @Test
    public void executeValidations_whenNoRelatedCommand_thenSkip() {
        final SqlResponse sqlResponse = new SqlResponse();
        assertTrue(executeValidation(ImmutableList.of("another " + command), sqlResponse).isEmpty());
    }

    @Test
    public void executeValidations_whenReferAny_thenExecute() {
        final SqlResponse sqlResponse = new SqlResponse();
        assertEquals(ImmutableList.of(sqlResponse), executeValidation(ImmutableList.of("aNy"), sqlResponse));
    }

    @Test
    public void executeValidations_whenRelatedCommand_thenExecute() {
        final SqlResponse sqlResponse = new SqlResponse();
        assertEquals(ImmutableList.of(sqlResponse), executeValidation(ImmutableList.of("another " + command,
                command), sqlResponse));
    }

    @Test
    public void executeQueries_whenFile_thenExecuteQueriesSeveralTimes() {
        CommandResponse commandResponse = new CommandResponse();
        doReturn(Arrays.asList(commandResponse)).when(sqlExecutionHelperService.get()).executeCommand(anyString(), anyString());
        doReturn(1).when(postgreSqlDriver.get()).executeUpdate(any(), any());
        miaContext.get().getFlowData().addParameter("toEvaluate", "evaluated");
        String sqlFile = "../../../../../test/config/SqlExecutionHelperServiceTest.sql";
        sqlExecutionHelperService.get().executeCommand(sqlFile, systemName);
        Mockito.verify(sqlExecutionHelperService.get(), times(1)).executeCommand(eq(sqlFile), eq(systemName));
        assertEquals(1, postgreSqlDriver.get().executeUpdate(any(Server.class), eq("update table1")));
        assertEquals(1, postgreSqlDriver.get().executeUpdate(any(Server.class), eq("update table2")));
        assertEquals(1, postgreSqlDriver.get().executeUpdate(any(Server.class), eq("UpDate evaluated")));
    }

    /**
     * The test checks the correctness of the method depending on the database specified in the environment.
     */
    @Test
    public void handleSingleQuery_checkCassandra() {
        server.set(preparePostgresServer("jdbc:cassandra://" + System.getProperty("CASSANDRA_IP") + ":5432/mia"));
        String query = "select * from anydataBase";
        DbTable mockTable = new DbTable(new ArrayList<>(), new ArrayList<>());
        doReturn(mockTable).when(cassandraDriver.get()).executeQuery(eq(server.get()), eq(query), anyInt());
        sqlExecutionHelperService.get().handleSingleQuery(query, server.get(), false);
        Mockito.verify(cassandraDriver.get(), times(1)).executeQuery(eq(server.get()), eq(query), anyInt());
    }

    /**
     * The test checks the correctness of the method in cases where the database is defined as Oracle.
     */
    @Test
    public void handleSingleQuery_updateWithOracle() {
        String query = "update * from anYdatabase";
        Server server = preparePostgresServer("jdbc:oracle://localhost:5432/mia");
        doReturn(1).when(oracleDriver.get()).executeUpdate(any(), any());
        List<SqlResponse> sqlResponse = sqlExecutionHelperService.get().handleSingleQuery(query, server, false);
        assertEquals("Affected rows: 1", sqlResponse.get(0).getDescription());
    }

    @Test
    public void handleSingleQuery_whenSingleQueryUpdate() {
        String query = "insert into TABLE (name, ':param1', :param2, null)";
        String evaluatedQuery = "insert into TABLE (name, 'value1', value2, null)";
        miaContext.get().getFlowData().addParameter("param1", "value1");
        miaContext.get().getFlowData().addParameter("param2", "value2");
        doReturn(1).when(postgreSqlDriver.get()).executeUpdate(any(), any());
        sqlExecutionHelperService.get().executeCommand(query, systemName);
        Mockito.verify(sqlExecutionHelperService.get(), times(1)).executeCommand(eq(query), eq(systemName));
        postgreSqlDriver.get().executeUpdate(any(Server.class), eq(evaluatedQuery));
    }

    /**
     * The test checks the correctness of the method in cases where the database is not defined(Actually it is the
     * same as oracle pre-defined db.
     */
    @Test
    public void handleSingleQuery_whenUndefinedDbType_thenThrow() {
        String query = "select * from anyDatabase";
        server.set(spy(new Server(Connection.builder().build(), "db")));
        String dbType = "wrongWrittenBDOrNonSupportedDB";
        when(server.get().getProperty(anyString())).thenReturn(dbType);
        try {
            sqlExecutionHelperService.get().handleSingleQuery(query, server.get(), false);
            fail("Must be thrown");
        } catch (RuntimeException e) {
            String expected = ErrorCodes.MIA_1309_SQL_LOAD_DRIVER_FAIL.getMessage(dbType);
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void handleSingleQuery_whenConnectionError_thenThrow() {
        server.set(preparePostgresServer("jdbc:postgresql://" + System.getProperty("POSTGRES_IP") + ":5499/mia"));
        String query = "select :param1 from table where :param2";
        assertThrows(
                SqlConnectionFailException.class,
                () -> sqlExecutionHelperService.get().handleSingleQuery(query, server.get(), false)
        );
    }

    private List<SqlResponse> executeValidation(List<String> referToCommandExecution, SqlResponse sqlResponse) {
        final Validation validation = new Validation("SQL", systemName, "select ")
                .setTableName("AccountDetails")
                .setReferToCommandExecution(referToCommandExecution);
        sqlResponse.setDescription("Description");
        final List<Validation> validations = Arrays.asList(validation);
        String commandToExecute = "prerequisites;" + command;
        final Command command = new Command().setToExecute(commandToExecute);
        doReturn(sqlResponse).when(postgreSqlDriver.get()).executeQuery(any(Server.class), anyString(), anyString(),
                eq(true), eq(false), anyInt());
        return sqlExecutionHelperService.get().executeValidations(validations, command);
    }
}
