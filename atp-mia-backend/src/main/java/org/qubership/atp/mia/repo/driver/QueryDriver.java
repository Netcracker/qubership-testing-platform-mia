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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlFirstValueNotPresentException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.pot.db.DbAnswer;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.utils.Utils;
import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

public interface QueryDriver<T extends AutoCloseable> {

    /**
     * Close.
     */
    default void close(T closeable) {
        Utils.closeQuietly(closeable);
    }

    /**
     * Create Closeable object for server.
     *
     * @param server server
     * @return Closeable object
     */
    T create(Server server);

    /**
     * Executes query.
     */
    default SqlResponse executeQuery(Server server, String query, String tableName,
                                     boolean saveToWordFile, boolean saveToZipFile, int dbExecutionRecordsLimit) {
        DbTable dbTable = executeQuery(server, query, dbExecutionRecordsLimit);
        SqlResponse sqlResponse = new SqlResponse(server);
        sqlResponse.setQuery(query);
        sqlResponse.setData(dbTable);
        sqlResponse.setRecords(dbTable.getData().size());
        sqlResponse.setTableName(tableName);
        sqlResponse.setSaveToWordFile(saveToWordFile);
        sqlResponse.setSaveToZipFile(saveToZipFile);
        if (dbTable.getActualDataSizeBeforeLimit() > dbExecutionRecordsLimit) {
            sqlResponse.setLimitRecordsMessage("The number of returned rows exceeds the maximum allowed number of "
                    + dbExecutionRecordsLimit + " rows");
        }
        return sqlResponse;
    }

    /**
     * Execute query.
     *
     * @param server Server
     * @param query  query
     * @return DbTable
     */
    DbTable executeQuery(Server server, String query);

    DbTable executeQuery(Server server, String query, int limitRecords);

    /**
     * Executes query.
     */
    default String executeQueryAndGetFirstValue(Server server, String query) {
        try {
            DbTable table = executeQuery(server, query, 0);
            return table.getData().get(0).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new SqlFirstValueNotPresentException(query);
        }
    }

    /**
     * Execute 'update'.
     *
     * @param server server
     * @param query  query
     * @return number affected rows
     */
    int executeUpdate(Server server, String query);

    /**
     * Execute stored procedure.
     *
     * @param server server
     * @param query  query
     */
    DbAnswer executeStoredProcedure(Server server, String query);

    /**
     * Get driver type.
     *
     * @return driver type
     */
    String getDriverType();

    /**
     * Get execution timeout (milliseconds).
     *
     * @return milliseconds
     */
    default int getExecutionTimeout(int executionTimeout, Server server) {
        int timeout = executionTimeout < 1 ? 30 : executionTimeout;
        return server.getTimeoutExecute(timeout, 600);
    }

    /**
     * Init pool clean up .
     *
     * @param log         Logger.
     * @param expireAfter expireAfter.
     */
    default LoadingCache<Server, T> initPool(Logger log, int expireAfter) {
        long expireTimeout = expireAfter < 1 ? 30000 : expireAfter;
        return CacheBuilder.newBuilder()
                .expireAfterAccess(expireTimeout, TimeUnit.MILLISECONDS)
                .removalListener((RemovalListener<Server, T>) notification -> {
                    log.info("{} connection released for {}", getDriverType(), notification.getKey());
                    close(notification.getValue());
                })
                .build(new CacheLoader<Server, T>() {
                    @Override
                    public T load(Server key) {
                        log.debug("{} connection added for {}", getDriverType(), key);
                        return create(key);
                    }
                });
    }

    /**
     * Init pool clean up .
     *
     * @param log            Logger.
     * @param cache          cache.
     * @param cleanUpTimeout timeout for call clean up.
     */
    default void initPoolCleanUp(Logger log, LoadingCache<Server, T> cache, long cleanUpTimeout) {
        long timeout = cleanUpTimeout < 1 ? 30000 : cleanUpTimeout;
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    Thread.currentThread().setName("mia_clean" + getDriverType() + "Pool");
                    long cacheSizeBefore = cache.size();
                    cache.cleanUp();
                    log.info("Clean {} cache finished. Size before {} and after {}",
                            getDriverType(), cacheSizeBefore, cache.size());
                }, 0L, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Gets pool size.
     *
     * @return pool size
     */
    long poolSize();
}
