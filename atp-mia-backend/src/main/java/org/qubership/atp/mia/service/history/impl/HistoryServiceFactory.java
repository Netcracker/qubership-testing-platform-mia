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

import java.util.List;
import java.util.Optional;

import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.service.history.RestoreHistoryService;
import org.qubership.atp.mia.service.history.RetrieveHistoryService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HistoryServiceFactory {
    private final List<RestoreHistoryService> restoreHistoryServices;
    private final List<RetrieveHistoryService<? extends DateAuditorEntity>> retrieveHistoryServices;

    /**
     * Returns the concrete implementation of RestoreHistoryService depending on entity type.
     *
     * @param itemType type of domain entity with supported history
     * @return RestoreHistoryService implementation
     */
    public Optional<RestoreHistoryService> getRestoreHistoryService(String itemType) {
        return restoreHistoryServices.stream()
                .filter(service -> service.getItemType().toString().equalsIgnoreCase(itemType))
                .findFirst();
    }

    /**
     * Returns the concrete implementation of RetrieveHistoryService depending on entity type.
     *
     * @param itemType type of domain entity with supported history
     * @return RetrieveHistoryService implementation
     */
    public Optional<RetrieveHistoryService<? extends DateAuditorEntity>> getRetrieveHistoryService(String itemType) {
        return retrieveHistoryServices.stream()
                .filter(service -> service.getItemType().toString().equalsIgnoreCase(itemType))
                .findFirst();
    }
}
