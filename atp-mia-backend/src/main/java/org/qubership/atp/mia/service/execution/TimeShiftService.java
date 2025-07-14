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

import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.repo.impl.pool.ssh.SshSessionPool;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeShiftService {

    private final MiaContext miaContext;
    private final SshSessionPool sshSessionPool;

    /**
     * Check if system is present in systemsForTimeShifting map and time shifitng is On.
     */
    public boolean checkTimeShifting(String systemId) {
        if (sshSessionPool.systemsForTimeShifting.containsKey(systemId)) {
            log.debug("System [{}] is present in systemsForTimeShifting list.", systemId);
            if (sshSessionPool.systemsForTimeShifting.get(systemId)) {
                log.debug("Time shifting is ON for system [{}].",
                        systemId);
                return true;
            } else {
                log.debug("Time shifting is OFF for system [{}].",
                        systemId);
            }
        } else {
            log.debug("No system [{}] in systemsForTimeShifting list.", systemId);
        }
        return false;
    }

    /**
     * Update systemsForTimeShifting with new systemId and time shifting value.
     *
     * @param value time shifting On/Off
     */
    public void updateTimeShifting(UUID systemId, boolean value) {
        String logMsg = SshSessionPool.systemsForTimeShifting.containsKey(systemId)
                ? "is updated with value" : "is added to systemsForTimeShifting list with value";
        log.debug("System [{}] {} {}.", systemId, logMsg, value);
        sshSessionPool.systemsForTimeShifting.put(systemId, value);
        Optional<System> system = miaContext.getFlowData().getEnvironment().getSystems()
                .stream().filter(sys -> sys.getId().equals(systemId)).findFirst();
        if (system.isPresent()) {
            Server server = miaContext.getFlowData()
                    .getSystem(system.get().getName()).getServer(Server.ConnectionType.SSH);
            if (value) {
                sshSessionPool.addTimeShiftSession(server, miaContext.getConfig().getCommonConfiguration());
            } else {
                sshSessionPool.removeTimeShiftSession(server);
            }
        }
    }
}
