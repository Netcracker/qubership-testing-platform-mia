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

package org.qubership.atp.mia.model.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.annotation.Nonnull;

import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWorkbook {

    @Nonnull
    final String filePath;
    @Nonnull
    final FileInputStream fileInputStream;
    @Nonnull
    final XSSFWorkbook workbook;
    @Nonnull
    final HashMap<IndexedColors, XSSFCellStyle> styles = new HashMap<>();

    /**
     * Opens Excel workbook for changes.
     *
     * @param filePath file path of workbook
     * @throws IOException in case problem open workbook
     */
    public ExcelWorkbook(@Nonnull String filePath) throws IOException {
        this.filePath = filePath;
        this.fileInputStream = new FileInputStream(new File(filePath));
        this.workbook = new XSSFWorkbook(fileInputStream);
    }

    @Nonnull
    public String getFilePath() {
        return filePath;
    }

    @Nonnull
    public FileInputStream getFileInputStream() {
        return fileInputStream;
    }

    @Nonnull
    public XSSFWorkbook getWorkbook() {
        return workbook;
    }

    /**
     * Gets styles value.
     *
     * @return styles
     */
    @Nonnull
    public HashMap<IndexedColors, XSSFCellStyle> getStyles() {
        return styles;
    }

    /**
     * Add style to map.
     */
    @Nonnull
    public void addStyle(IndexedColors color, XSSFCellStyle cellStyle) {
        styles.put(color, cellStyle);
    }
}

