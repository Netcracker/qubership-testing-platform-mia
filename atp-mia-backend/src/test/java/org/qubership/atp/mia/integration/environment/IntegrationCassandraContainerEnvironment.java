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
import org.qubership.atp.mia.integration.containers.CassandraContainer;

public class IntegrationCassandraContainerEnvironment extends ExternalResource {

    private static IntegrationCassandraContainerEnvironment instance;
    private static CassandraContainer cassandraContainer;
    private String cassandraAddress;
    private String cassandraPort;
    private static final int initPort = 9042;

    private IntegrationCassandraContainerEnvironment() {
        Optional<String> startEnvironment = Optional.ofNullable(System.getProperty("LOCAL_DOCKER_START"));
        if (startEnvironment.isPresent() && Boolean.parseBoolean(startEnvironment.get())) {
            cassandraContainer = CassandraContainer.getInstance(initPort);
        } else {
            cassandraAddress = System.getProperty("CASSANDRA_IP");
            cassandraPort = String.valueOf(initPort);
        }
    }

    public static IntegrationCassandraContainerEnvironment getInstance() {
        if (instance == null) {
            instance = new IntegrationCassandraContainerEnvironment();
        }
        return instance;
    }

    @Override
    protected void before() {
        if (cassandraContainer != null) {
            cassandraContainer.start();
            cassandraAddress = cassandraContainer.getHost();
            cassandraPort = String.valueOf(cassandraContainer.getMappedPort(initPort));
        }
    }

    @Override
    protected void after() {
        if (cassandraContainer != null) {
            cassandraContainer.stop();
        }
    }

    public String getCassandraAddress() {
        return cassandraAddress;
    }

    public String getCassandraPort() {
        return cassandraPort;
    }
}
