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

package org.qubership.atp.mia.integration.ssh;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.repo.impl.SshConnectionManager;

//@Disabled("Temporarily disabled for refactoring")
public class SshConnectionTest extends BaseIntegrationTestConfiguration {

    @Test
    public void testSshConnection() {
        SshConnectionManager sshConnectionManager = getSshConnectionManager();
        assertNotNull(sshConnectionManager);
        String command = "echo 1";
        assertTrue(sshConnectionManager.runCommand(command).contains(command));
    }
}
