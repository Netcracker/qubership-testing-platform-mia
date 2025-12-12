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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import org.qubership.atp.mia.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.exceptions.AuthenticationException;
import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class CassandraDriver implements QueryDriver<Cluster> {

    public static final String CASSANDRA_DATE_TIMESTAMP = "yyyy-MM-dd HH:mm:ss";

    private final LoadingCache<Server, Cluster> pool;
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
        initPoolCleanUp(log, pool, cleanUpTimeout * 1000L);
    }

    /**
     * Constructor with parameters (test usage).
     */
    public CassandraDriver(@Autowired MiaContext miaContext, int expireAfter, int cleanUpTimeout) {
        pool = initPool(log, expireAfter);
        initPoolCleanUp(log, pool, cleanUpTimeout);
        this.miaContext = miaContext;
    }

    @Override
    public Cluster create(Server server) {
        Cluster.Builder builder = Cluster.builder()
                .addContactPoints(server.getHost())
                .withSocketOptions(new SocketOptions()
                        .setConnectTimeoutMillis(server.getTimeoutConnect(1, 60))
                        .setReadTimeoutMillis(getExecutionTimeout(executionTimeout, server)))
                .withPoolingOptions(new PoolingOptions()
                        .setConnectionsPerHost(HostDistance.LOCAL, 5, 10)
                        .setConnectionsPerHost(HostDistance.REMOTE, 2, 4)
                        .setMaxRequestsPerConnection(HostDistance.LOCAL, 10)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, 10)
                        .setHeartbeatIntervalSeconds(30)
                );
        if (!Strings.isNullOrEmpty(server.getProperty("db_port"))) {
            builder.withPort(Integer.parseInt(server.getProperty("db_port")));
        }
        if (server.getUser() != null && server.getPass() != null) {
            builder.withCredentials(server.getUser(), CryptoUtils.decryptValue(server.getPass()));
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
            Cluster cluster = pool.get(server);
            int timeout = getExecutionTimeout(executionTimeout, server);
            try {
                Session session = getSession(cluster, server);
                PreparedStatement prepare = session.prepare(query);
                BoundStatement bind = prepareStatement(prepare);
                ResultSet queryResult = session.executeAsync(bind).get(timeout, TimeUnit.MILLISECONDS);
                if (queryResult != null && metricsService != null) {
                    int size = queryResult.getAvailableWithoutFetching();
                    log.info("[SIZE] Cassandra query retrieved {} records", size);
                    metricsService.sqlQueryRecordsSize(size);
                }
                //TODO: below code implicitly relies on queryResult != null and not empty. Need to add explicit checks.
                ColumnDefinitions columnDefinitions = queryResult.getColumnDefinitions();
                int columnsSize = columnDefinitions.size();
                List<String> columnNames = Lists.newArrayListWithExpectedSize(columnsSize);
                for (int index = 0; index < columnsSize; index++) {
                    columnNames.add(columnDefinitions.getName(index));
                }
                CodecRegistry cr = session.getCluster().getConfiguration().getCodecRegistry();
                Stream<List<String>> rows = Utils.streamOf(queryResult.iterator())
                        .limit(limitRecords > 0 ? limitRecords : Long.MAX_VALUE)
                        .map(row -> {
                            List<String> result = Lists.newArrayListWithExpectedSize(columnsSize);
                            for (int i = 0; i < columnsSize; i++) {
                                result.add(getValueForType(cr, row, i, row.getColumnDefinitions().getType(i)));
                            }
                            return result;
                        });
                return new DbTable(columnNames, rows);
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
            Cluster cluster = pool.get(server);
            int timeout = getExecutionTimeout(executionTimeout, server);
            try {
                Session session = getSession(cluster, server);
                PreparedStatement prepare = session.prepare(query);
                BoundStatement bind = prepareStatement(prepare);
                ResultSet queryResult = session.executeAsync(bind).get(timeout, TimeUnit.MILLISECONDS);
                return queryResult.all().size();
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

    private BoundStatement prepareStatement(PreparedStatement prepare) {
        BoundStatement bind = prepare.bind();
        final Map<String, String> parameters = miaContext.getFlowData().getParameters();
        final Matcher matcher = Pattern.compile("(^|[\\s'\"]):(\\w+)\\b").matcher(prepare.getQueryString());
        int index = 0;
        while (matcher.find()) {
            final String keyInSqlFound = matcher.group(2);
            log.info("Parameter in sql found: {}", keyInSqlFound);
            if (parameters.containsKey(keyInSqlFound)) {
                Object value = parameters.get(keyInSqlFound);
                log.info("Replaced by: {}", value);
                TypeToken token = TypeToken.of(value.getClass());
                bind.set(index, value, token);
                index++;
            } else {
                throw new CasandraParameterNotFoundException(keyInSqlFound);
            }
        }
        return bind;
    }

    private String getValueForType(CodecRegistry cr, Row row, int columnNo, DataType colType) {
        String type = colType.getName().toString();
        if (row.getObject(columnNo) != null) {
            if (type.equals("timestamp")) {
                SimpleDateFormat formatter = new SimpleDateFormat(CASSANDRA_DATE_TIMESTAMP);
                formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                return formatter.format(row.get(columnNo, cr.codecFor(colType).getJavaType()));
            }
            if (type.equals("double")) {
                return String.valueOf(new Double(row.getDouble(columnNo)).longValue());
            }
        }
        return String.valueOf(row.getObject(columnNo));
    }

    private Session getSession(Cluster cluster, Server server) {
        return cluster.connect(server.getProperty("schema") != null
                ? server.getProperty("schema")
                : server.getProperty("db_name"));
    }
}
