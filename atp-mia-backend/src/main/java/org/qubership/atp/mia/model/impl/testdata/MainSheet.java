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

import java.util.LinkedList;
import java.util.List;

import javax.xml.ws.Holder;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.qubership.atp.mia.model.impl.testdata.parsed.Description;
import org.qubership.atp.mia.model.impl.testdata.parsed.Scenario;
import org.qubership.atp.mia.model.impl.testdata.parsed.ValidatedParameters;

import lombok.Data;

@Data
public class MainSheet {

    public static final String MAIN_SHEET_NAME = "Main";

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
        Sheet sheet = wb.getWorkbook().createSheet(MAIN_SHEET_NAME);
        //Headers
        final Row hRowBefore = sheet.createRow(12);
        final Row hRow = sheet.createRow(13);
        final Cell cellSce = hRowBefore.createCell(0);
        cellSce.setCellValue(MainSheetRecord.SCENARIO);
        cellSce.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        sheet.addMergedRegion(new CellRangeAddress(12, 13, 0, 0));
        final Cell cellDescr = hRowBefore.createCell(1);
        cellDescr.setCellValue(MainSheetRecord.DESCRIPTION);
        cellDescr.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        sheet.addMergedRegion(new CellRangeAddress(12, 13, 1, 1));
        final Holder<Integer> rowNum = new Holder<>(13);
        final Holder<Integer> cellNum = new Holder<>(1);
        if (!scenarios.isEmpty() && !scenarios.get(0).getDescriptions().isEmpty()) {
            Description descr = findDescriptionToWriteHeader(true, false, false, false);
            if (descr != null) {
                descr.getEventParams().forEach((k, v) -> hRow.createCell(++cellNum.value).setCellValue(k));
                int firstCellNumber = cellNum.value - descr.getEventParams().size() + 1;
                final Cell cell = hRowBefore.createCell(firstCellNumber);
                cell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
                cell.setCellValue("Event parameters");
                if (firstCellNumber < cellNum.value) {
                    sheet.addMergedRegion(new CellRangeAddress(rowNum.value - 1, rowNum.value - 1,
                            firstCellNumber, cellNum.value));
                }
            }
            descr = findDescriptionToWriteHeader(false, true, false, false);
            if (descr != null) {
                descr.getOtherParams().forEach((k, v) -> hRow.createCell(++cellNum.value).setCellValue(k));
                int firstCellNumber = cellNum.value - descr.getOtherParams().size() + 1;
                final Cell cell = hRowBefore.createCell(firstCellNumber);
                cell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
                cell.setCellValue("Other parameters");
                if (firstCellNumber < cellNum.value) {
                    sheet.addMergedRegion(new CellRangeAddress(rowNum.value - 1, rowNum.value - 1,
                            firstCellNumber, cellNum.value));
                }
            }
            descr = findDescriptionToWriteHeader(false, false, true, false);
            if (descr != null) {
                descr.getValidationParams().forEach((k, v) -> hRow.createCell(++cellNum.value).setCellValue(k));
                int firstCellNumber = cellNum.value - descr.getValidationParams().size() + 1;
                final Cell cell = hRowBefore.createCell(firstCellNumber);
                cell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
                cell.setCellValue("Parameters to validate");
                if (firstCellNumber < cellNum.value) {
                    sheet.addMergedRegion(new CellRangeAddress(rowNum.value - 1, rowNum.value - 1,
                            firstCellNumber, cellNum.value));
                }
            }
            descr = findDescriptionToWriteHeader(false, false, false, true);
            if (descr != null) {
                descr.getValidatedParams().forEach(v -> hRow.createCell(++cellNum.value).setCellValue(v.getKey()));
                int firstCellNumber = cellNum.value - descr.getValidatedParams().size() + 1;
                final Cell cell = hRowBefore.createCell(firstCellNumber);
                cell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
                cell.setCellValue("Validated parameters");
                if (firstCellNumber < cellNum.value) {
                    sheet.addMergedRegion(new CellRangeAddress(rowNum.value - 1, rowNum.value - 1,
                            firstCellNumber, cellNum.value));
                }
            }
            final Cell statusCell = hRowBefore.createCell(++cellNum.value);
            statusCell.setCellValue("VALIDATION STATUS");
            statusCell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
            sheet.addMergedRegion(new CellRangeAddress(rowNum.value - 1, rowNum.value, cellNum.value, cellNum.value));
            //Values
            scenarios.forEach(s -> {
                cellNum.value = -1;
                sheet.createRow(++rowNum.value).createCell(++cellNum.value).setCellValue(s.getName());
                s.getDescriptions().forEach(d -> {
                    final Row row = sheet.createRow(++rowNum.value);
                    cellNum.value = 0;
                    row.createCell(++cellNum.value).setCellValue(d.getName());
                    if (d.getEventParams() != null && !d.getEventParams().isEmpty()) {
                        d.getEventParams().forEach((k, v) -> row.createCell(++cellNum.value).setCellValue(v));
                    }
                    if (d.getOtherParams() != null && !d.getOtherParams().isEmpty()) {
                        d.getOtherParams().forEach((k, v) -> row.createCell(++cellNum.value).setCellValue(v));
                    }
                    if (d.getValidationParams() != null && !d.getValidationParams().isEmpty()) {
                        d.getValidationParams().forEach((k, v) -> row.createCell(++cellNum.value).setCellValue(v));
                    }
                    if (d.getValidatedParams() != null && !d.getValidatedParams().isEmpty()) {
                        d.getValidatedParams().forEach(v -> {
                            final Cell cell = row.createCell(++cellNum.value);
                            if (v.getState() != null) {
                                cell.setCellStyle(wb.getCellStyle(v.getState()));
                            }
                            cell.setCellValue(v.getValue());
                        });
                    }
                    //Status
                    if (d.isValidated()) {
                        final Cell cell = row.createCell(++cellNum.value);
                        cell.setCellStyle(wb.getCellStyle(d.isStatus() ? ValidatedParameters.State.PASSED :
                                ValidatedParameters.State.FAILED));
                        cell.setCellValue(d.isStatus() ? "SUCCESS" : "FAILED");
                    }
                });
            });
        }
    }

    /**
     * Find description in which should be  EventParams/OtherParams/ValidationParams/ValidatedParams.
     *
     * @param isEvent      check for EventParams
     * @param isOther      check for OtherParams
     * @param isValidation check for ValidationParams
     * @param isValidated  check for ValidatedParams
     * @return found description, NULL otherwise.
     */
    private Description findDescriptionToWriteHeader(boolean isEvent, boolean isOther, boolean isValidation,
                                                     boolean isValidated) {
        Description descr = null;
        for (Scenario s : scenarios) {
            if (descr != null) {
                break;
            }
            if (s.getDescriptions() != null) {
                for (Description d : s.getDescriptions()) {
                    if (isEvent && d.getEventParams() != null && !d.getEventParams().isEmpty()) {
                        descr = d;
                        break;
                    }
                    if (isOther && d.getOtherParams() != null && !d.getOtherParams().isEmpty()) {
                        descr = d;
                        break;
                    }
                    if (isValidation && d.getValidationParams() != null && !d.getValidationParams().isEmpty()) {
                        descr = d;
                        break;
                    }
                    if (isValidated && d.getValidatedParams() != null && !d.getValidatedParams().isEmpty()) {
                        descr = d;
                        break;
                    }
                }
            }
        }
        return descr;
    }
}
