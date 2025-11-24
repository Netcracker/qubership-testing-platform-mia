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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.mia.model.CacheKeys;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import com.hazelcast.spring.cache.HazelcastCache;


public interface MiaCacheService {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MiaCacheService.class);

    /**
     * Mandatory.
     *
     * @return hazelcast cache manager
     */
    CacheManager cacheManager();

    /**
     * Clear environments cache.
     */
    default void clearEnvironmentsCache(CacheManager cacheManager, UUID projectId) {
        for (CacheKeys cacheKey : CacheKeys.values()) {
            log.info("Clearing caches for cache key '{}' and project id {}", cacheKey, projectId);
            Cache cache = cacheManager.getCache(cacheKey.getKey());
            if (cache != null) {
                if (!cacheKey.isKeyContainProjectId()) {
                    cache.clear();
                    continue;
                }
                Set<Object> keySet = new HashSet<>();
                if (cache instanceof HazelcastCache) {
                    keySet = ((HazelcastCache) cache).getNativeCache().keySet();
                }
                if (cache instanceof CaffeineCache) {
                    keySet = ((CaffeineCache) cache).getNativeCache().asMap().keySet();
                }
                for (Object key : keySet) {
                    if (key.toString().contains(projectId.toString())) {
                        cache.evict(key);
                    }
                }
            }
        }
    }

    /**
     * Clear project configuration cache by projectId.
     *
     * @param projectId projectId
     */
    default void clearConfigurationCache(UUID projectId) {
        Cache cache = cacheManager().getCache(CacheKeys.Constants.CONFIGURATION_KEY_OS);
        if (cache != null) {
            cache.evictIfPresent(projectId);
        }
    }
}
