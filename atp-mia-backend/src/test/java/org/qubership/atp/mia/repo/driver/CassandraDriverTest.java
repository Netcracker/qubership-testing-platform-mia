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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.qubership.atp.mia.TestConstants.CASSANDRA_QUERY;
import static org.qubership.atp.mia.TestConstants.JDBC_URL;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.component.QueryDriverFactory;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;

@ExtendWith(SkipTestInJenkins.class)
class CassandraDriverTest extends ConfigTestBean {

    @Test
    void execute() {
        //mock
        Server server = getServer();
        //call
        long cacheSizeBefore = cassandraDriver.get().poolSize();
        cassandraDriver.get().executeQuery(server, CASSANDRA_QUERY);
        //check
        assertEquals(cacheSizeBefore + 1, cassandraDriver.get().poolSize());
        cassandraDriver.get().executeQuery(server, CASSANDRA_QUERY);
        assertEquals(cacheSizeBefore + 1, cassandraDriver.get().poolSize());
    }

    /*@Test
    void cleanUp() throws InterruptedException {
        //mock
        cassandraDriver.set(spy(new CassandraDriver(miaContext.get(), 2000, 1800)));
        queryDriverFactory.set(new QueryDriverFactory(Arrays.asList(cassandraDriver.get(), oracleDriver.get(), postgreSqlDriver.get())));
        Server server = getServer();
        //call
        long cacheSizeBefore = cassandraDriver.get().poolSize();
        cassandraDriver.get().executeQuery(server, CASSANDRA_QUERY);
        //check
        assertEquals(cacheSizeBefore + 1, cassandraDriver.get().poolSize());
        Thread.sleep(100L);
        assertEquals(cacheSizeBefore + 1, cassandraDriver.get().poolSize());
        Thread.sleep(8000L);
        assertEquals(cacheSizeBefore, cassandraDriver.get().poolSize());
    }*/

    private Server getServer() {
        String cassandraHost = System.getProperty("CASSANDRA_IP");
        Connection connection = Connection.builder()
                .id(UUID.randomUUID())
                .name("cassandra")
                .sourceTemplateId(Connection.SourceTemplateId.DB.id)
                .systemId(UUID.randomUUID())
                .parameters(new HashMap<String, String>() {{
                    put("db_host", cassandraHost);
                    put("db_port", "9042");
                    put(JDBC_URL, "jdbc:cassandra://" + cassandraHost + ":9042/test");
                    put(SQL_LOGIN, "cassandra");
                    put(SQL_PASSWORD, "cassandra");
                }})
                .build();
        return new Server(connection, "db");
    }
}
