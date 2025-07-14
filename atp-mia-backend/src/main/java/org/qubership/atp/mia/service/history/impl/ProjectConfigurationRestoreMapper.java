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

import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.springframework.stereotype.Component;

@Component
public class ProjectConfigurationRestoreMapper extends AbstractRestoreMapper<ProjectConfiguration> {
    ProjectConfigurationRestoreMapper(ModelMapper mapper) {
        super(ProjectConfiguration.class, ProjectConfiguration.class, mapper);
    }

    @Override
    public void mapSpecificFields(ProjectConfiguration source, ProjectConfiguration destination) {
        destination.setProjectName(source.getProjectName());
        destination.setGitUrl(source.getGitUrl());
        destination.setCommonConfiguration(source.getCommonConfiguration());
        destination.setHeaderConfiguration(source.getHeaderConfiguration());
        destination.setPotHeaderConfiguration(source.getPotHeaderConfiguration());
        destination.setLastLoadedWhen(source.getLastLoadedWhen());
        destination.setPrimaryMigrationDone(source.isPrimaryMigrationDone());
        destination.setValidationResult(source.getValidationResult());
    }
}
