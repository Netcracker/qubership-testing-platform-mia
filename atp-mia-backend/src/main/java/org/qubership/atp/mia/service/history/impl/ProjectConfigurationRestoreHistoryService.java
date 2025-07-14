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
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.service.history.EntityHistoryService;
import org.springframework.stereotype.Service;

@Service
public class ProjectConfigurationRestoreHistoryService extends AbstractRestoreHistoryService<ProjectConfiguration> {

    public ProjectConfigurationRestoreHistoryService(Javers javers,
                                                     EntityHistoryService<ProjectConfiguration> entityHistoryService,
                                                     ValidateReferenceExistsService validateReferenceExistsService,
                                                     ProjectConfigurationRestoreMapper modelMapper) {
        super(javers, entityHistoryService, validateReferenceExistsService, modelMapper);
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.PROJECTCONFIGURATION;
    }

    @Override
    public Class<ProjectConfiguration> getEntityClass() {
        return ProjectConfiguration.class;
    }

    @Override
    protected void copyValues(ProjectConfiguration shadow, ProjectConfiguration actualObject) {
        actualObject.setValidationResult(shadow.getValidationResult());
        actualObject.setCommonConfiguration(shadow.getCommonConfiguration());
        actualObject.setHeaderConfiguration(shadow.getHeaderConfiguration());
        actualObject.setPotHeaderConfiguration(shadow.getPotHeaderConfiguration());
    }
}
