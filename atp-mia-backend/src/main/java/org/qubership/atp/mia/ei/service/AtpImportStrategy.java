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

package org.qubership.atp.mia.ei.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.dto.validation.MessageType;
import org.qubership.atp.ei.node.dto.validation.UserMessage;
import org.qubership.atp.integration.configuration.model.notification.Notification;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.mia.ei.component.ImportLoader;
import org.qubership.atp.mia.exceptions.ei.MiaImportTypeNotSupportException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AtpImportStrategy implements ImportStrategy {

    public static ThreadLocal<Long> IMPORT_TIMESTAMP = new ThreadLocal<>();
    private final List<ImportLoader> importLoaders;
    private final ProjectConfigurationService projectConfigurationService;
    private final NotificationService notificationService;

    public ExportFormat getFormat() {
        return ExportFormat.ATP;
    }

    @Override
    public void miaImport(ExportImportData importData, Path path) {
        try {
            IMPORT_TIMESTAMP.set(System.currentTimeMillis());
            ProjectConfiguration projectConfiguration =
                    projectConfigurationService.getConfiguration(importData.getProjectId());
            if (projectConfiguration.getGitUrl() == null || projectConfiguration.getGitUrl().trim().isEmpty()) {
                Path sectionPath = path.resolve(ExportImportEntities.MIA_SECTION.getValue());
                getImportLoader(ExportImportEntities.MIA_SECTION)
                        .importEntity(projectConfiguration, importData, sectionPath);
                getImportLoader(ExportImportEntities.MIA_PROCESSES)
                        .importEntity(projectConfiguration, importData, sectionPath);
                getImportLoader(ExportImportEntities.MIA_COMPOUNDS)
                        .importEntity(projectConfiguration, importData, sectionPath);
                Path filesPath = path.resolve(ExportImportEntities.MIA_FILES.getValue());
                getImportLoader(ExportImportEntities.MIA_DIRECTORY)
                        .importEntity(projectConfiguration, importData, filesPath);
                getImportLoader(ExportImportEntities.MIA_FILES)
                        .importEntity(projectConfiguration, importData, filesPath);
                Path projectConfigurationPath = path.resolve(ExportImportEntities.MIA_PROJECT_CONFIGURATION.getValue());
                getImportLoader(ExportImportEntities.MIA_COMMON_CONFIGURATION)
                        .importEntity(projectConfiguration, importData, projectConfigurationPath);
                getImportLoader(ExportImportEntities.MIA_HEADER_CONFIGURATION)
                        .importEntity(projectConfiguration, importData, projectConfigurationPath);
                getImportLoader(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION)
                        .importEntity(projectConfiguration, importData, projectConfigurationPath);
                log.info("Save configuration after import");
                projectConfigurationService.updateProjectWithReplicationOff(projectConfiguration, true);
            } else {
                log.info("Skipping MIA import. {}", ErrorCodes.MIA_7008_IMPORT_GIT_ENABLED.getMessage());
            }
        } catch (Exception e) {
            log.error("Error during the import: {}", e.getMessage(), e);
            throw e;
        } finally {
            IMPORT_TIMESTAMP.remove();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValidationResult validateData(ExportImportData importData, Path path) {
        List<UserMessage> details = new ArrayList<>();
        Map<UUID, UUID> replacementMap = new HashMap<>(importData.getReplacementMap());
        ProjectConfiguration projectConfiguration =
                projectConfigurationService.getConfiguration(importData.getProjectId());

        if (projectConfiguration.getGitUrl() != null && !projectConfiguration.getGitUrl().trim().isEmpty()) {
            String message = ErrorCodes.MIA_7008_IMPORT_GIT_ENABLED.getMessage();
            log.warn(message);
            details.add(new UserMessage(message));
            notificationService.sendNotification(
                    new Notification(message, Notification.Type.WARNING, projectConfiguration.getProjectId())
            );
        } else {
            try {
                Path sectionPath = path.resolve(ExportImportEntities.MIA_SECTION.getValue());
                Set<UUID> allIds = new HashSet<>();
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_SECTION)
                        .validate(projectConfiguration, importData, sectionPath));
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_PROCESSES)
                        .validate(projectConfiguration, importData, sectionPath));
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_COMPOUNDS)
                        .validate(projectConfiguration, importData, sectionPath));

                Path filesPath = path.resolve(ExportImportEntities.MIA_FILES.getValue());
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_DIRECTORY)
                        .validate(projectConfiguration, importData, filesPath));
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_FILES)
                        .validate(projectConfiguration, importData, filesPath));

                Path projectConfigurationPath = path.resolve(ExportImportEntities.MIA_PROJECT_CONFIGURATION.getValue());
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_COMMON_CONFIGURATION)
                        .validate(projectConfiguration, importData, projectConfigurationPath));
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_HEADER_CONFIGURATION)
                        .validate(projectConfiguration, importData, projectConfigurationPath));
                allIds.addAll(getImportLoader(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION)
                        .validate(projectConfiguration, importData, projectConfigurationPath));
                boolean isReplacement = importData.isCreateNewProject() || importData.isInterProjectImport();
                if (isReplacement) {
                    allIds.forEach(id -> replacementMap.put(id, UUID.randomUUID()));
                }
                log.info("Validation complete ids {} be replaced", isReplacement ? "will" : "won't");
            } catch (Exception e) {
                log.error("Error during import", e);
                UserMessage message = new UserMessage(e.getMessage());
                message.setMessageType(MessageType.ERROR);
                details.add(message);
            }
        }
        return new ValidationResult(details, replacementMap);
    }

    private ImportLoader getImportLoader(@NotNull ExportImportEntities type) throws MiaImportTypeNotSupportException {
        return importLoaders.stream()
                .filter(importLoader -> type.equals(importLoader.getEntityType()))
                .findFirst()
                .orElseThrow(() -> new MiaImportTypeNotSupportException(type.name()));
    }
}
