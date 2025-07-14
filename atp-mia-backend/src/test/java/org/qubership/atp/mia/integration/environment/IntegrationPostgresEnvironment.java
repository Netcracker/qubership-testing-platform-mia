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
import org.qubership.atp.mia.integration.containers.PostgresServerContainer;

public class IntegrationPostgresEnvironment extends ExternalResource {

    private static IntegrationPostgresEnvironment instance;
    private String postgresJdbcUrl;
    private PostgresServerContainer postgresServerContainer;

    private IntegrationPostgresEnvironment() {
        Optional<String> startEnvironment = Optional.ofNullable(System.getProperty("LOCAL_DOCKER_START"));
        if (startEnvironment.isPresent() && Boolean.parseBoolean(startEnvironment.get())) {
            postgresServerContainer = PostgresServerContainer.getInstance();
        } else {
            postgresJdbcUrl = "jdbc:postgresql://" + System.getProperty("POSTGRES_IP") + ":5432/mia";
        }
    }

    public static IntegrationPostgresEnvironment getInstance() {
        if (instance == null) {
            instance = new IntegrationPostgresEnvironment();
        }
        return instance;
    }

    @Override
    protected void before() {
        if (postgresServerContainer != null) {
            postgresServerContainer.start();
            postgresJdbcUrl = postgresServerContainer.getJdbcUrl();
        }
    }

    @Override
    protected void after() {
        if (postgresServerContainer != null) {
            postgresServerContainer.stop();
        }
    }

    public String getJdbcUrl() {
        return postgresJdbcUrl;
    }
}
