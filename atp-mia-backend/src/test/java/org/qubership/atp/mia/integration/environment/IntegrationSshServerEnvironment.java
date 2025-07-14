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

package org.qubership.atp.mia.integration.environment;

import java.util.Optional;

import org.junit.rules.ExternalResource;
import org.qubership.atp.mia.integration.containers.SshServerContainer;

public class IntegrationSshServerEnvironment extends ExternalResource {

    private static IntegrationSshServerEnvironment instance;
    private String sshServerIp;
    private SshServerContainer sshServerContainer;

    private IntegrationSshServerEnvironment() {
        Optional<String> startEnvironment = Optional.ofNullable(System.getProperty("LOCAL_DOCKER_START"));
        if (startEnvironment.isPresent() && Boolean.parseBoolean(startEnvironment.get())) {
            sshServerContainer = SshServerContainer.getInstance(); // locally
        } else {
            sshServerIp = System.getProperty("SSHSERVER_IP"); // Jenkins job initialized SSH server
        }
    }

    public static IntegrationSshServerEnvironment getInstance() {
        if (instance == null) {
            instance = new IntegrationSshServerEnvironment();
        }
        return instance;
    }

    @Override
    protected void before() {
        if (sshServerContainer != null) {
            sshServerContainer.start();
        }
    }

    @Override
    protected void after() {
        if (sshServerContainer != null) {
            sshServerContainer.stop();
        }
    }

    public String getSshServerIp() {
        if (sshServerContainer != null) {
            return sshServerContainer.getSshIp();
        } else {
            return sshServerIp;
        }
    }

    public int getSshServerPort() {
        if (sshServerContainer != null) {
            return sshServerContainer.getSshPort();
        } else {
            return 22;
        }
    }
}
