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

package org.qubership.atp.mia.ei.component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportPotHeaderConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ImportLoaderPotHeaderConfiguration extends ImportLoader<
        ExportImportEntities,
        PotHeaderConfiguration,
        ExportImportPotHeaderConfiguration> {

    public ImportLoaderPotHeaderConfiguration(@Autowired ObjectLoaderFromDiskService objectLoaderFromDiskService) {
        super(objectLoaderFromDiskService);
    }

    @Override
    public Class<ExportImportPotHeaderConfiguration> getClazz() {
        return ExportImportPotHeaderConfiguration.class;
    }

    @Override
    public ExportImportEntities getEntityType() {
        return ExportImportEntities.MIA_POT_HEADER_CONFIGURATION;
    }

    @Override
    public PotHeaderConfiguration toEntity(ProjectConfiguration projectConfig, ExportImportPotHeaderConfiguration dto) {
        return PotHeaderConfiguration.builder()
                .projectId(projectConfig.getProjectId())
                .headers(dto.getHeaders())
                .projectConfiguration(projectConfig)
                .build();
    }

    /**
     * Return an empty list, as the projectId serves as the configuration ID and should not be a random number.
     *
     * @param projectConfiguration projectConfiguration
     * @param importData           importData
     * @param path                 path to load objects
     * @return Return empty list.
     * @throws MiaException nested
     */
    @Override
    public List<UUID> validate(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path)
            throws MiaException {
        return Collections.emptyList();
    }

    @Override
    public void importEntity(ProjectConfiguration projectConfiguration, ExportImportData importData, Path path) {
        List<ExportImportPotHeaderConfiguration> configurations = loadConfiguration(importData, path);
        if (!configurations.isEmpty()) {
            ExportImportPotHeaderConfiguration configuration = configurations.get(0);
            projectConfiguration.setPotHeaderConfiguration(toEntity(projectConfiguration, configuration));
        }
    }
}
