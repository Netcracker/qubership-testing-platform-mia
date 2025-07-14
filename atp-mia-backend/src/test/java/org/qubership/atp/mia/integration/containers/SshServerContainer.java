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

package org.qubership.atp.mia.integration.containers;

import java.io.File;
import java.nio.file.InvalidPathException;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class SshServerContainer extends GenericContainer<SshServerContainer> {

    private static SshServerContainer container;
    private String sshIp;
    private Integer sshPort;

    private SshServerContainer(ImageFromDockerfile imageFromDockerfile) {
        super(imageFromDockerfile);
    }

    public static SshServerContainer getInstance() {
        if (container == null) {
            container = new SshServerContainer(
                    new ImageFromDockerfile().withFileFromFile("Dockerfile", new File("src/test/resources/sshserver/Dockerfile")))
                    .withExposedPorts(22);
        }
        return container;
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (
                InvalidPathException e) {
            throw new RuntimeException("Make sure docker application started!", e);
        }
        sshIp = container.getHost();
        sshPort = container.getMappedPort(22);
        log.info("Started External Server container at port {}", sshIp);
    }

    @Override
    public void stop() {
        super.stop();
        log.info("STOP External Server container at port {}", sshIp);
    }
}
