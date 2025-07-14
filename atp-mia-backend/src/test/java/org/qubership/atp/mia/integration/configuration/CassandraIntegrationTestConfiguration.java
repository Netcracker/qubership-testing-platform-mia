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

package org.qubership.atp.mia.integration.configuration;

import static org.qubership.atp.mia.TestConstants.CASSANDRA_DB_TYPE;
import static org.qubership.atp.mia.TestConstants.CASSANDRA_DB_VALUE;
import static org.qubership.atp.mia.TestConstants.CASSANDRA_HOST;
import static org.qubership.atp.mia.TestConstants.CASSANDRA_PORT;
import static org.qubership.atp.mia.TestConstants.CASSANDRA_SCHEMA;
import static org.qubership.atp.mia.TestConstants.SCHEMA;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN_VALUE;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD_VALUE;
import static org.qubership.atp.mia.TestConstants.SYS_DATE_VALUE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CassandraIntegrationTestConfiguration extends SqlIntegrationTestConfiguration {

    private static String cassandraPort;
    protected static String cassandraUrl;

    private final String createKeySpace = "CREATE KEYSPACE IF NOT EXISTS " + CASSANDRA_SCHEMA +
            " WITH replication = \n{'class':'SimpleStrategy','replication_factor':'1'};";
    private final String dropTable = "DROP TABLE IF EXISTS gparams;";
    private final String createTable = "create table gparams(id text, name text, PRIMARY key(id))";
    private final String insertTable = "INSERT INTO gparams(id, name) VALUES ('1', '" + SYS_DATE_VALUE + "');";

    private final int retryAmount = 3;
    private final int sleepTime = 5000;

    private static Map<String, String> initDbParameters;

    @AfterEach
    public void afterTest() {
        testConnectionDb.get().setParameters(initDbParameters);
    }

    @BeforeEach
    public void prepareCassandraBeforeTest() throws InterruptedException {
        cassandraUrl = cassandraContainerEnvironment.getCassandraAddress();
        cassandraPort = cassandraContainerEnvironment.getCassandraPort();
        cleanCassandraDbBeforeTest();
        initDbParameters = testConnectionDb.get().getParameters();
        testConnectionDb.get().setParameters(
                new HashMap<String, String>() {{
                    put(CASSANDRA_HOST, cassandraUrl);
                    put(CASSANDRA_PORT, cassandraPort);
                    put(SQL_LOGIN, SQL_LOGIN_VALUE);
                    put(SQL_PASSWORD, SQL_PASSWORD_VALUE);
                    put(CASSANDRA_DB_TYPE, CASSANDRA_DB_VALUE);
                    put(SCHEMA, CASSANDRA_SCHEMA);
                }}
        );
    }

    private void cleanCassandraDbBeforeTest() throws InterruptedException {
        int i = 0;
        try (final Cluster cluster = getCluster()) {
            while (!runCleanCassandraCommand(cluster) && i++ < retryAmount) {
                Thread.sleep(sleepTime);
                if (i++ == retryAmount) {
                    Assert.fail("Retried " + retryAmount + " times and can't start cassandra");
                }
            }
        }
    }

    private Cluster getCluster() {
        return Cluster.builder()
                .addContactPoint(cassandraUrl)
                .withPort(Integer.parseInt(cassandraPort))
                .build();
    }

    private boolean runCleanCassandraCommand(Cluster cluster) {
        boolean result = true;
        try (Session session = cluster.connect()) {
            session.execute(createKeySpace);
            try (Session sessionWithKeyspace = cluster.connect(CASSANDRA_SCHEMA)) {
                sessionWithKeyspace.execute(dropTable);
                sessionWithKeyspace.execute(createTable);
                sessionWithKeyspace.execute(insertTable);
            }
        } catch (Exception e) {
            log.error("Cassandra clean error:", e);
            result = false;
        }
        return result;
    }
}
