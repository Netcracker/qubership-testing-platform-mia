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

import java.util.LinkedList;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.springframework.stereotype.Component;

@Component
public class CompoundRestoreMapper extends AbstractRestoreMapper<CompoundConfiguration> {

    CompoundRestoreMapper(ModelMapper mapper) {
        super(CompoundConfiguration.class, CompoundConfiguration.class, mapper);
    }

    @Override
    public void mapSpecificFields(CompoundConfiguration source, CompoundConfiguration destination) {
        destination.setName(source.getName());
        destination.setReferToInput(source.getReferToInput());
        LinkedList<ProcessConfiguration> restoredProcesses = new LinkedList<>(source.getProcesses());
        LinkedList<ProcessConfiguration> converted = new LinkedList<>();
        restoredProcesses.forEach(pr -> {
            Optional<ProcessConfiguration> any = destination.getProjectConfiguration().getProcesses().stream()
                    .filter(process -> process.getId().equals(pr.getId())).findAny();
            if (any.isPresent()) {
                ProcessConfiguration processConfiguration = any.get();
                mapper.map(pr, processConfiguration);
                converted.add(processConfiguration);
            } else {
                converted.add(pr);
            }
        });
        destination.setProcesses(converted);
    }
}
