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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.ws.Holder;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.qubership.atp.mia.model.impl.testdata.parsed.Scenario;

import lombok.Data;

@Data
public class TestDataSheet {

    public static final String TEST_DATA_SHEET_NAME = "Test Data";

    private List<MainSheetRecord> records;
    private LinkedList<Scenario> scenarios;

    /**
     * Add scenario.
     *
     * @param scenario scenario
     */
    public void addScenario(Scenario scenario) {
        if (scenarios == null) {
            this.scenarios = new LinkedList<>();
        }
        scenarios.add(scenario);
    }

    /**
     * Write sheet into workbook.
     *
     * @param wb workbook with styles
     */
    public void write(TestDataWorkbookStyles wb) {
        Sheet sheet = wb.getWorkbook().createSheet(TEST_DATA_SHEET_NAME);
        //Headers
        final Holder<Integer> rowNum = new Holder<>(-1);
        final Holder<Integer> cellNum = new Holder<>(-1);
        final Row headerRow = sheet.createRow(++rowNum.value);
        final Cell cellSce = headerRow.createCell(++cellNum.value);
        cellSce.setCellValue(MainSheetRecord.SCENARIO);
        cellSce.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        final Cell cellDescr = headerRow.createCell(++cellNum.value);
        cellDescr.setCellValue(MainSheetRecord.DESCRIPTION);
        cellDescr.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        if (scenarios != null && !scenarios.isEmpty() && !scenarios.get(0).getDescriptions().isEmpty()) {
            final LinkedHashMap<String, String> params = scenarios.get(0).getDescriptions().get(0).getOtherParams();
            if (!params.isEmpty()) {
                params.forEach((k, v) -> {
                    final Cell cell = headerRow.createCell(++cellNum.value);
                    cell.setCellValue(k);
                    cell.setCellStyle(wb.getCellStyle(IndexedColors.GREY_25_PERCENT));
                });
            }
            //Values
            scenarios.forEach(s -> {
                cellNum.value = -1;
                sheet.createRow(++rowNum.value).createCell(++cellNum.value).setCellValue(s.getName());
                s.getDescriptions().forEach(d -> {
                    final Row row = sheet.createRow(++rowNum.value);
                    cellNum.value = 0;
                    row.createCell(++cellNum.value).setCellValue(d.getName());
                    if (!d.getOtherParams().isEmpty()) {
                        d.getOtherParams().forEach((k, v) -> row.createCell(++cellNum.value).setCellValue(v));
                    }
                });
            });
        }
    }
}
