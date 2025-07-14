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
public class MongoContainer extends GenericContainer<MongoContainer> {

    private static MongoContainer mongoContainer;

    private MongoContainer(ImageFromDockerfile imageFromDockerfile) {
        super(imageFromDockerfile);
    }

    public static MongoContainer getInstance(int initPort) {
        if (mongoContainer == null) {
            mongoContainer = new MongoContainer(
                    new ImageFromDockerfile().withFileFromFile("Dockerfile", new File("src/test/resources/db/mongo/Dockerfile")))
                    .withExposedPorts(initPort);
        }
        return mongoContainer;
    }
}
