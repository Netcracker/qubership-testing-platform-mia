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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class CassandraContainer extends GenericContainer<CassandraContainer> {

    private static CassandraContainer cassandraContainer;

    private CassandraContainer(ImageFromDockerfile imageFromDockerfile) {
        super(imageFromDockerfile);
    }

    public static CassandraContainer getInstance(int initPort) {
        if (cassandraContainer == null) {
            cassandraContainer = new CassandraContainer(
                    new ImageFromDockerfile().withFileFromFile("Dockerfile", new File("src/test/resources/db/cassandra/Dockerfile")))
                    .withExposedPorts(initPort);
        }
        return cassandraContainer;
    }
}
