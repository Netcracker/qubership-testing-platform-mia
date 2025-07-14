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

package org.qubership.atp.mia.ei.executor;

import java.nio.file.Path;

import org.qubership.atp.ei.node.ImportExecutor;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.mia.ei.service.ImportStrategy;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtpMiaImportExecutor implements ImportExecutor {

    private final ImportStrategy importStrategy;

    @Override
    public void importData(ExportImportData importData, Path path) {
        log.info("Request for import with data: {}", importData);
        importStrategy.miaImport(importData, path);
        log.info("End export. Request {}", importData);
    }

    @Override
    public ValidationResult preValidateData(ExportImportData importData, Path workDir) {
        return null;
    }

    @Override
    public ValidationResult validateData(ExportImportData importData, Path workDir) {
        log.info("Request for validate with data: {}", importData);
        return importStrategy.validateData(importData, workDir);
    }
}
