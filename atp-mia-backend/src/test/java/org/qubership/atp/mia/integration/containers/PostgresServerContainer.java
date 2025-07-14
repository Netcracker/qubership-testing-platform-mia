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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostgresServerContainer extends GenericContainer<PostgresServerContainer> {

    private static final String dbName = "mia";
    private static final int PG_PORT = 5432;
    private static PostgresServerContainer container;
    private String postgresIp;

    private PostgresServerContainer(ImageFromDockerfile imageFromDockerfile) {
        super(imageFromDockerfile);
    }

    public static PostgresServerContainer getInstance() {
        if (container == null) {
            container = new PostgresServerContainer(
                    new ImageFromDockerfile().withFileFromFile("Dockerfile", new File("src/test/resources/db/postgres/Dockerfile")))
                    .withExposedPorts(PG_PORT);
        }
        return container;
    }

    @Override
    public void start() {
        try {
            super.start();
        } catch (InvalidPathException e) {
            throw new RuntimeException("Make sure docker application started!", e);
        }
        postgresIp = container.getContainerIpAddress() + ":" + container.getMappedPort(PG_PORT);
        log.info("Started Postgres Server container at port {}", postgresIp);
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://" + postgresIp + "/" + dbName;
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}
