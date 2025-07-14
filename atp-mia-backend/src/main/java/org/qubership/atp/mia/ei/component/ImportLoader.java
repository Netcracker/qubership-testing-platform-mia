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

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.ei.MiaImportEntityException;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportIdentifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public abstract class ImportLoader<E extends ExportImportEntities,
        T extends Serializable,
        F extends ExportImportIdentifier> {

    public static final String EI_CONFLICT = "_ImportConflict_";
    protected final ObjectLoaderFromDiskService objectLoaderFromDiskService;

    /**
     * Get class of import entity.
     *
     * @return class
     */
    public abstract Class<F> getClazz();

    /**
     * Get entity type.
     *
     * @return entity type
     */
    public abstract E getEntityType();

    /**
     * Import entity.
     */
    public abstract void importEntity(ProjectConfiguration projectConfiguration,
                                      ExportImportData importData,
                                      Path path);

    /**
     * Convert model from to.
     *
     * @param projectConfiguration project configuration
     * @param modelFrom            model from
     * @return model to
     */
    public abstract T toEntity(ProjectConfiguration projectConfiguration, F modelFrom);

    /**
     * Validate.
     *
     * @param projectConfiguration projectConfiguration
     * @param importData           importData
     * @param path                 path to load objects
     * @return List of IDs to be imported
     * @throws MiaException if validation failed
     */
    public abstract List<UUID> validate(ProjectConfiguration projectConfiguration,
                                        ExportImportData importData,
                                        Path path) throws MiaException;

    /**
     * Load configuration from files.
     *
     * @param importData import data
     * @param path       path for files
     * @return ImportExport configuration instance.
     */
    protected List<F> loadConfiguration(ExportImportData importData, Path path) {
        log.info("Extract {} during import", getEntityType());
        List<F> extractedFolderEntities = new ArrayList<>();
        Path entityPath = Paths.get(path.toString());
        Map<UUID, Path> entityFiles = objectLoaderFromDiskService.getListOfObjects(entityPath, getClazz());
        Map<UUID, UUID> replacementMap = importData.getReplacementMap();
        log.debug("Extracted {} list with paths: {}", getEntityType(), entityFiles);
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        entityFiles.forEach((id, filePath) -> {
            log.debug("Extract {} configuration with ID {}", getEntityType(), id);
            F entityConfiguration = load(filePath, replacementMap, isReplacement);
            log.debug("Loaded {} configuration: {}", getEntityType(), entityConfiguration);
            if (entityConfiguration == null) {
                throw new MiaImportEntityException(getEntityType(), filePath);
            }
            entityConfiguration.setSourceId(id);
            extractedFolderEntities.add(entityConfiguration);
        });
        log.info("{} are extracted during import: {}", getEntityType(), extractedFolderEntities.size());
        return extractedFolderEntities;
    }

    protected List<UUID> replaceIdsBack(List<UUID> uuids, Map<UUID, UUID> map) {
        List<UUID> returnList = new ArrayList<>();
        uuids.forEach(uuid -> returnList.add(map.entrySet().stream()
                .filter(entry -> uuid.equals(entry.getValue()))
                .findFirst()
                .orElse(new AbstractMap.SimpleEntry<>(uuid, uuid)).getKey()));
        return returnList;
    }

    private F load(Path filePath, Map<UUID, UUID> replacementMap, boolean isReplacement) {
        if (isReplacement) {
            log.debug("Load {} by path [{}] with replacementMap: {}", getEntityType(), filePath, replacementMap);
            return objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(filePath, getClazz(), replacementMap,
                    true, false);
        } else {
            log.debug("Load {} by path [{}] without replacementMap", getEntityType(), filePath);
            return objectLoaderFromDiskService.loadFileAsObject(filePath, getClazz());
        }
    }
}
