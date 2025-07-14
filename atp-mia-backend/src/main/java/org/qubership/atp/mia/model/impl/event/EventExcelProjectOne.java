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

package org.qubership.atp.mia.model.impl.event;

import static org.qubership.atp.mia.utils.ExcelParserHelper.getAllCellsByPatternFromRow;
import static org.qubership.atp.mia.utils.ExcelParserHelper.getCell;
import static org.qubership.atp.mia.utils.ExcelParserHelper.getCellValue;
import static org.qubership.atp.mia.utils.ExcelParserHelper.getHeaderByPattern;

import java.util.HashMap;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import clover.org.apache.commons.lang3.Range;
import com.google.common.base.Strings;

public class EventExcelProjectOne extends EventExcelCustomer {

    private static final String DESCRIPTION = "Description";

    EventExcelProjectOne(@Nonnull XSSFSheet sheet, @Nullable String scenario, @Nullable String testCase) {
        super(sheet, scenario, testCase);
        eventInfoRange = Range.between(2, 15);
        headerRowRange = Range.between(1, 16);
        attrNameRowNumber = 16;
        identifications = new HashMap<>();
        identifications.put(SCENARIO, false);
        identifications.put(DESCRIPTION, false);
    }

    @Override
    public void parse() throws IllegalArgumentException {
        EventExcelMetricService.metricsAggregateServiceStatic.eventFromExcelCallStarted();
        defineCellsWithData(SCENARIO, scenario, DESCRIPTION, testCase);
        super.parse();
    }

    @Override
    protected void defineCellsWithData(@Nonnull String parentHeaderName, @Nonnull String parentHeaderValue,
                                       @Nonnull String childHeaderName, @Nullable String childHeaderValue) {
        parentHeaderValue = Strings.isNullOrEmpty(parentHeaderValue) ? ".+?" : parentHeaderValue;
        final int parentColumn = getHeaderByPattern(parentHeaderName, headerRowRange, sheet).getColumnIndex();
        final Set<XSSFCell> parentCells = getAllCellsByPatternFromRow(parentHeaderValue, parentColumn,
                headerRowRange.getMaximum() + 1, sheet);
        parentCells.forEach(pCell -> {
            final int childColumn = getHeaderByPattern(childHeaderName, headerRowRange, sheet).getColumnIndex();
            for (int rowIdx = pCell.getRowIndex() + 1; ; rowIdx++) {
                final XSSFCell dataCell = getCell(rowIdx, childColumn, sheet);
                final String dataValue = getCellValue(dataCell);
                if (Strings.isNullOrEmpty(dataValue)) {
                    break;
                }
                if (Strings.isNullOrEmpty(childHeaderValue)) {
                    cellsWithData.add(dataCell);
                } else {
                    if (dataValue.matches(childHeaderValue)) {
                        cellsWithData.add(dataCell);
                    }
                }
            }
        });
    }
}
