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

package org.qubership.atp.mia.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.ws.Holder;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qubership.atp.mia.model.impl.ExcelWorkbook;

import clover.com.google.common.base.Strings;
import clover.org.apache.commons.lang3.Range;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelParserHelper {

    /**
     * Gets Cell by header name in row.
     *
     * @param headerName header name
     * @param row        row number of cell
     * @return Cell by header name in row, NULL if header not found
     */
    @Nullable
    public static XSSFCell getCellByHeaderNameInRow(@Nonnull String headerName, @Nonnull XSSFRow row) {
        return getCellByHeaderNameInRow(headerName, 0, row);
    }

    /**
     * Gets Cell by header name in row.
     *
     * @param headerName   header name
     * @param headerRowNum row number of header
     * @param row          row number of cell
     * @return Cell by header name in row, NULL if header not found
     */
    @Nullable
    public static XSSFCell getCellByHeaderNameInRow(@Nonnull String headerName, int headerRowNum,
                                                    @Nonnull XSSFRow row) {
        final Holder<XSSFCell> cellHolder = new Holder<>(null);
        row.forEach(cell -> {
            final XSSFRow rowHeader = row.getSheet().getRow(headerRowNum);
            if (rowHeader.getCell(cell.getColumnIndex()) != null
                    && rowHeader.getCell(cell.getColumnIndex()).getCellType() == CellType.STRING
                    && rowHeader.getCell(cell.getColumnIndex()).getStringCellValue().equalsIgnoreCase(headerName)) {
                cellHolder.value = (XSSFCell) cell;
                return;
            }
        });
        return cellHolder.value;
    }

    /**
     * Gets Cell header of the provided Cell.
     *
     * @param headerRowNum row number of header
     * @param cell         cell
     * @return Cell header
     */
    public static XSSFCell getHeaderByCell(int headerRowNum, @Nonnull XSSFCell cell) {
        return cell.getRow().getSheet().getRow(headerRowNum).getCell(cell.getColumnIndex());
    }

    /**
     * Gets header by pattern in Row 0.
     *
     * @param pattern pattern of header
     * @param sheet   sheet
     * @return header cell, NULL otherwise
     */
    @Nullable
    public static XSSFCell getHeaderByPattern(@Nonnull String pattern, @Nonnull XSSFSheet sheet) {
        return getHeaderByPattern(pattern, 0, sheet);
    }

    /**
     * Gets header by pattern.
     *
     * @param pattern        pattern of header
     * @param headerRowRange header row range
     * @param sheet          sheet
     * @return header cell, NULL otherwise
     */
    @Nullable
    public static XSSFCell getHeaderByPattern(@Nonnull String pattern, Range<Integer> headerRowRange,
                                              @Nonnull XSSFSheet sheet) {
        XSSFCell cell;
        int rowId = headerRowRange.getMinimum();
        do {
            cell = getHeaderByPattern(pattern, rowId, sheet);
            rowId++;
        } while (cell == null && rowId <= headerRowRange.getMaximum());
        return cell;
    }

    /**
     * Gets header by pattern.
     *
     * @param pattern      pattern of header
     * @param headerRowNum header row number
     * @param sheet        sheet
     * @return header cell, NULL otherwise
     */
    @Nullable
    public static XSSFCell getHeaderByPattern(@Nonnull String pattern, int headerRowNum, @Nonnull XSSFSheet sheet) {
        final Holder<XSSFCell> cellHolder = new Holder<>(null);
        sheet.getRow(headerRowNum).forEach(c -> {
            final XSSFCell cell = (XSSFCell) c;
            if (getCellValue(cell).matches(pattern)) {
                cellHolder.value = cell;
                return;
            }
        });
        return cellHolder.value;
    }

    /**
     * Gets header by pattern in column Range.
     *
     * @param pattern      header pattern
     * @param headerRowNum header row number
     * @param sheet        sheet
     * @param range        columns range
     * @return Cell if found, NULL otherwise
     */
    @Nullable
    public static XSSFCell getHeaderByPatternInRange(@Nonnull String pattern,
                                                     int headerRowNum,
                                                     @Nonnull XSSFSheet sheet,
                                                     @Nonnull Range<Integer> range) {
        final Holder<XSSFCell> cellHolder = new Holder<>(null);
        sheet.getRow(headerRowNum).forEach(c -> {
            final XSSFCell cell = (XSSFCell) c;
            if (range.contains(cell.getColumnIndex()) && getCellValue(cell).matches(pattern)) {
                cellHolder.value = cell;
                return;
            }
        });
        return cellHolder.value;
    }

    /**
     * Gets Cell by parent and child headers in row.
     *
     * @param parentHeaderName   parent header name
     * @param parentHeaderRowNum parent header row number
     * @param childHeaderName    childheader name
     * @param childHeaderRowNum  child eader row number
     * @param row                row
     * @return Cell if parent and child headers found, NULL otherwise
     */
    @Nullable
    public static XSSFCell getCellByParentAndChildHeadersInRow(@Nonnull String parentHeaderName, int parentHeaderRowNum,
                                                               @Nonnull String childHeaderName, int childHeaderRowNum,
                                                               @Nonnull XSSFRow row) {
        XSSFCell header = getHeaderByPattern(parentHeaderName, parentHeaderRowNum, row.getSheet());
        if (header != null) {
            XSSFCell childHeaderCell = getHeaderByPatternInRange(childHeaderName, childHeaderRowNum, row.getSheet(),
                    getCellRowRange(header));
            return row.getCell(childHeaderCell.getColumnIndex());
        }
        return null;
    }

    /**
     * Gets Cell by column number and pattern.
     *
     * @param pattern      pattern
     * @param columnNumber column number
     * @param sheet        sheet
     * @return Cell if pattern matched in column, NULL otherwise
     */
    @Nullable
    public static XSSFCell getCellByPattern(@Nonnull String pattern, int columnNumber, @Nonnull XSSFSheet sheet) {
        final Iterator rowIterator = sheet.rowIterator();
        XSSFCell cell;
        do {
            if (!rowIterator.hasNext()) {
                return null;
            }
            XSSFRow row = (XSSFRow) rowIterator.next();
            cell = row.getCell(columnNumber);
        } while (!getCellValue(cell).matches(pattern));
        return cell;
    }

    /**
     * Gets Cell in row and column.
     *
     * @param rowNumber    row number
     * @param columnNumber column number
     * @param sheet        sheet
     * @return Cell in row and column
     */
    @Nullable
    public static XSSFCell getCell(int rowNumber, int columnNumber, @Nonnull XSSFSheet sheet) {
        final Holder<XSSFCell> cell = new Holder<>(null);
        final XSSFRow row = sheet.getRow(rowNumber);
        if (cell.value == null) {
            sheet.getMergedRegions().forEach(mergedRegion -> {
                if (mergedRegion.isInRange(rowNumber, columnNumber)) {
                    cell.value = sheet.getRow(mergedRegion.getFirstRow()).getCell(mergedRegion.getFirstColumn());
                }
            });
        }
        if (cell.value == null && row != null) {
            cell.value = row.getCell(columnNumber);
        }
        return cell.value;
    }

    /**
     * Gets Cell in row and column. Create cell if absent.
     *
     * @param rowNumber    row number
     * @param columnNumber column number
     * @param sheet        sheet
     * @return Cell in row and column
     */
    @Nullable
    public static XSSFCell getCellAnyway(int rowNumber, int columnNumber, @Nonnull XSSFSheet sheet) {
        XSSFRow row = sheet.getRow(rowNumber);
        if (row == null) {
            row = sheet.createRow(rowNumber);
        }
        XSSFCell cell = row.getCell(columnNumber);
        if (cell == null) {
            cell = row.createCell(columnNumber);
        }
        return cell;
    }

    /**
     * Gets cell by pattern in range.
     *
     * @param pattern      pattern
     * @param columnNumber column number
     * @param rowRange     row range
     * @param sheet        sheet
     * @return Cell pattern found, NULL otherwise
     */
    @Nullable
    public static XSSFCell getCellByPatternInRange(@Nonnull String pattern, int columnNumber,
                                                   @Nonnull Range<Integer> rowRange, @Nonnull XSSFSheet sheet) {
        XSSFCell cell = null;
        for (int rowId = rowRange.getMinimum(); rowId <= rowRange.getMaximum(); rowId++) {
            XSSFCell curCell = sheet.getRow(rowId).getCell(columnNumber);
            if (getCellValue(curCell).matches(pattern)) {
                cell = curCell;
                break;
            }
        }
        return cell;
    }

    /**
     * Gets all Cells by pattern in column.
     *
     * @param pattern      pattern
     * @param columnNumber column number
     * @param sheet        sheet
     * @return Cells by pattern in column
     */
    @Nonnull
    public static Set<XSSFCell> getAllCellsByPattern(@Nonnull String pattern, int columnNumber,
                                                     @Nonnull XSSFSheet sheet) {
        Iterator<Row> rowIterator = sheet.rowIterator();
        final Set<XSSFCell> cells = new LinkedHashSet<>();
        while (rowIterator.hasNext()) {
            XSSFRow row = (XSSFRow) rowIterator.next();
            final XSSFCell cell = row.getCell(columnNumber);
            if (cell != null) {
                final String value = getCellValue(cell);
                if (value.matches(pattern)) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }

    /**
     * Gets all cells by pattern in range.
     *
     * @param pattern      pattern
     * @param columnNumber column number
     * @param rowRange     row range
     * @param sheet        sheet
     * @return all cell by pattern, NULL otherwise
     */
    @Nonnull
    public static Set<XSSFCell> getAllCellsByPatternInRange(@Nonnull String pattern,
                                                            int columnNumber,
                                                            @Nonnull Range<Integer> rowRange,
                                                            @Nonnull XSSFSheet sheet) {
        final Set<XSSFCell> cells = new LinkedHashSet<>();
        for (int rowId = rowRange.getMinimum(); rowId <= rowRange.getMaximum(); rowId++) {
            XSSFCell curCell = getCell(rowId, columnNumber, sheet);
            if (getCellValue(curCell).matches(pattern)) {
                cells.add(curCell);
            }
        }
        return cells;
    }

    /**
     * Gets all cells by pattern in range.
     *
     * @param pattern        pattern
     * @param columnNumber   column number
     * @param startRowNumber start row number
     * @param sheet          sheet
     * @return all cell by pattern, NULL otherwise
     */
    @Nonnull
    public static Set<XSSFCell> getAllCellsByPatternFromRow(@Nonnull String pattern, int columnNumber,
                                                            int startRowNumber, @Nonnull XSSFSheet sheet) {
        final Set<XSSFCell> cells = new LinkedHashSet<>();
        for (int rowId = startRowNumber; rowId <= sheet.getLastRowNum(); rowId++) {
            XSSFCell curCell = getCell(rowId, columnNumber, sheet);
            if (getCellValue(curCell).matches(pattern)) {
                cells.add(curCell);
            }
        }
        return cells;
    }

    /**
     * Gets range of rows in merged column.
     *
     * @param cell cell
     * @return Range of rows in merged column
     */
    public static Range<Integer> getCellColumnRange(@Nonnull XSSFCell cell) {
        Range<Integer> returnValue = Range.between(cell.getRowIndex(), cell.getRowIndex());
        final XSSFSheet sheet = cell.getSheet();
        for (int mergedRegionsId = 0; mergedRegionsId < sheet.getNumMergedRegions(); ++mergedRegionsId) {
            final CellRangeAddress mergedRegion = sheet.getMergedRegion(mergedRegionsId);
            if (mergedRegion.isInRange(cell.getRowIndex(), cell.getColumnIndex())) {
                returnValue = Range.between(mergedRegion.getFirstRow(), mergedRegion.getLastRow());
                break;
            }
        }
        return returnValue;
    }

    /**
     * Gets range of columns in merged row.
     *
     * @param cell cell
     * @return Range of columns in merged row
     */
    @Nonnull
    public static Range<Integer> getCellRowRange(@Nonnull XSSFCell cell) {
        final Holder<Range<Integer>> range = new Holder<>(Range.between(cell.getColumnIndex(), cell.getColumnIndex()));
        final XSSFSheet sheet = cell.getSheet();
        sheet.getMergedRegions().forEach(mergedRegion -> {
            if (mergedRegion.isInRange(cell.getRowIndex(), cell.getColumnIndex())) {
                range.value = Range.between(mergedRegion.getFirstColumn(), mergedRegion.getLastColumn());
                return;
            }
        });
        return range.value;
    }

    /**
     * Gets workbook.
     *
     * @param path path to excel file
     * @returnworkbook
     */
    @Nonnull
    public static ExcelWorkbook getWorkBook(@Nonnull String path) {
        try {
            return new ExcelWorkbook(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("File is not found or can't read: " + path, e);
        }
    }

    /**
     * Gets sheet of excel file.
     *
     * @param workbook  workbook
     * @param sheetName sheet name
     * @return sheet of excel file
     * @throws IllegalArgumentException in case problem with file or sheet
     */
    @Nonnull
    public static XSSFSheet getSheet(@Nonnull XSSFWorkbook workbook, @Nullable String sheetName)
            throws IllegalArgumentException {
        XSSFSheet sheet;
        if (workbook.getNumberOfSheets() > 0) {
            if (Strings.isNullOrEmpty(sheetName)) {
                sheet = workbook.getSheetAt(0);
            } else {
                try {
                    sheet = workbook.getSheet(sheetName);
                } catch (Exception e1) {
                    throw new IllegalArgumentException("Error reading sheet " + sheetName);
                }
            }
        } else {
            throw new IllegalArgumentException("No sheet in Excel file");
        }
        return sheet;
    }

    @Nonnull
    public static XSSFSheet getSheet(@Nonnull ExcelWorkbook workbook, @Nullable String sheetName)
            throws IllegalArgumentException {
        return getSheet(workbook.getWorkbook(), sheetName);
    }

    /**
     * Gets sheet of excel file.
     *
     * @param path      path to excel file
     * @param sheetName sheet name
     * @return sheet of excel file
     * @throws IllegalArgumentException in case problem with file or sheet
     */
    @Nonnull
    public static XSSFSheet getSheet(Path path, @Nullable String sheetName) throws IllegalArgumentException {
        return getSheet(getWorkBook(path.toString()).getWorkbook(), sheetName);
    }

    /**
     * Gets cell value.
     *
     * @param cell cell
     * @return cell value
     */
    @Nonnull
    public static String getCellValue(@Nullable Cell cell) {
        return getCellValue((XSSFCell) cell);
    }

    /**
     * Gets cell value.
     *
     * @param cell cell
     * @return cell value
     */
    @Nonnull
    public static String getCellValue(@Nullable XSSFCell cell) {
        if (cell != null) {
            try {
                Holder<XSSFCell> holderCell = new Holder<>(cell);
                final XSSFSheet sheet = cell.getSheet();
                sheet.getMergedRegions().forEach(mergedRegion -> {
                    if (mergedRegion.isInRange(cell.getRowIndex(), cell.getColumnIndex())) {
                        holderCell.value = getCell(mergedRegion.getFirstRow(), mergedRegion.getFirstColumn(), sheet);
                    }
                });
                return convertCellValue(holderCell.value, holderCell.value.getCellType()).trim();
            } catch (Exception e) {
                log.error("Error getting cell value [{},{}]: {}", cell.getRowIndex(), cell.getColumnIndex(),
                        e.getMessage());
            }
        }
        return "";
    }

    /**
     * Converts cell value by cell type.
     *
     * @param cell     cell
     * @param cellType cell type
     * @return cell value as string according to cell type
     */
    private static String convertCellValue(Cell cell, CellType cellType) {
        String value = "";
        switch (cellType) {
            case NUMERIC:
                value = decimalFormat(cell.getNumericCellValue());
                if (DateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/YYYY");
                    value = sdf.format(cell.getDateCellValue());
                }
                break;
            case STRING:
                value = cell.getStringCellValue();
                break;
            case FORMULA:
                value = convertCellValue(cell, cell.getCachedFormulaResultType());
                break;
            default:
                break;
        }
        return value;
    }

    /**
     * Gets decimal format.
     *
     * @param value value
     * @return decimal format of double
     */
    public static String decimalFormat(Double value) {
        DecimalFormat format = new DecimalFormat("###.#####");
        return format.format(value);
    }
}
