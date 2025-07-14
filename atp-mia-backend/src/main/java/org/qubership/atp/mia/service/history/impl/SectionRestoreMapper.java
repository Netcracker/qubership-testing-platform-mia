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
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.springframework.stereotype.Component;

@Component
public class SectionRestoreMapper extends AbstractRestoreMapper<SectionConfiguration> {

    SectionRestoreMapper(ModelMapper mapper) {
        super(SectionConfiguration.class, SectionConfiguration.class, mapper);
    }

    @Override
    public void mapSpecificFields(SectionConfiguration source, SectionConfiguration destination) {
        destination.setName(source.getName());
        destination.setPlace(source.getPlace());
        if (source.getParentSection() != null) {
            if (destination.getParentSection() == null) {
                destination.setParentSection(new SectionConfiguration());
            } else {
                mapper.map(destination.getParentSection(), source.getParentSection());
            }
        } else {
            destination.setParentSection(null);
        }
    }
}
