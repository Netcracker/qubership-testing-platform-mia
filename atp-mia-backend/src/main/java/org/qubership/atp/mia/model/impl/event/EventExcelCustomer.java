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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.qubership.atp.mia.utils.ExcelParserHelper;

import clover.com.google.common.base.Strings;
import clover.org.apache.commons.lang3.Range;

public abstract class EventExcelCustomer {

    protected static final String EVENT_INFO = "Event Info";
    protected static final String TC_ID = "TC ID";
    protected static final String SCENARIO = "Scenario";

    @Nonnull
    final XSSFSheet sheet;
    @Nullable
    final String scenario;
    @Nullable
    final String testCase;
    @Nonnull
    final List<XSSFCell> cellsWithData = new ArrayList<>();
    @Nonnull
    Map<String, List<String>> attrNameAndValue = new HashMap<>();
    @Nonnull
    HashMap<String, Boolean> identifications = new HashMap<>();
    @Nonnull
    Range<Integer> headerRowRange = Range.between(0, 0);
    @Nonnull
    Range<Integer> eventInfoRange = Range.between(0, 0);
    int attrNameRowNumber = 0;

    EventExcelCustomer(@Nonnull XSSFSheet sheet, @Nullable String scenario, @Nullable String testCase) {
        this.sheet = sheet;
        this.scenario = scenario;
        this.testCase = testCase;
    }

    Map<String, List<String>> getAttrNameAndValue() {
        return attrNameAndValue;
    }

    /**
     * Check if format of sheet related to provided Customer.
     *
     * @return true if all headers for identification is found in row
     */
    public boolean isCurrentFormat() {
        for (int rowId = headerRowRange.getMinimum(); rowId <= headerRowRange.getMaximum(); rowId++) {
            final Row row = sheet.getRow(rowId);
            if (row != null) {
                row.forEach(cell -> {
                    final String cellValue = ExcelParserHelper.getCellValue(cell);
                    if (cell != null && identifications.containsKey(cellValue)) {
                        identifications.put(cellValue, true);
                    }
                });
            }
        }
        return identifications.values().stream().allMatch(Boolean::booleanValue);
    }

    /**
     * Parses Excel file.
     *
     * @throws IllegalArgumentException in case parsing problem
     */
    public void parse() throws IllegalArgumentException {
        final XSSFCell eventInfoCell = ExcelParserHelper.getHeaderByPattern(EVENT_INFO, headerRowRange, sheet);
        if (eventInfoCell != null) {
            eventInfoRange = ExcelParserHelper.getCellRowRange(eventInfoCell);
        }
        for (int columnId = eventInfoRange.getMinimum(); columnId <= eventInfoRange.getMaximum(); columnId++) {
            String attrName = ExcelParserHelper.getCellValue(ExcelParserHelper.getCell(attrNameRowNumber, columnId,
                    sheet)).replaceAll("\\W", "");
            if (Strings.isNullOrEmpty(attrName)) {
                continue;
            }
            attrNameAndValue.put(attrName, new ArrayList<>());
            final List<String> attValues = attrNameAndValue.get(attrName);
            for (XSSFCell cell : cellsWithData) {
                final Range<Integer> rowRange = ExcelParserHelper.getCellColumnRange(cell);
                for (int rowId = rowRange.getMinimum(); rowId <= rowRange.getMaximum(); rowId++) {
                    attValues.add(ExcelParserHelper.getCellValue(ExcelParserHelper.getCell(rowId, columnId, sheet)));
                }
            }
        }
    }

    /**
     * Defines cell with data for Event generation.
     *
     * @param parentHeaderName  parent header name
     * @param parentHeaderValue parent header value
     * @param childHeader       child header name
     * @param childHeaderValue  child header value
     */
    protected abstract void defineCellsWithData(@Nonnull String parentHeaderName, @Nonnull String parentHeaderValue,
                                                @Nonnull String childHeader, @Nullable String childHeaderValue);
}
