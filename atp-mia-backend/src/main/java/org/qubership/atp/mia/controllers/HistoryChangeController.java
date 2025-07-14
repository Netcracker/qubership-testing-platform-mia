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

package org.qubership.atp.mia.controllers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import org.qubership.atp.mia.controllers.api.MiaHistoryChangesControllerApi;
import org.qubership.atp.mia.controllers.api.dto.CompareEntityResponseDto;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemResponseDto;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.service.history.RestoreHistoryService;
import org.qubership.atp.mia.service.history.RetrieveHistoryService;
import org.qubership.atp.mia.service.history.impl.HistoryServiceFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HistoryChangeController implements MiaHistoryChangesControllerApi {
    private final HistoryServiceFactory historyServiceFactory;

    /**
     * Get entity changes.
     */
    @PreAuthorize("@entityAccess.checkAccess(#itemType, #projectId, 'READ')")
    public ResponseEntity<HistoryItemResponseDto> getAllHistory(UUID projectId,
                                                                String itemType,
                                                                UUID id,
                                                                @Valid Integer offset,
                                                                @Valid Integer limit) {
        Optional<RetrieveHistoryService<? extends DateAuditorEntity>> historyServiceOptional =
                historyServiceFactory.getRetrieveHistoryService(itemType);

        if (historyServiceOptional.isPresent()) {
            RetrieveHistoryService retrieveHistoryService = historyServiceOptional.get();
            HistoryItemResponseDto response = retrieveHistoryService.getAllHistory(id, offset, limit);
            return ResponseEntity.ok(response);
        } else {
            throw new RuntimeException(itemType);
        }
    }

    /**
     * Get all entity fields by versions for comparison.
     */
    @PreAuthorize("@entityAccess.checkAccess(#itemType, #projectId, 'READ')")
    public ResponseEntity<List<CompareEntityResponseDto>> getEntitiesByVersion(UUID projectId,
                                                                               String itemType,
                                                                               UUID uuid,
                                                                               List<String> versions) {
        Optional<RetrieveHistoryService<? extends DateAuditorEntity>> historyServiceOptional =
                historyServiceFactory.getRetrieveHistoryService(itemType);

        if (historyServiceOptional.isPresent()) {
            return ResponseEntity.ok(historyServiceOptional.get().getEntitiesByVersions(uuid, versions));
        } else {
            throw new RuntimeException(itemType);
        }
    }

    /**
     * Restore entity.
     */
    @PreAuthorize("@entityAccess.checkAccess(#itemType,#projectId,'UPDATE')")
    public ResponseEntity<Void> restoreToRevision(UUID projectId, String itemType,
                                                  UUID id, Integer revisionId) {
        Optional<RestoreHistoryService> historyServiceOptional =
                historyServiceFactory.getRestoreHistoryService(itemType);

        if (historyServiceOptional.isPresent()) {
            historyServiceOptional.get().restoreToRevision(id, revisionId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            throw new RuntimeException(itemType);
        }
    }
}
