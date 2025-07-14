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
import org.qubership.atp.mia.integration.containers.MongoContainer;

public class IntegrationMongoContainerEnvironment extends ExternalResource {

    private static IntegrationMongoContainerEnvironment instance;
    private static MongoContainer mongoContainer;
    private String mongoAddress;
    private String mongoPort;
    private static final int initPort = 27017;

    private IntegrationMongoContainerEnvironment() {
        Optional<String> startEnvironment = Optional.ofNullable(System.getProperty("LOCAL_DOCKER_START"));
        if (startEnvironment.isPresent() && Boolean.parseBoolean(startEnvironment.get())) {
            mongoContainer = MongoContainer.getInstance(initPort);
        } else {
            mongoAddress = System.getProperty("MONGO_IP");
            mongoPort = String.valueOf(initPort);
        }
    }

    public static IntegrationMongoContainerEnvironment getInstance() {
        if (instance == null) {
            instance = new IntegrationMongoContainerEnvironment();
        }
        return instance;
    }

    @Override
    protected void before() {
        if (mongoContainer != null) {
            mongoContainer.start();
            mongoAddress = mongoContainer.getContainerIpAddress();
            mongoPort = String.valueOf(mongoContainer.getMappedPort(initPort));
        }
    }

    @Override
    protected void after() {
        if (mongoContainer != null) {
            mongoContainer.stop();
        }
    }

    public String getMongoAddress() {
        return mongoAddress;
    }

    public String getMongoPort() {
        return mongoPort;
    }
}
