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

import java.util.UUID;

import org.javers.core.Javers;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemTypeDto;
import org.qubership.atp.mia.exceptions.configuration.ProcessNotFoundException;
import org.qubership.atp.mia.exceptions.history.MiaCompoundHistoryRevisionRestoreException;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.service.history.EntityHistoryService;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CompoundConfigurationRestoreHistoryService extends AbstractRestoreHistoryService<CompoundConfiguration> {

    protected final Javers javers;
    protected final EntityHistoryService<CompoundConfiguration> entityHistoryService;
    private final ValidateReferenceExistsService validateReferenceExistsService;
    protected final AbstractRestoreMapper modelMapper;
    protected final ProcessConfigurationRestoreHistoryService processConfigurationRestoreHistoryService;

    /**
     * History service should use own custom Mapper.
     */
    public CompoundConfigurationRestoreHistoryService(Javers javers,
                                                      AbstractEntityHistoryService<CompoundConfiguration>
                                                              entityHistoryService,
                                                      ValidateReferenceExistsService validateReferenceExistsService,
                                                      CompoundRestoreMapper modelMapper,
                                                      ProcessConfigurationRestoreHistoryService
                                                              processConfigurationRestoreHistoryService) {
        super(javers, entityHistoryService, validateReferenceExistsService, modelMapper);
        this.entityHistoryService = entityHistoryService;
        this.validateReferenceExistsService = validateReferenceExistsService;
        this.modelMapper = modelMapper;
        this.javers = javers;
        this.processConfigurationRestoreHistoryService = processConfigurationRestoreHistoryService;
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.COMPOUND;
    }

    @Override
    public Class getEntityClass() {
        return CompoundConfiguration.class;
    }

    @Override
    protected void copyValues(CompoundConfiguration shadow, CompoundConfiguration actualObject) {
        modelMapper.map(shadow, actualObject);
    }

    @Override
    public Object restoreToRevision(UUID id, long revisionId) {
        try {
            return super.restoreToRevision(id, revisionId);
        } catch (ProcessNotFoundException ex) {
            throw new MiaCompoundHistoryRevisionRestoreException(revisionId);
        }
    }
}
