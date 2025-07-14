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

package org.qubership.atp.mia.model.environment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.mia.exceptions.externalsystemintegrations.EnvSystemNotFoundInContextException;

import com.google.common.base.Strings;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder
@Data
@NoArgsConstructor
@Slf4j
public class Environment extends AbstractConfiguratorModel {

    private UUID projectId;
    private List<System> systems;

    /**
     * Gets system by name.
     *
     * @param sysName system name
     * @return system name, throw RuntimeException if not found
     */
    public System getSystem(String sysName) {
        if (!Strings.isNullOrEmpty(sysName)) {
            final Optional<System> system = systems.stream().filter(sys -> sysName.equals(sys.getName())).findFirst();
            if (system.isPresent()) {
                return system.get();
            }
        }
        throw new EnvSystemNotFoundInContextException(sysName);
    }
}
