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

package org.qubership.atp.mia.repo.impl.pool.ssh;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.repo.impl.SshConnectionManager;
import org.qubership.atp.mia.repo.impl.SshSession;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SshSessionPool implements ConnectionPool {

    // Holds System Ids of systems for timeShifting
    public static final Map<UUID, Boolean> systemsForTimeShifting = new ConcurrentHashMap<>();
    private static final Map<Server, SshSession> connectionCache = new ConcurrentHashMap<>();
    // Holds actual timeShift sessions
    private static final Map<Server, SshSession> timeShiftStorage = new ConcurrentHashMap<>();
    public static int KEEP_ALIVE_MSG_INTERVAL;
    private final MiaContext miaContext;

    /**
     * Creates connection pool for SSH connections.
     * After {@code cleanTimeout} (seconds) removes ssh connection from cache.
     * Also every {@code cleanTimeout} (seconds) performs cleaning connections.
     *
     * @param cleanTimeout after this timeout in seconds cache will be
     */
    public SshSessionPool(@Value("${ssh.close.delay:300}") String cleanTimeout,
                          @Value("${db.server.keep.alive:30000}") String keepAlive,
                          MiaContext miaContext) {
        KEEP_ALIVE_MSG_INTERVAL =
                (int) Utils.parseLongValueOrDefault(keepAlive, 30000, "db.server.keep.alive");
        long cleanCacheTimeout = Utils.parseLongValueOrDefault(cleanTimeout, 180, "ssh.close.delay");
        // Schedule cache cleanup every cleanCacheTimeout
        ScheduledExecutorService cleanConnectionPool = Executors.newSingleThreadScheduledExecutor();
        cleanConnectionPool.scheduleAtFixedRate(this::cleanConnectionCache, 0, cleanCacheTimeout, TimeUnit.SECONDS);
        ScheduledExecutorService cleanTimeShiftPool = Executors.newSingleThreadScheduledExecutor();
        cleanTimeShiftPool.scheduleAtFixedRate(this::cleanTimeShiftMap, 0, cleanCacheTimeout, TimeUnit.SECONDS);
        this.miaContext = miaContext;
    }

    /**
     * For opening new Ssh connection.
     */
    private SshSession createSession(Server server, CommonConfiguration config) {
        log.debug("Connection is not present in cache. Initiating a new manager for server: {}", server.toString());
        return new SshSession(server, config);
    }

    /**
     * Adds SystemId and its time shift status, also creates/reopens session.
     * We avoid recreation of timeShift session on purpose, because establishing
     * such connection requires readiness of both connection sides.
     *
     * @param server        - with ssh server credentials.
     * @param configuration - configuration to configure keystore.
     */
    public void addTimeShiftSession(Server server, CommonConfiguration configuration) {
        if (!timeShiftStorage.containsKey(server)) {
            timeShiftStorage.put(server, createSession(server, configuration));
            log.debug("Added timeshift session for server: {}", server);
        }
    }

    /**
     * Removes and closes time shift session.
     *
     * @param server - which session need to remove.
     */
    public void removeTimeShiftSession(Server server) {
        log.debug("Removing timeshift session for server: {}", server);
        Optional<SshSession> removed = Optional.ofNullable(timeShiftStorage.remove(server));
        if (removed.isPresent()) {
            removed.get().disconnect();
            log.debug("Removing timeshift session for server: {}", server);
        }
    }

    /**
     * Checks whether timeshift for a given system is on.
     *
     * @param systemId - systemId
     * @return true or false.
     */
    private boolean isTimeShiftOn(UUID systemId) {
        return systemId != null && systemsForTimeShifting.containsKey(systemId) && systemsForTimeShifting.get(systemId);
    }

    /**
     * Checks if there is available connection in Cache.
     * If it is then returns already opened connection
     * otherwise creates a new one and overrides old.
     *
     * @param server - object with ssh server credentials.
     * @return SshConnectionManager which is ready to execute ssh commands.
     */
    @Override
    public SshConnectionManager getConnection(Server server, String extPrefix) {
        log.debug("Searching for open ssh connection by server: [{}]", server.toString());
        SshSession session = getTimeShiftSession(server).orElse(getCommonSession(server));
        return new SshConnectionManager(session, extPrefix, miaContext);
    }

    private SshSession getCommonSession(Server server) {
        log.trace("Trying to get session. Count of connections in storage: [{}]", connectionCache.size());
        CommonConfiguration commonConfiguration = miaContext.getConfig().getCommonConfiguration();
        SshSession session = connectionCache.get(server);
        if (session == null || !session.isSame(server, commonConfiguration)) {
            session = createSession(server, commonConfiguration);
            Optional.ofNullable(connectionCache.put(server, session)).ifPresent(SshSession::disconnect);
        }
        return session;
    }

    private Optional<SshSession> getTimeShiftSession(Server server) {
        if (isTimeShiftOn(server.getConnection().getSystemId())) {
            log.debug("Timeshift for server is on: [{}]", server);
            SshSession conn = timeShiftStorage.get(server);
            if (conn == null) {
                log.error("Connection to timeShift is null, but should be open! Reopening...");
                conn = createSession(server, miaContext.getConfig().getCommonConfiguration());
            } else {
                log.debug("Found cached and open timeShift session for server: [{}]", server);
            }
            timeShiftStorage.put(server, conn);
            return Optional.of(conn);
        }
        return Optional.empty();
    }

    /**
     * Invalidates all free and not executing connection in cache (soft clean).
     */
    public void cleanConnectionCache() {
        Thread.currentThread().setName("mia_cleanConnectionCache");
        log.info("Try to clean SSH connection cache");
        synchronized (connectionCache) {
            int size = connectionCache.size();
            boolean isDelete = connectionCache.entrySet().removeIf(e ->
                    !e.getValue().isExecuting() && e.getValue().disconnect());
            log.info("{} connectionCache size before: {}, after: {}",
                    isDelete ? "Removed some entities in ts map" : "Nothing removed", size, connectionCache.size());
        }
    }

    /**
     * Removes all not time shifted entities (with false as a key).
     */
    public void cleanTimeShiftMap() {
        Thread.currentThread().setName("mia_cleanTimeShiftMap");
        log.trace("Try to clean time shift map");
        synchronized (systemsForTimeShifting) {
            log.trace("Clean time shift map");
            int size = systemsForTimeShifting.size();
            boolean isDelete = systemsForTimeShifting.entrySet().removeIf(e -> !e.getValue());
            log.trace("{} systemsForTimeShifting size before: {}, after: {}",
                    isDelete ? "Removed some entities in ts map" : "Nothing removed", size, timeShiftStorage.size());
        }
        synchronized (timeShiftStorage) {
            int size = timeShiftStorage.size();
            boolean isDelete = timeShiftStorage.entrySet().removeIf(e -> !e.getValue().isExecuting()
                    && isTimeShiftOn(e.getKey().getConnection().getSystemId()));
            log.trace("{} timeShiftStorage size before: {}, after: {}",
                    isDelete ? "Removed some entities in ts map" : "Nothing removed", size, timeShiftStorage.size());
        }
        log.trace("Cleaning over for time shift map");
    }

    @Override
    public void resetCache() {
        log.info("Cleaning ssh cache. Current size.\n connection: {}; systemsForTimeshifting: {}; timeshiftStorage: {}",
                connectionCache.size(), systemsForTimeShifting.size(), timeShiftStorage.size());
        synchronized (connectionCache) {
            connectionCache.clear();
        }
        synchronized (systemsForTimeShifting) {
            systemsForTimeShifting.clear();
        }
        synchronized (timeShiftStorage) {
            timeShiftStorage.clear();
        }
        log.info("Complete clean manually ssh cache.");
    }
}
