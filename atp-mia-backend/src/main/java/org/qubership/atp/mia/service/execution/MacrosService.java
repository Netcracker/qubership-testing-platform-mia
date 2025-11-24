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

package org.qubership.atp.mia.service.execution;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.macros.core.model.Macros;
import org.qubership.atp.macros.core.repository.MacrosRepository;
import org.qubership.atp.mia.model.CacheKeys;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class MacrosService {

    @Cacheable(value = CacheKeys.Constants.ATP_MACROS_KEY_OS, key = "#projectId", condition = "#projectId!=null",
            sync = true)
    public List<Macros> getMacros(UUID projectId, MacrosRepository macrosRepository) {
        return macrosRepository.findByProjectId(projectId);
    }
}
