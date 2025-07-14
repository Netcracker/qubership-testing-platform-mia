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

package org.qubership.atp.mia.ei.service;

import java.nio.file.Path;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;

public interface ImportStrategy {

    /**
     * Get Export format.
     *
     * @return ExportFormat
     */
    ExportFormat getFormat();

    /**
     * Import data.
     *
     * @param importData import data model.
     * @param path       path
     */
    void miaImport(ExportImportData importData, Path path);

    /**
     * Validate imported data on name duplicates.
     *
     * @return ValidationResult
     */
    ValidationResult validateData(ExportImportData importData, Path workDir);
}
