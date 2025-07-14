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

package org.qubership.atp.mia.model.impl.testdata;

import java.util.HashMap;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.qubership.atp.mia.model.impl.testdata.parsed.ValidatedParameters;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TestDataWorkbookStyles implements AutoCloseable {

    private final SXSSFWorkbook workbook;
    private HashMap<IndexedColors, CellStyle> styles;

    /**
     * Gets style for Cell.
     *
     * @param isPassed if style of cell should be PASSED
     * @return cell style
     */
    public CellStyle getCellStyle(ValidatedParameters.State isPassed) {
        return getCellStyle(isPassed.toString().equals("PASSED") ? IndexedColors.GREEN : isPassed.toString().equals(
                "FAILED") ? IndexedColors.RED : IndexedColors.GREY_40_PERCENT);
    }

    /**
     * Gets style for Cell.
     *
     * @param fillColor color to fill cell
     * @return cell style
     */
    public CellStyle getCellStyle(IndexedColors fillColor) {
        if (styles == null) {
            styles = new HashMap<>();
        }
        if (!styles.containsKey(fillColor)) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(fillColor.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styles.put(fillColor, style);
        }
        return styles.get(fillColor);
    }

    @Override
    public void close() throws Exception {
        workbook.dispose();
    }
}
