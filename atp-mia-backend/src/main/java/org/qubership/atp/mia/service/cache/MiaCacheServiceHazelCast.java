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

package org.qubership.atp.mia.service.cache;

import java.util.UUID;

import org.qubership.atp.mia.model.CacheKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConnectionStrategyConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiaCacheServiceHazelCast implements MiaCacheService {

    public static final String CACHE_CLIENT_NAME = "ATP_MIA_";
    public static final String CACHE_SERVER_NAME = "ATP-MIA-SERVER";
    public static final UUID CACHE_ID = UUID.randomUUID();

    @Value("${spring.cache.hazelcast.cluster-name:atp-hc}")
    private String cacheClusterName;
    @Value("${spring.cache.hazelcast.server.address:127.0.0.1}")
    private String hazelcastServerAddress;
    @Value("${spring.cache.hazelcast.server.enable:#{true}}")
    private boolean hazelcastServerEnable;
    @Value("${spring.cache.hazelcast.server.port:#{5701}}")
    private int hazelcastServerPort;
    private volatile HazelcastInstance hzInstanceClient;
    @Value("${server.port}")
    private String serverPort;
    private boolean serverStarted = false;
    private CacheManager cacheManager;

    /**
     * Mandatory.
     *
     * @return hazelcast cache manager
     */
    public CacheManager cacheManager() {
        if (cacheManager == null) {
            log.info("Create HAZELCAST cache manager");
            cacheManager = new HazelcastCacheManager(getHzInstance());
        }
        return cacheManager;
    }

    /**
     * Generates hazelcast instance client.
     *
     * @return instance
     */
    public synchronized HazelcastInstance getHzInstance() {
        if (hzInstanceClient == null) {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.setInstanceName(CACHE_CLIENT_NAME + "on_port_" + serverPort + "_with_id_" + CACHE_ID);
            clientConfig.setClusterName(cacheClusterName);
            clientConfig.getNetworkConfig().addAddress(hazelcastServerAddress + ":" + hazelcastServerPort);
            clientConfig.getConnectionStrategyConfig()
                    .setReconnectMode(ClientConnectionStrategyConfig.ReconnectMode.ASYNC);
            if (!serverStarted) {
                startCacheServer();
            }
            try {
                log.debug("Connect to HAZELCAST as client");
                hzInstanceClient = HazelcastClient.newHazelcastClient(clientConfig);
                for (CacheKeys key : CacheKeys.values()) {
                    String name = key.getKey();
                    if (CacheKeys.AUTH_PROJECTS_KEY.equals(key)) {
                        continue;
                    }
                    try {
                        log.debug("Try to create config for map {}", name);
                        hzInstanceClient.getConfig().addMapConfig(
                                new MapConfig(name).setTimeToLiveSeconds(key.getTtlInSeconds()));
                    } catch (Exception failedCreate) {
                        log.warn("Map {} already created. Not possible to change map config: {}", name, failedCreate);
                    }
                }
            } catch (Exception e) {
                log.error("HazelCast server is not available!!! {}", e);
                serverStarted = false;
            }
        }
        return hzInstanceClient;
    }

    private void startCacheServer() {
        if (hazelcastServerEnable) {
            log.info("Get or start cache config on address " + hazelcastServerAddress + ":" + hazelcastServerPort);
            Config config = new Config(CACHE_SERVER_NAME);
            NetworkConfig network = config.getNetworkConfig()
                    .setPort(hazelcastServerPort)
                    .setPortCount(1)
                    .setPortAutoIncrement(false)
                    .setReuseAddress(true);
            network.getJoin().getMulticastConfig().setEnabled(true);
            for (CacheKeys key : CacheKeys.values()) {
                if (CacheKeys.AUTH_PROJECTS_KEY.equals(key)) {
                    continue;
                }
                config.addMapConfig(new MapConfig(key.getKey()).setTimeToLiveSeconds(key.getTtlInSeconds()));
            }
            config.setClusterName(cacheClusterName);
            try {
                if (Hazelcast.getOrCreateHazelcastInstance(config) != null) {
                    serverStarted = true;
                }
            } catch (Exception e) {
                log.warn("HazelCast server already started: {}", e.getMessage());
                serverStarted = false;
            }
        } else {
            serverStarted = true;
        }
    }
}
