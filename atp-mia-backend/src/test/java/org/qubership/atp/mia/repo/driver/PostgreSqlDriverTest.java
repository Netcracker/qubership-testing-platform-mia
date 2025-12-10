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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.qubership.atp.mia.TestConstants.POSTGRESQL_QUERY;

import java.util.Arrays;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.component.QueryDriverFactory;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlTimeoutException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ImmutableList;

@ExtendWith(SkipTestInJenkins.class)
public class PostgreSqlDriverTest extends ConfigTestBean {

    private final ThreadLocal<Server> server = new ThreadLocal<>();

    @BeforeEach
    public void beforePostgreSqlDriverTest() {
        server.set(new Server(testConnectionDb.get(), "db"));
    }

    @Test
    public void executeQueryAndGetFirstValue_whenValuePresent() {
        final String query = "select account_num from dbTable";
        final String firstValue = "123456";
        final DbTable dbTable = new DbTable(ImmutableList.of("account_num", "bill_seq"),
                ImmutableList.of(ImmutableList.of(firstValue, "1")));
        doReturn(dbTable).when(postgreSqlDriver.get()).executeQuery(eq(server.get()), eq(query), eq(0));
        String actualValue = postgreSqlDriver.get().executeQueryAndGetFirstValue(server.get(), query);
        assertEquals(firstValue, actualValue);
    }

    @Test
    void cleanUp() throws InterruptedException {
        //mock
        postgreSqlDriver.set(spy(new PostgreSqlDriver(miaConfiguration.executorServiceForSql(0, 2, 1500), 3000, 1800)));
        queryDriverFactory.set(new QueryDriverFactory(Arrays.asList(cassandraDriver.get(), oracleDriver.get(), postgreSqlDriver.get())));
        //call
        long cacheSizeBefore = postgreSqlDriver.get().poolSize();
        postgreSqlDriver.get().executeQuery(server.get(), POSTGRESQL_QUERY);
        //check
        assertEquals(cacheSizeBefore + 1, postgreSqlDriver.get().poolSize());
        Thread.sleep(1000L);
        assertEquals(cacheSizeBefore + 1, postgreSqlDriver.get().poolSize());
        Thread.sleep(7000L);
        assertEquals(cacheSizeBefore, postgreSqlDriver.get().poolSize());
    }

    @Test
    void abortLongExecution() throws InterruptedException {
        //mock
        postgreSqlDriver.set(spy(new PostgreSqlDriver(miaConfiguration.executorServiceForSql(0, 2, 1500), 3000, 1800)));
        ReflectionTestUtils.setField(postgreSqlDriver.get(), "executionTimeout", 3);
        queryDriverFactory.set(new QueryDriverFactory(Arrays.asList(cassandraDriver.get(), oracleDriver.get(), postgreSqlDriver.get())));
        //call
        Long timestampBefore = System.currentTimeMillis();
        long cacheSizeBefore = postgreSqlDriver.get().poolSize();
        try {
            postgreSqlDriver.get().executeQuery(server.get(), "SELECT pg_sleep(30);");
            fail("Must be SqlTimeoutException");
        } catch (SqlTimeoutException timeoutException) {
            //check
            Long timestampAfter = System.currentTimeMillis();
            MatcherAssert.assertThat(timestampAfter - timestampBefore, Matchers.lessThan(8000L)); // slow machine in jenkins
            Thread.sleep(7000L);
            assertEquals(cacheSizeBefore, postgreSqlDriver.get().poolSize());
        }
    }
}
