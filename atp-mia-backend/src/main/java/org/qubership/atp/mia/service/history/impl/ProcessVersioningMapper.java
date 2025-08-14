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
import org.qubership.atp.mia.controllers.api.dto.HistoryItemTypeDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessHistoryChangeDto;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.converters.ProcessSettingsConverter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProcessVersioningMapper extends AbstractVersioningMapper<ProcessConfiguration, ProcessHistoryChangeDto> {

    private final ProcessSettingsConverter processSettingsConverter;

    ProcessVersioningMapper(ModelMapper mapper, ProcessSettingsConverter processSettingsConverter) {
        super(ProcessConfiguration.class, ProcessHistoryChangeDto.class, mapper);
        this.processSettingsConverter = processSettingsConverter;
    }

    protected HistoryItemTypeDto getEntityTypeEnum() {
        return HistoryItemTypeDto.PROCESS;
    }

    @Override
    public void mapSpecificFields(ProcessConfiguration source, ProcessHistoryChangeDto destination) {
        super.mapSpecificFields(source, destination);
        destination.id(source.getId());
        destination.setName(source.getName());
        if (source.getCompounds() != null) {
            destination.setInSections(source.getSections());
        }

        if (source.getCompounds() != null) {
            destination.setInCompounds(source.getCompounds());
        }
        destination.setProcessSettings(processSettingsConverter.convertToDatabaseColumn(source.getProcessSettings()));
    }
}
