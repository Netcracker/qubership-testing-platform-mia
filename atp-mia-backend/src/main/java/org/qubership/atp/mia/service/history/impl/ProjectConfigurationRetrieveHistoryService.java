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

package org.qubership.atp.mia.service.history.impl;

import org.javers.core.Javers;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemTypeDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectConfigurationHistoryChangeDto;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectConfigurationRetrieveHistoryService
        extends AbstractRetrieveHistoryService<ProjectConfiguration, ProjectConfigurationHistoryChangeDto> {
    @Autowired
    public ProjectConfigurationRetrieveHistoryService(Javers javers,
                                         ProjectConfigurationVersioningMapper mapper) {
        super(javers, mapper);
    }

    @Override
    public Class<ProjectConfiguration> getEntityClass() {
        return ProjectConfiguration.class;
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.PROJECTCONFIGURATION;
    }
}
