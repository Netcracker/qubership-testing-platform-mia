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
import static org.qubership.atp.mia.utils.ExcelParserHelper.getAllCellsByPatternInRange;
import static org.qubership.atp.mia.utils.ExcelParserHelper.getCellColumnRange;
import static org.qubership.atp.mia.utils.ExcelParserHelper.getHeaderByPattern;

import java.util.HashMap;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import clover.org.apache.commons.lang3.Range;
import com.google.common.base.Strings;

public class EventExcelProjectFour extends EventExcelCustomer {

    EventExcelProjectFour(@Nonnull XSSFSheet sheet, @Nullable String scenario, @Nullable String testCase) {
        super(sheet, scenario, testCase);
        eventInfoRange = Range.between(4, 24);
        headerRowRange = Range.between(1, 3);
        attrNameRowNumber = 2;
        identifications = new HashMap<>();
        identifications.put("Priority", false);
        identifications.put(TC_ID, false);
        identifications.put(SCENARIO, false);
        identifications.put("Product", false);
    }

    @Override
    public void parse() throws IllegalArgumentException {
        EventExcelMetricService.metricsAggregateServiceStatic.eventFromExcelCallStarted();
        defineCellsWithData(TC_ID, testCase, SCENARIO, scenario);
        super.parse();
        EventExcelMetricService.metricsAggregateServiceStatic.eventFromExcelCallStarted();
    }

    @Override
    protected void defineCellsWithData(@Nonnull String parentHeaderName, @Nonnull String parentHeaderValue,
                                       @Nonnull String childHeaderName, @Nullable String childHeaderValue) {
        parentHeaderValue = Strings.isNullOrEmpty(parentHeaderValue) ? ".+?" : parentHeaderValue;
        final int parentColumn = getHeaderByPattern(parentHeaderName, headerRowRange, sheet).getColumnIndex();
        final Set<XSSFCell> parentCells = getAllCellsByPatternFromRow(parentHeaderValue, parentColumn,
                headerRowRange.getMaximum() + 1, sheet);
        if (!Strings.isNullOrEmpty(childHeaderValue)) {
            final XSSFCell childCell = getHeaderByPattern(childHeaderName, headerRowRange, sheet);
            parentCells.forEach(caseRowCell -> cellsWithData.addAll(getAllCellsByPatternInRange(childHeaderValue,
                    childCell.getColumnIndex(), getCellColumnRange(caseRowCell), sheet)));
        } else {
            cellsWithData.addAll(parentCells);
        }
    }
}
