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

package org.qubership.atp.mia.repo.driver;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.mia.exceptions.businesslogic.sql.CasandraParameterNotFoundException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.CassandraAuthenticationException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.CassandraDbConnectionIssueException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.CassandraPoolException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlTimeoutException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.pot.db.DbAnswer;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.auth.AuthenticationException;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CassandraDriver implements QueryDriver<CqlSession> {

    public static final String CASSANDRA_DATE_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";

    private final LoadingCache<Server, CqlSession> pool;
    private final ScheduledExecutorService cleanupScheduler;
    private final MiaContext miaContext;
    @Autowired
    private MetricsAggregateService metricsService;
    @Value("${db.close.delay:300}")
    protected int cleanUpTimeout;
    @Value("${db.alive.length:300}")
    protected int expiredAfter;
    @Value("${db.execution.timeout:30}")
    protected int executionTimeout;

    /**
     * Constructor.
     */
    @Autowired
    public CassandraDriver(MiaContext miaContext) {
        this.miaContext = miaContext;
        pool = initPool(log, expiredAfter * 1000);
        cleanupScheduler = initPoolCleanUp(log, pool, cleanUpTimeout * 1000L);
    }

    /**
     * Constructor with parameters (test usage).
     */
    public CassandraDriver(@Autowired MiaContext miaContext, int expireAfter, int cleanUpTimeout) {
        pool = initPool(log, expireAfter);
        cleanupScheduler = initPoolCleanUp(log, pool, cleanUpTimeout);
        this.miaContext = miaContext;
    }

    @Override
    public CqlSession create(Server server) {
        CqlSessionBuilder builder = CqlSession.builder();

        // Determine port - configured or default (9042 for Cassandra)
        String portString = server.getProperties().getOrDefault("db_port", "9042");
        int port = StringUtils.isEmpty(portString) ? 9042 : Integer.parseInt(portString);

        builder.addContactPoint(new InetSocketAddress(server.getHost(), port));
        if (server.getUser() != null && server.getPass() != null) {
            builder.withAuthCredentials(server.getUser(), CryptoUtils.decryptValue(server.getPass()));
        }

        // Check configured keyspace (schema/db_name)
        String keyspace = server.getProperty("schema") != null
                ? server.getProperty("schema")
                : server.getProperty("db_name");

        if (keyspace != null && !keyspace.isEmpty()) {
            builder.withKeyspace(keyspace);
            log.info("Using keyspace: {}", keyspace);
        }

        DriverConfigLoader driverConfigLoader = DriverConfigLoader.programmaticBuilder()
                .withDuration(
                        DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT,
                        Duration.ofSeconds(server.getTimeoutConnect(1, 60))
                )
                .withDuration(
                        DefaultDriverOption.REQUEST_TIMEOUT,
                        Duration.ofMillis(getExecutionTimeout(executionTimeout, server))
                )
                .withInt(
                        DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE,
                        5
                )
                .withInt(
                        DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE,
                        2
                ).withInt(
                        DefaultDriverOption.CONNECTION_MAX_REQUESTS,
                        10
                )
                .withDuration(
                        DefaultDriverOption.HEARTBEAT_INTERVAL,
                        Duration.ofSeconds(30)
                )
                .build();

        builder.withConfigLoader(driverConfigLoader);

        // Set Datacenter (if configured)
        String dataCenter = server.getProperty("local_datacenter");
        if (!StringUtils.isEmpty(dataCenter)) {
            builder.withLocalDatacenter(dataCenter);
        }
        return builder.build();
    }

    @Override
    public DbTable executeQuery(Server server, String query) {
        return executeQuery(server, query, 0);
    }

    @Override
    public DbTable executeQuery(Server server, String query, int limitRecords) {
        try {
            log.info("Execute query: get Cassandra connection for {}", server);
            CqlSession session = pool.get(server);
            int timeout = getExecutionTimeout(executionTimeout, server);
            try {
                PreparedStatement prepare = session.prepare(query);
                BoundStatement bind = prepareStatement(prepare, session);

                // Async execution with timeout
                CompletionStage<AsyncResultSet> future = session.executeAsync(bind);
                AsyncResultSet queryResult = future.toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);

                //TODO: below code implicitly relies on queryResult != null and not empty. Need to add explicit checks.
                ColumnDefinitions columnDefinitions = queryResult.getColumnDefinitions();
                int columnsSize = columnDefinitions.size();
                List<String> columnNames = Lists.newArrayListWithExpectedSize(columnsSize);
                for (int index = 0; index < columnsSize; index++) {
                    columnNames.add(columnDefinitions.get(index).getName().asInternal());
                }
                CodecRegistry cr = session.getContext().getCodecRegistry();

                List<List<String>> allRows = new ArrayList<>();
                long checkedLimitRecords = limitRecords > 0 ? limitRecords : Long.MAX_VALUE;
                int fetched = 0;
                do {
                    for (Row row : queryResult.currentPage()) {
                        if (fetched >= checkedLimitRecords) break;
                        List<String> result = Lists.newArrayListWithExpectedSize(columnsSize);
                        for (int i = 0; i < columnsSize; i++) {
                            result.add(getValueForType(cr, row, i, columnDefinitions.get(i).getType()));
                        }
                        allRows.add(result);
                        fetched++;
                    }

                    // Переходим к следующей странице, если нужно
                    if (fetched < limitRecords && queryResult.hasMorePages()) {
                        queryResult = queryResult
                                .fetchNextPage()
                                .toCompletableFuture()
                                .get(timeout, TimeUnit.MILLISECONDS);
                    } else {
                        break;
                    }
                } while (queryResult.hasMorePages() && fetched < checkedLimitRecords);

                if (fetched > 0 && metricsService != null) {
                    log.info("[SIZE] Cassandra query retrieved {} records", fetched);
                    metricsService.sqlQueryRecordsSize(fetched);
                }
                return new DbTable(columnNames, allRows.stream());
            } catch (TimeoutException e) {
                throw new SqlTimeoutException(timeout, "seconds", query);
            } catch (AuthenticationException e) {
                throw new CassandraAuthenticationException(e.getMessage());
            } catch (Exception e) {
                throw new CassandraDbConnectionIssueException(e.getMessage());
            }
        } catch (Exception e) {
            throw new CassandraPoolException(e);
        }
    }

    @Override
    public int executeUpdate(Server server, String query) {
        try {
            log.info("Execute update: Get Cassandra connection for {}", server);
            CqlSession session = pool.get(server);
            int timeout = getExecutionTimeout(executionTimeout, server);
            try {
                PreparedStatement prepare = session.prepare(query);
                BoundStatement bind = prepareStatement(prepare, session);

                // Async execution with timeout
                CompletionStage<AsyncResultSet> future = session.executeAsync(bind);
                AsyncResultSet queryResult = future.toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
                return (int) queryResult.currentPage().spliterator().estimateSize(); // TODO Need to check.
            } catch (TimeoutException e) {
                throw new SqlTimeoutException(timeout, "seconds", query);
            } catch (AuthenticationException e) {
                throw new CassandraAuthenticationException(e.getMessage());
            } catch (Exception e) {
                throw new CassandraDbConnectionIssueException(e.getMessage());
            }
        } catch (Exception e) {
            throw new CassandraPoolException(e);
        }
    }

    @Override
    public DbAnswer executeStoredProcedure(Server server, String query) {
        return new DbAnswer(true, executeUpdate(server, query));
    }

    @Override
    public String getDriverType() {
        return "cassandra";
    }

    @Override
    public long poolSize() {
        return pool.size();
    }

    private BoundStatement prepareStatement(PreparedStatement prepare, CqlSession session) {
        BoundStatement bind = prepare.bind();
        final Map<String, String> parameters = miaContext.getFlowData().getParameters();
        final Matcher matcher = Pattern.compile("(^|[\\s'\"]):(\\w+)\\b").matcher(prepare.getQuery());
        int index = 0;

        // Get variables metadata
        ColumnDefinitions variables = prepare.getVariableDefinitions();

        // Get CodecRegistry via session
        CodecRegistry registry = session.getContext().getCodecRegistry();

        while (matcher.find()) {
            final String keyInSqlFound = matcher.group(2);
            log.info("Parameter in sql found: {}", keyInSqlFound);
            if (parameters.containsKey(keyInSqlFound)) {
                String value = parameters.get(keyInSqlFound);
                log.info("Replaced by: {}", value);

                // Get expected parameter type from the prepared statement variables metadata
                DataType expectedType = variables.get(index).getType();

                // Get codec for the expected type
                TypeCodec<Object> codec = registry.codecFor(expectedType);

                // Parse String parameter value into object of the expected type
                Object convertedValue = codec.parse(value);

                // Bind parameter value.
                // Note: in the modern API, the result should be explicitly set back to 'bind' object.
                bind = bind.set(index, convertedValue, codec);
                index++;
            } else {
                throw new CasandraParameterNotFoundException(keyInSqlFound);
            }
        }
        return bind;
    }

    private String getValueForType(CodecRegistry cr, Row row, int columnNo, DataType colType) {
        String type = colType.toString();
        if (row.getObject(columnNo) != null) {
            if (type.equals("timestamp")) {
                SimpleDateFormat formatter = new SimpleDateFormat(CASSANDRA_DATE_TIMESTAMP);
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                return formatter.format(row.get(columnNo, cr.codecFor(colType).getJavaType()));
            } else if (type.equals("double")) {
                return String.valueOf(Double.valueOf(row.getDouble(columnNo)).longValue());
            }
        }
        return String.valueOf(row.getObject(columnNo));
    }

    @Override
    public void shutdown() {
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            log.info("{}: pool shutdown; shutting down the cleanupScheduler too..", Thread.currentThread().getName());
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("{}: cleanupScheduler shutdown completed.", Thread.currentThread().getName());
        }
    }
}
