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

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.mia.model.CacheKeys;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiaCacheServiceNoHazelCast implements MiaCacheService {

    private CacheManager cacheManager;

    /**
     * Returns CacheManager Object.
     *
     * @return CacheManager Object.
     */
    public CacheManager cacheManager() {
        if (cacheManager == null) {
            List<Cache> caches = new ArrayList<>();
            for (CacheKeys key : CacheKeys.values()) {
                caches.add(new ConcurrentMapCache(key.getKey(),
                        CacheBuilder.newBuilder().expireAfterWrite(key.getTimeToLive(), key.getTimeUnit())
                                .maximumSize(100).build().asMap(),
                        true));
            }
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            cacheManager.setCaches(caches);
            this.cacheManager = cacheManager;
        }
        return cacheManager;
    }
}
