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

import java.util.List;

import javax.validation.constraints.NotNull;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.mia.ei.service.ExportStrategy;
import org.qubership.atp.mia.exceptions.ei.MiaExportTypeNotSupportException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExportStrategiesRegistry {

    private final List<ExportStrategy> exportStrategies;

    /**
     * Lookup export strategy by export tool type and request transport type parameters.
     *
     * @param format export format
     * @return export strategy implementation
     */
    public ExportStrategy getExportStrategy(@NotNull ExportFormat format) throws MiaExportTypeNotSupportException {
        return exportStrategies.stream()
                .filter(exportStrategy -> format.equals(exportStrategy.getFormat()))
                .findFirst()
                .orElseThrow(() -> new MiaExportTypeNotSupportException(format.name()));
    }
}
