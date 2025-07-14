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

package org.qubership.atp.mia.repo.migration.v2;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.entities.ServiceEntities;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.mia.config.BeanAwareSpringLiquibase;
import org.qubership.atp.mia.model.UserManagementEntities;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public class ServiceEntitiesMigrationCustomChange implements CustomTaskChange {

    @Getter @Setter private String serviceName;

    private static final UUID SERVICE_ENTITIES_ID = UUID.fromString("d1dd25c3-5d8e-4398-b258-9f6471ee85d2");

    @SneakyThrows
    @Override
    public void execute(Database database) {
        ServiceEntities entities = new ServiceEntities();
        entities.setUuid(SERVICE_ENTITIES_ID);
        entities.setService(serviceName);
        entities.setEntities(Arrays.stream(UserManagementEntities.values())
                .map(UserManagementEntities::getName)
                .collect(Collectors.toList()));

        UsersService usersService = BeanAwareSpringLiquibase.getBean(UsersService.class);
        usersService.sendEntities(entities);
    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
