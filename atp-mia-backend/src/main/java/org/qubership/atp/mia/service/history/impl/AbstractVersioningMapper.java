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

import javax.annotation.PostConstruct;

import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.AbstractCompareEntityDto;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemTypeDto;
import org.qubership.atp.mia.model.DateAuditorEntity;

public abstract class AbstractVersioningMapper<S extends DateAuditorEntity, D extends AbstractCompareEntityDto>
        extends AbstractMapper<S, D> {

    AbstractVersioningMapper(Class<S> sourceClass, Class<D> destinationClass, ModelMapper mapper) {
        super(sourceClass, destinationClass, mapper);
    }

    abstract HistoryItemTypeDto getEntityTypeEnum();

    @PostConstruct
    public void setupMapper() {
        mapper.createTypeMap(sourceClass, destinationClass).setPostConverter(mapConverter());
    }

    @Override
    void mapSpecificFields(S source, D destination) {
        destination.entityType(getEntityTypeEnum());
    }
}
