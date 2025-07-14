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
import org.qubership.atp.mia.controllers.api.dto.FileHistoryChangeDto;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemTypeDto;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.springframework.stereotype.Component;

@Component
public class FileVersioningMapper extends AbstractVersioningMapper<ProjectFile, FileHistoryChangeDto> {
    public FileVersioningMapper(ModelMapper mapper) {
        super(ProjectFile.class, FileHistoryChangeDto.class, mapper);
    }

    @Override
    HistoryItemTypeDto getEntityTypeEnum() {
        return HistoryItemTypeDto.FILE;
    }

    @Override
    public void mapSpecificFields(ProjectFile source, FileHistoryChangeDto destination) {
        super.mapSpecificFields(source, destination);
        if (source.getDirectory() != null) {
            destination.setDirectory(source.getDirectory().getName());
        }
    }
}
