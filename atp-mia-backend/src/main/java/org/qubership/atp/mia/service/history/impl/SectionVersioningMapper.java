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

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemTypeDto;
import org.qubership.atp.mia.controllers.api.dto.SectionHistoryChangeDto;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SectionVersioningMapper extends AbstractVersioningMapper<SectionConfiguration, SectionHistoryChangeDto> {

    @Autowired
    public SectionVersioningMapper(ModelMapper mapper) {
        super(SectionConfiguration.class, SectionHistoryChangeDto.class, mapper);
    }

    @Override
    protected HistoryItemTypeDto getEntityTypeEnum() {
        return HistoryItemTypeDto.SECTION;
    }

    @Override
    public void mapSpecificFields(SectionConfiguration source, SectionHistoryChangeDto destination) {
        super.mapSpecificFields(source, destination);
        destination.setName(source.getName());
        if (source.getParentSection() != null) {
            destination.parentSection(source.getParentSection().getName());
        }
        destination.place(source.getPlace());
        destination.childSections(source.getSections()
                .stream().map(SectionConfiguration::getName).collect(Collectors.toList()));
        destination.compounds(source.getCompounds()
                .stream().map(CompoundConfiguration::getName).collect(Collectors.toList()));
        destination.processes(source.getProcesses()
                .stream().map(ProcessConfiguration::getName).collect(Collectors.toList()));
    }
}
