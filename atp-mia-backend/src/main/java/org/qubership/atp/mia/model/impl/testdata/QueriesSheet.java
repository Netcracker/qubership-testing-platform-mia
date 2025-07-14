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
import org.qubership.atp.mia.model.impl.testdata.parsed.Query;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class QueriesSheet {

    public static final String QUERIES_SHEET_NAME = "Validation Queries";

    private List<QueriesSheetRecord> records;
    private LinkedList<Query> queries;

    /**
     * Add query.
     *
     * @param query query
     */
    public void addQuery(Query query) {
        if (queries == null) {
            this.queries = new LinkedList<>();
        }
        queries.add(query);
    }

    /**
     * Write sheet into workbook.
     *
     * @param wb workbook with styles
     */
    public void write(TestDataWorkbookStyles wb) {
        Sheet sheet = wb.getWorkbook().createSheet(QUERIES_SHEET_NAME);
        //Headers
        final Holder<Integer> rowNum = new Holder<>(0);
        final Row headerRow = sheet.createRow(rowNum.value);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue(QueriesSheetRecord.QUERY_COLUMN);
        headerCell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        headerCell = headerRow.createCell(1);
        headerCell.setCellValue(QueriesSheetRecord.VALIDATE_COLUMN);
        headerCell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        headerCell = headerRow.createCell(2);
        headerCell.setCellValue(QueriesSheetRecord.VALIDATED_COLUMN);
        headerCell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        headerCell = headerRow.createCell(3);
        headerCell.setCellValue(QueriesSheetRecord.TYPE_COLUMN);
        headerCell.setCellStyle(wb.getCellStyle(IndexedColors.YELLOW));
        //Values
        if (queries != null && queries.size() > 0) {
            queries.forEach(q -> {
                Holder<Row> row = new Holder<>(sheet.createRow(++rowNum.value));
                row.value.createCell(0).setCellValue(q.getQuery());
                row.value.createCell(3).setCellValue(q.getType().toString());
                if (q.getValidateValue() != null && q.getValidateValue().size() > 0) {
                    int finalRowNum = rowNum.value + q.getValidateValue().size() - 1;
                    if (finalRowNum > rowNum.value) {
                        sheet.addMergedRegion(new CellRangeAddress(rowNum.value, finalRowNum, 0, 0));
                        sheet.addMergedRegion(new CellRangeAddress(rowNum.value, finalRowNum, 3, 3));
                    }
                    q.getValidateValue().forEach(validateValue -> {
                        row.value.createCell(1).setCellValue(validateValue.getValidateName());
                        row.value.createCell(2).setCellValue(validateValue.getValidatedName());
                        row.value = sheet.createRow(++rowNum.value);
                    });
                }
            });
        }
    }
}
