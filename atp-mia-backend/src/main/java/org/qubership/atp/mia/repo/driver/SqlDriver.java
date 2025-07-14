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

package org.qubership.atp.mia.repo.driver;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*import org.owasp.esapi.Encoder;
import org.owasp.esapi.codecs.OracleCodec;
import org.owasp.esapi.reference.DefaultEncoder;*/
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlConnectionFailException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlExecuteFailException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlTimeoutException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.pot.db.DbAnswer;
import org.qubership.atp.mia.model.pot.db.SqlUtils;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public abstract class SqlDriver implements QueryDriver<Connection> {

    protected final LoadingCache<Server, Connection> pool;
    protected final ExecutorService executorService;
    @Autowired
    protected MetricsAggregateService metricsService;
    @Value("${db.close.delay:300}")
    protected int cleanUpTimeout;
    @Value("${db.alive.length:300}")
    protected int expiredAfter;
    @Value("${db.execution.timeout:30}")
    protected int executionTimeout;

    //private final Encoder esapiEncoder = DefaultEncoder.getInstance();
    //private final OracleCodec oracleCodec = new OracleCodec();

    /**
     * Constructor.
     */
    protected SqlDriver(ExecutorService executorService) {
        this.executorService = executorService;
        pool = initPool(log, expiredAfter * 1000);
        initPoolCleanUp(log, pool, cleanUpTimeout * 1000L);
    }

    /**
     * Constructor with parameters (test usage).
     */
    protected SqlDriver(ExecutorService executorService, int expireAfter, int cleanUpTimeout) {
        this.executorService = executorService;
        pool = initPool(log, expireAfter);
        initPoolCleanUp(log, pool, cleanUpTimeout);
    }

    /**
     * Creates connection.
     *
     * @param server server
     * @return Connection to DB
     * @throws UncheckedExecutionException which explicitly wraps {@link SqlConnectionFailException},
     *                                     which wraps {@link SQLException} with connection error.
     */
    @Override
    public Connection create(Server server) {
        try {
            return DriverManager.getConnection(server.getProperty("jdbc_url"), server.getUser(),
                    CryptoUtils.decryptValue(server.getPass()));
        } catch (SQLException e) {
            throw new SqlConnectionFailException(server.getProperty("jdbc_url"), e);
        }
    }

    @Override
    public DbTable executeQuery(Server server, String query) {
        return executeQuery(server, query, 0);
    }

    @Override
    public DbTable executeQuery(Server server, String query, int limitRecords) {
        int timeout = getExecutionTimeout(executionTimeout, server);
        try {
            Connection connection = pool.get(server);
            try (PreparedStatement statement = connection.prepareStatement(query,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY)) {
                Map<String, String> mdcMap = MDC.getCopyOfContextMap();
                ResultSet rs = executorService.submit(() -> {
                            setThreadName(server, "Query");
                            MdcUtils.setContextMap(mdcMap);
                            return statement.executeQuery();
                        })
                        .get(timeout, TimeUnit.MILLISECONDS);
                int actualRecordsSize = 0;
                if (metricsService != null && rs != null && rs.last()) {
                    actualRecordsSize = rs.getRow();
                    log.info("[SIZE] SQL query retrieved {} records", actualRecordsSize);
                    metricsService.sqlQueryRecordsSize(actualRecordsSize);
                    rs.beforeFirst();
                }
                DbTable dbTable = SqlUtils.resultSetToDbTable(rs, limitRecords);
                dbTable.setActualDataSizeBeforeLimit(actualRecordsSize);
                return dbTable;
            } catch (TimeoutException e) {
                throw new SqlTimeoutException(timeout, "milliseconds", query);
            } catch (Exception e) {
                throw new SqlExecuteFailException(query, e);
            }
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw handleConnectionException(query, e);
        }
    }


    @Override
    public int executeUpdate(Server server, String query) {
        int timeout = getExecutionTimeout(executionTimeout, server);
        try {
            Connection connection = pool.get(server);
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                Map<String, String> mdcMap = MDC.getCopyOfContextMap();
                return executorService.submit(() -> {
                            setThreadName(server, "Update");
                            MdcUtils.setContextMap(mdcMap);
                            return statement.executeUpdate();
                        })
                        .get(timeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new SqlTimeoutException(timeout, "milliseconds", query);
            } catch (Exception e) {
                throw new SqlExecuteFailException(query, e);
            }
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw handleConnectionException(query, e);
        }
    }

    @Override
    public DbAnswer executeStoredProcedure(Server server, String query) {
        int timeout = getExecutionTimeout(executionTimeout, server);
        //String safeQuery = esapiEncoder.encodeForSQL(new OracleCodec(), query);
        try {
            Connection connection = pool.get(server);
            try (CallableStatement statement = connection.prepareCall(query)) {
                Map<String, String> mdcMap = MDC.getCopyOfContextMap();
                boolean status = executorService.submit(() -> {
                            setThreadName(server, "StoredProcedure");
                            MdcUtils.setContextMap(mdcMap);
                            return statement.execute();
                        })
                        .get(timeout, TimeUnit.MILLISECONDS);
                return new DbAnswer(status, statement);
            } catch (TimeoutException e) {
                throw new SqlTimeoutException(timeout, "milliseconds", query);
            } catch (Exception e) {
                throw new SqlExecuteFailException(query, e);
            }
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw handleConnectionException(query, e);
        }
    }


    @Override
    public long poolSize() {
        return pool.size();
    }

    private void setThreadName(Server server, String postfix) {
        Thread.currentThread().setName("mia_" + getDriverType() + "_execute" + postfix + "_" + server.getHostFull());
    }

    private MiaException handleConnectionException(String query, Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof SqlConnectionFailException) {
            return (SqlConnectionFailException) cause;
        }
        return new SqlExecuteFailException(query, e);
    }
}
