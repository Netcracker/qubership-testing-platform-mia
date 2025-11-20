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

package org.qubership.atp.mia.repo.impl;

import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.ws.Holder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.qubership.atp.mia.component.QueryDriverFactory;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotCreateFileFailedException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotFileToWriteNotFoundException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotHeaderTypeUnsupportedException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotIoException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotProcessStatusMissedException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotResultsSavingException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotSessionNotFoundException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotStepListEmptyException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotTemplateCanNotBeClosedException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotTemplateNotFoundOnPathException;
import org.qubership.atp.mia.exceptions.proofoftesting.PotWriteFailedException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.PotHeader;
import org.qubership.atp.mia.model.impl.executable.PotHeaderType;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.Marker;
import org.qubership.atp.mia.model.pot.PotSessionException;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;
import org.qubership.atp.mia.model.pot.entity.PotExecutionStep;
import org.qubership.atp.mia.model.pot.entity.PotSession;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.execution.RecordingSessionsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ProofOfTestingRepository {

    private static final String RED = "fd483d";
    private static final String GREEN = "008000";
    private static final String YELLOW = "ffff00";
    private static final String YELLOW_STATUS = "ee9d30";
    private static final String GREY = "8b8d90";
    private static final String BLUE = "0000FF";
    private static final String POT_DOCX = "POT_%s.docx";
    private static final String POT_ARCHIVE = "POT_archive_%s.zip";
    private static final String shrinkSymbol = "...";
    private final ShellRepository shellRepository;
    private final MiaFileService miaFileService;
    private final QueryDriverFactory queryDriverFactory;
    private final MiaContext miaContext;
    private final RecordingSessionsService recordingSessionsService;
    private final String fileDownloadPrefix;
    @Value("${mia.pot.minLogLength:5}")
    private int minimumAmountOfLinesInFileToPrint;
    @Value("${mia.pot.maxLinesLogs:1000}")
    private int maxPrintedLines;
    @Value("${mia.pot.maxTableRows}")
    private int maxPrintedLinesSqlTable;
    private int printedLines = 0;

    /**
     * Download proof of testing steps to resulting docx file and generates archive with this file and other output.
     *
     * @return link to output archive
     */
    public List<Link> downloadProofOfTesting() {
        UUID sessionId = miaContext.getFlowData().getSessionId();
        log.info("Start save POT for [" + sessionId + "] session");
        Path parentPath = miaContext.getLogPath();
        File potDocxFile = parentPath.resolve(String.format(POT_DOCX, sessionId)).toFile();
        File potArchiveFile = parentPath.resolve(String.format(POT_ARCHIVE, sessionId)).toFile();
        Optional<PotSession> session = recordingSessionsService.getSession(sessionId);
        if (session.isPresent()) {
            if (!parentPath.toFile().exists()) {
                try {
                    Files.createDirectories(parentPath);
                } catch (IOException e) {
                    throw new PotCreateFileFailedException(parentPath.toString(), e);
                }
            }
            List<Link> result = saveProofOfTesting(session.get(), potDocxFile);
            log.info("POT saved for [" + sessionId + "] session");
            return result;
        } else {
            List<Link> resultFiles = new ArrayList<>();
            try {
                miaFileService.getFile(potDocxFile);
                resultFiles.add(miaContext.getLogLinkOnUi(potDocxFile.getAbsolutePath()));
            } catch (Exception e) {
                try {
                    miaFileService.getFile(potArchiveFile);
                    resultFiles.add(miaContext.getLogLinkOnUi(potArchiveFile.getAbsolutePath()));
                } catch (Exception e1) {
                    throw new PotSessionNotFoundException();
                }
            }
            return resultFiles;
        }
    }

    /**
     * Save recording session to word file.
     *
     * @return link for download file
     */
    public List<Link> saveProofOfTesting(PotSession session, File targetFile) {
        if (session == null || session.getId() == null) {
            throw new PotSessionNotFoundException();
        }
        if (!targetFile.getAbsolutePath().endsWith("docx")) {
            throw new RuntimeException("You should use docx file");
        }
        List<PotExecutionStep> stepList = session.getPotExecutionSteps();
        if (stepList == null || stepList.size() < 1) {
            throw new PotStepListEmptyException();
        }

        /*Path baseDir = miaContext.getLogPath().toAbsolutePath().normalize();
        validatePathTraversal(targetFile.toPath(), baseDir);*/

        List<String> filePaths = new ArrayList<>();
        //Blank Document
        File potTemplate = miaContext.getPotTemplate();
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(potTemplate))) {
            try (FileOutputStream out = new FileOutputStream(targetFile)) {
                document.createParagraph().setPageBreak(true);
                printHeaders(document, session);
                //create Paragraph
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setStyle("Heading1");
                XWPFRun run = paragraph.createRun();
                run.setText("Table of Contents");
                XWPFParagraph paragraphWithContent = document.createParagraph();
                paragraphWithContent.createRun();
                int bookmarkId = 0;
                List<String> stepsTitles = new ArrayList<>();
                for (PotExecutionStep step : stepList) {
                    stepsTitles.add(printExecutionStep(document, step, filePaths, bookmarkId));
                    bookmarkId++;
                }
                bookmarkId = 0;
                for (int i = 0; i < stepList.size(); i++) {
                    String anchor = stepList.get(i)
                            .getStepName().replace(" ", "_") + "_" + bookmarkId;
                    XWPFHyperlinkRun hyperLinkRun = createHyperlinkRunToAnchor(paragraphWithContent, anchor);
                    hyperLinkRun.setText(stepsTitles.get(i));
                    hyperLinkRun.setColor(BLUE);
                    paragraphWithContent.createRun().addBreak();
                    bookmarkId++;
                }
                if (document.getParagraphs().size() > 3) {
                    while (document.getParagraphs().get(0).getText().startsWith("Heading")) {
                        document.removeBodyElement(0);
                    }
                }
                document.write(out);
                log.trace("Written document");
            } catch (FileNotFoundException e) {
                throw new PotFileToWriteNotFoundException(targetFile.toString(), e);
            } catch (IOException e) {
                throw new PotWriteFailedException(targetFile.toString(), e);
            }
        } catch (FileNotFoundException e) {
            throw new PotTemplateNotFoundOnPathException(potTemplate.toString(), e);
        } catch (IOException e) {
            throw new PotTemplateCanNotBeClosedException(potTemplate.toString(), e);
        } catch (Exception e) {
            throw new PotResultsSavingException(e);
        }
        List<Link> resultFiles = new ArrayList<>();
        if (filePaths.size() > 0) {
            filePaths.add(targetFile.getAbsolutePath());
            Path archivePath = targetFile.getParentFile().toPath().resolve(String.format(POT_ARCHIVE, session.getId()));

            //validatePathTraversal(archivePath, baseDir);

            Link link = miaContext.zipCommandOutputs(filePaths, archivePath);
            resultFiles.add(link);
            FileMetaData fileMetaData = FileMetaData.log(miaContext.getProjectId(), archivePath.toFile().getPath());
            miaFileService.saveLogFile(fileMetaData, archivePath.toFile());
        } else {
            FileMetaData fileMetaData = FileMetaData.log(miaContext.getProjectId(), targetFile.getPath());
            resultFiles.add(miaContext.getLogLinkOnUi(targetFile.getAbsolutePath()));
            miaFileService.saveLogFile(fileMetaData, targetFile);
        }
        resultFiles.forEach(link -> {
            miaFileService.getFile(link.getPath());
            link.setPath(fileDownloadPrefix + link.getPath());
        });
        return resultFiles;
    }

    private void validatePathTraversal(Path filePath, Path baseDir) {
        Path normalizedPath = filePath.toAbsolutePath().normalize();
        if (!normalizedPath.startsWith(baseDir)) {
            throw new SecurityException("Invalid path: Path traversal attempt detected: " + normalizedPath);
        }
    }

    private boolean analyseAndPrintTable(XWPFDocument document, String content, TableMarkerResult tableMarkerResult) {
        assertTableRowCount(document, tableMarkerResult);
        if (tableMarkerResult.getColumnStatuses() == null || tableMarkerResult.getColumnStatuses().size() < 1) {
            return printTable(document, content);
        } else {
            XWPFRun run;
            XWPFTable table;
            XWPFTableRow row;
            XWPFTableCell cell;
            final TableContentInfo tci = new TableContentInfo(content);
            printTableDescription(tci, document);
            for (int i = 0; i < (int) Math.ceil((double) tci.countOfColumns / tci.columnsInString); i++) {
                table = document.createTable();
                row = setTableStyleAndCreateRow(table);
                List<String> columnStatuses = new ArrayList<>();
                //add columnName
                for (int j = 0; j < tci.columnsInString && i * tci.columnsInString + j < tci.countOfColumns; j++) {
                    cell = row.createCell();
                    run = getRunForColumnNameCell(cell);
                    String columnName = tci.columnNames[i * tci.columnsInString + j];
                    run.setText(columnName);
                    if (tableMarkerResult.getColumnStatuses().stream().anyMatch(s ->
                            s.getColumnName().equalsIgnoreCase(columnName))) {
                        String columnStatus = String.valueOf(tableMarkerResult.getColumnStatuses().stream().filter(s ->
                                s.getColumnName().equalsIgnoreCase(columnName)).findFirst().get().getStatus());
                        columnStatuses.add(columnStatus);
                    } else {
                        columnStatuses.add("");
                    }
                }
                //add Data
                printTableRow(table, Arrays.asList(tci.tableRows[1].split(",", -1)), tci.columnsInString,
                        tci.countOfColumns, i);
                List<String> data = Arrays.asList(tci.tableRows[2].split(",", -1));
                row = table.createRow();
                for (int j = 0; j < tci.columnsInString && i * tci.columnsInString + j < tci.countOfColumns; j++) {
                    cell = row.getCell(j);
                    if (columnStatuses.get(j).equals(Statuses.SUCCESS.toString())) {
                        cell.setColor(GREEN);
                    } else if (columnStatuses.get(j).equals(Statuses.FAIL.toString())) {
                        cell.setColor(RED);
                    }
                    run = getRunForCell(cell);
                    run.setText(data.get(i * tci.columnsInString + j));
                }
                document.createParagraph();
            }
            document.createParagraph();
            return false;
        }
    }

    private boolean analyseAndPrintTextFile(XWPFParagraph paragraph, LineIterator it, Marker logMarkerEntity) {
        boolean shouldWePrintFile = false;
        XWPFRun run;
        int amountLinesToPrint = getMinimumAmountOfLinesInFileToPrint();
        int i = 0;
        try {
            while (it.hasNext()) {
                String str;
                if (i++ == amountLinesToPrint) {
                    shouldWePrintFile = true;
                    run = paragraph.createRun();
                    run.setFontSize(8);
                    run.setText(shrinkSymbol);
                    run.addBreak();
                    break;
                }
                str = it.next();
                run = paragraph.createRun();
                run.setFontSize(8);
                if (logMarkerEntity != null) {
                    if (logMarkerEntity.getPassedMarkerForLog() != null && logMarkerEntity.getPassedMarkerForLog()
                            .stream().anyMatch(passMarker -> Pattern.compile(passMarker).matcher(str).find())) {
                        run.setColor(GREEN);
                    } else if (logMarkerEntity.getFailedMarkersForLog() != null && logMarkerEntity
                            .getFailedMarkersForLog().stream().anyMatch(failMarker ->
                                    Pattern.compile(failMarker).matcher(str).find())) {
                        run.setColor(RED);
                    } else if (logMarkerEntity.getWarnMarkersForLog() != null && logMarkerEntity
                            .getWarnMarkersForLog().stream().anyMatch(warnMarker ->
                                    Pattern.compile(warnMarker).matcher(str).find())) {
                        run.setColor(YELLOW);
                    }
                }
                run.setText(str);
                run.addBreak();
                printedLines++;
            }
        } finally {
            LineIterator.closeQuietly(it);
        }
        return shouldWePrintFile;
    }

    private void assertTableRowCount(XWPFDocument document, TableMarkerResult tableMarkerResult) {
        if (tableMarkerResult != null && tableMarkerResult.getTableRowCount() != null) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setBold(true);
            run.setText("Records count  ");
            run = paragraph.createRun();
            run.setColor(GREY);
            run.setText("ER: ");
            run = paragraph.createRun();
            run.setBold(true);
            run.setText(tableMarkerResult.getTableRowCount().getExpectedResult());
            run = paragraph.createRun();
            run.setColor(GREY);
            run.setText("  AR: ");
            run = paragraph.createRun();
            run.setBold(true);
            if (Statuses.SUCCESS.equals(tableMarkerResult.getTableRowCount().getStatus())) {
                run.setColor(GREEN);
            } else {
                run.setColor(RED);
            }
            run.setText(tableMarkerResult.getTableRowCount().getActualResult());
        }
    }

    private XWPFParagraph createBookmarkedParagraph(XWPFDocument document, String anchor, int bookmarkId) {
        XWPFParagraph paragraph = document.createParagraph();
        CTBookmark bookmark = paragraph.getCTP().addNewBookmarkStart();
        bookmark.setName(anchor);
        bookmark.setId(BigInteger.valueOf(bookmarkId));
        paragraph.getCTP().addNewBookmarkEnd().setId(BigInteger.valueOf(bookmarkId));
        return paragraph;
    }

    private XWPFHyperlinkRun createHyperlinkRunToAnchor(XWPFParagraph paragraph, String anchor) {
        CTHyperlink cthyperLink = paragraph.getCTP().addNewHyperlink();
        cthyperLink.setAnchor(anchor);
        cthyperLink.addNewR();
        return new XWPFHyperlinkRun(
                cthyperLink,
                cthyperLink.getRArray(0),
                paragraph
        );
    }

    private XWPFRun createRunForEmptyHeaderCell(XWPFTableCell cell) {
        cell.setColor("ffffff");
        cell.getCTTc().getTcPr().addNewTcW().setW(BigInteger.valueOf(5000));
        return getXwpfRunFromParagraph(cell.getParagraphs().get(0), 11);
    }

    private LineIterator getLineIterator(File outputFile) {
        try {
            return FileUtils.lineIterator(outputFile, "UTF-8");
        } catch (IOException e) {
            throw new PotIoException(outputFile.toString());
        }
    }

    private int getMinimumAmountOfLinesInFileToPrint() {
        int amountLinesToPrint = maxPrintedLines - printedLines;
        if (amountLinesToPrint < 1) {
            amountLinesToPrint = minimumAmountOfLinesInFileToPrint;
        }
        return amountLinesToPrint;
    }

    private XWPFRun getRunForCell(XWPFTableCell cell) {
        return getXwpfRunFromParagraph(cell.getParagraphs().get(0), 6);
    }

    private XWPFRun getRunForColumnNameCell(XWPFTableCell cell) {
        cell.setColor("E3FCDF");
        cell.getCTTc().getTcPr().addNewTcW().setW(BigInteger.valueOf(1500));
        return getXwpfRunFromParagraph(cell.getParagraphs().get(0), 6);
    }

    private XWPFRun getRunForFirstHeaderCell(XWPFTableCell cell) {
        cell.setColor("E3FCDF");
        cell.getCTTc().getTcPr().addNewTcW().setW(BigInteger.valueOf(2000));
        final XWPFRun run = getXwpfRunFromParagraph(cell.getParagraphs().get(0), 11);
        run.setBold(true);
        return run;
    }

    /**
     * Get XWPFRun from paragraph.
     *
     * @param paragraph paragraph
     * @param fontSize  font size
     * @return XWPFRun
     */
    private XWPFRun getXwpfRunFromParagraph(XWPFParagraph paragraph, int fontSize) {
        paragraph.setIndentFromLeft(50);
        paragraph.setIndentFromRight(30);
        paragraph.setSpacingAfter(30);
        paragraph.setSpacingBefore(30);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(fontSize);
        return run;
    }

    private void printError(XWPFRun run, PotSessionException error) {
        run.setColor(RED);
        run.setFontSize(8);
        run.setText(error.getMessage());
        run.addBreak();
    }

    private String printExecutionStep(XWPFDocument document, PotExecutionStep step,
                                      List<String> filePaths, int bookmarkId) {
        log.info("Start saving '{}' step in POT", step.getStepName());
        XWPFParagraph titleParagraph = createBookmarkedParagraph(document,
                step.getStepName() + "_" + bookmarkId, bookmarkId);
        titleParagraph.setPageBreak(true);
        titleParagraph.setStyle("Heading1");
        XWPFRun run = titleParagraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(step.getStepName());
        run = titleParagraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        if (step.getProcessStatus() != null && step.getProcessStatus().getStatus().equals(Statuses.SUCCESS)) {
            run.setColor(GREEN);
            run.setText(" : " + step.getProcessStatus().getStatus().toString());
        } else if (step.getProcessStatus() != null && step.getProcessStatus().getStatus().equals(Statuses.WARNING)) {
            run.setColor(YELLOW_STATUS);
            run.setText(" : " + Statuses.WARNING.toString());
        } else {
            run.setColor(RED);
            run.setText(" : " + Statuses.FAIL.toString());
        }
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle("Heading2");
        run = paragraph.createRun();
        run.setText("Execution");
        run = document.createParagraph().createRun();
        if (step.getExecutedCommand() != null) {
            for (String str : step.getExecutedCommand().split("\r\n")) {
                run.setText(str);
                run.addBreak();
            }
        }
        for (Link link : step.getLinks()) {
            if (step.getProcessStatus() == null) {
                throw new PotProcessStatusMissedException(step.getStepName());
            }
            printOutputFile(document, link, step.getProcessStatus().getMarker(), null, filePaths);
        }
        if (step.getValidations().size() > 0) {
            paragraph = document.createParagraph();
            paragraph.setStyle("Heading2");
            run = paragraph.createRun();
            run.setFontSize(12);
            run.setBold(true);
            run.setText("Validations");
        }
        for (SqlResponse sqlResponse : step.getValidations()) {
            printValidation(document, sqlResponse, filePaths);
        }
        if (step.getErrors().size() > 0) {
            paragraph = document.createParagraph();
            paragraph.setStyle("Heading2");
            run = paragraph.createRun();
            run.setFontSize(12);
            run.setBold(true);
            run.setText("Errors");
        }
        for (PotSessionException error : step.getErrors()) {
            printError(document.createParagraph().createRun(), error);
        }
        log.info("'{}' step was saved", step.getStepName());
        return titleParagraph.getText();
    }

    /**
     * Print file (csv, log, txt) link and adds it to filePath (final archive).
     *
     * @param document  document
     * @param link      link to file
     * @param filePaths files of zip file
     */
    private void printFileLink(XWPFDocument document, Link link, List<String> filePaths) {
        if (link != null) {
            XWPFHyperlinkRun hyperlinkRun = document.createParagraph()
                    .createHyperlinkRun(fileDownloadPrefix + miaContext.getLogLinkOnUi(link.getPath()).getPath());
            hyperlinkRun.setText(link.getName());
            hyperlinkRun.setFontSize(10);
            hyperlinkRun.setColor(BLUE);
            hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
            if (filePaths != null) {
                filePaths.add(link.getPath());
            }
        } else {
            XWPFRun errorTextRun = document.createParagraph().createRun();
            errorTextRun.setText("Output file not found, link is empty.");
            errorTextRun.setFontSize(10);
            errorTextRun.setColor(RED);
        }
    }

    private void printFileName(XWPFDocument document, Link link) {
        printFileLink(document, link, null);
    }

    private void printHeader(XWPFTable table, PotHeader header) {
        XWPFTableRow row = table.createRow();
        XWPFRun run = getRunForFirstHeaderCell(row.getCell(0));
        run.setText(header.getName());
        run = createRunForEmptyHeaderCell(row.getCell(1));
        String type = header.getType();
        PotHeaderType headerType = PotHeaderType.valueOf(type);
        //save system and their connections
        Holder<System> system = new Holder<>(null);
        try {
            system.value = miaContext.getFlowData().getSystem(header.getSystem());
        } catch (Exception e) { /*skip*/ }
        if (system.value != null) {
            Arrays.stream(Server.ConnectionType.values()).forEach(t -> {
                try {
                    miaContext.getFlowData().addParameters(system.value.getServer(t).getProperties());
                } catch (IllegalArgumentException | NullPointerException e) { /*skip*/ }
            });
        }
        String value;
        switch (headerType) {
            case TEXT:
                value = header.getValue();
                break;
            case INPUT:
                value = miaContext.evaluate(header.getValue());
                break;
            case SSH:
                if (system.value != null) {
                    try {
                        CommandResponse commandResponse = shellRepository.executeAndGetLog(
                                new Command("POT_header", "SSH", header.getSystem(),
                                        listToSet(header.getValue())));
                        if (commandResponse.getCommandOutputs().size() > 0) {
                            value = String.join("\n", commandResponse.getCommandOutputs().get(0).contentFromFile());
                        } else {
                            value = "No output file for SSH command execution '" + header.getValue() + "'";
                        }
                    } catch (IllegalArgumentException e) {
                        value = "SSH connection for system '" + header.getSystem() + "' not found";
                    } catch (RuntimeException e) {
                        value = "SSH command execution '" + header.getValue() + "'failed for system '"
                                + header.getSystem() + "'";
                    }
                } else {
                    value = "System '" + header.getSystem() + "' not found";
                }
                break;
            case SQL:
                if (system.value != null) {
                    try {
                        Server server = system.value.getServer(Server.ConnectionType.DB);
                        value = queryDriverFactory.getDriver(server).executeQueryAndGetFirstValue(
                                server, miaContext.evaluate(header.getValue()));
                    } catch (IllegalArgumentException e) {
                        value = "Database connection for system '" + header.getSystem() + "' not found";
                    }
                } else {
                    value = "System '" + header.getSystem() + "' not found";
                }
                break;
            default:
                throw new PotHeaderTypeUnsupportedException(type);
        }
        run.setText(value);
    }

    private void printHeaders(XWPFDocument document, PotSession session) {
        List<PotHeader> headers = new ArrayList<>(miaContext.getPotHeaderConfiguration().getHeaders());
        if (headers.isEmpty()
                || headers.stream().noneMatch(header -> header.getName().equalsIgnoreCase("Environment"))) {
            headers.add(0, new PotHeader(
                    "Environment", PotHeaderType.INPUT.toString(), null,
                    session.getPotExecutionSteps().stream()
                            .map(PotExecutionStep::getEnvironmentName)
                            .collect(Collectors.toList()).toString()));
        }
        XWPFTable table = document.createTable();
        XWPFTableRow row = setTableStyleAndCreateRow(table);
        XWPFRun run = getRunForFirstHeaderCell(row.createCell());
        run.setText("Test line");
        createRunForEmptyHeaderCell(row.createCell());
        for (PotHeader header : headers) {
            printHeader(table, header);
        }
        table.removeRow(0);
        document.createParagraph().setPageBreak(true);
    }

    /**
     * Print file (csv, log, txt) content, add to zip file otherwise.
     *
     * @param document          document
     * @param link              link to file
     * @param logMarkerEntity   markers for log file
     * @param tableMarkerResult markers for table
     */
    private void printOutputFile(XWPFDocument document, Link link, Marker logMarkerEntity,
                                 TableMarkerResult tableMarkerResult, List<String> filePaths) {
        File outputFile = new File(link.getPath());
        try {
            miaFileService.getFile(outputFile);
            if (outputFile.isDirectory()) {
                throw new IOException("File not found, it's directory path:" + outputFile);
            }
        } catch (IOException | MiaException e) {
            log.error(ErrorCodes.MIA_2158_POT_PRINT_FILE_NOT_FOUND.getMessage(e.getMessage()), e);
            return;
        }
        printFileName(document, link);
        boolean shouldWePrintFile = false;
        if (link.getName().endsWith(".csv")) {
            try {
                String content = new String(Files.readAllBytes(outputFile.toPath()));
                if (tableMarkerResult != null) {
                    shouldWePrintFile = analyseAndPrintTable(document, content, tableMarkerResult);
                } else {
                    shouldWePrintFile = printTable(document, content);
                }
            } catch (IOException e) {
                log.error(ErrorCodes.MIA_2158_POT_PRINT_FILE_NOT_FOUND.getMessage(e.getMessage()), e);
            }
        } else if (link.getName().endsWith(".log") || link.getName().endsWith(".txt")) {
            LineIterator it = getLineIterator(outputFile);
            if (logMarkerEntity != null) {
                shouldWePrintFile = analyseAndPrintTextFile(document.createParagraph(), it, logMarkerEntity);
            } else {
                shouldWePrintFile = printTextFile(document.createParagraph().createRun(), it);
            }
        } else if (link.getName().endsWith(".json")) {
            shouldWePrintFile = true;
        }
        if (shouldWePrintFile) {
            filePaths.add(link.getPath());
        }
    }

    private boolean printTable(XWPFDocument document, String content) {
        XWPFTable table;
        XWPFTableRow row;
        XWPFTableCell cell;
        XWPFRun run;
        final TableContentInfo tci = new TableContentInfo(content);
        printTableDescription(tci, document);
        for (int i = 0; i < (int) Math.ceil((double) tci.columnNames.length / tci.columnsInString); i++) {
            table = document.createTable();
            row = setTableStyleAndCreateRow(table);
            //add columnName
            for (int j = 0; j < tci.columnsInString && i * tci.columnsInString + j < tci.countOfColumns; j++) {
                cell = row.createCell();
                run = getRunForColumnNameCell(cell);
                run.setText(tci.columnNames[i * tci.columnsInString + j]);
            }
            //add Data
            for (int d = 1; d < tci.tableRows.length && d <= maxPrintedLinesSqlTable; d++) {
                printTableRow(table, Arrays.asList(tci.tableRows[d].split(",", -1)), tci.columnsInString,
                        tci.countOfColumns, i);
            }
            XWPFParagraph paragraph = document.createParagraph();
            if (tci.tableRows.length > maxPrintedLinesSqlTable) {
                run = paragraph.createRun();
                run.setFontSize(8);
                run.setText(shrinkSymbol);
                run.addBreak();
            }
        }
        document.createParagraph();
        return tci.tableRows.length > maxPrintedLinesSqlTable;
    }

    /**
     * Prind description of table.
     *
     * @param tci      TableContentInfo instance
     * @param document document to write
     */
    private void printTableDescription(TableContentInfo tci, XWPFDocument document) {
        if (tci.description.length > 0) {
            final XWPFRun run = document.createParagraph().createRun();
            if (Arrays.toString(tci.description).matches(".*Message.*Cause.*StackTrace.*")) {
                run.setColor(RED);
                run.setFontSize(12);
            } else {
                run.setFontSize(8);
            }
            Arrays.stream(tci.description).forEach(d -> {
                run.setText(d);
                run.addBreak();
            });
        }
    }

    private void printTableRow(XWPFTable table, List<String> data, int columnsInString,
                               int countOfColumns, int partOfTable) {
        XWPFTableRow row;
        XWPFTableCell cell;
        row = table.createRow();
        for (int j = 0; j < columnsInString && partOfTable * columnsInString + j < countOfColumns; j++) {
            try {
                cell = row.getCell(j);
                XWPFRun run = getRunForCell(cell);
                run.setText(data.get(partOfTable * columnsInString + j));
            } catch (Exception e) {
                log.error(ErrorCodes.MIA_2157_POT_PRINT_ROW_ERROR.getMessage(),
                        countOfColumns, partOfTable * columnsInString + j, data.size());
            }
        }
    }

    private boolean printTextFile(XWPFRun run, LineIterator it) {
        boolean shouldWePrintFile = false;
        int amountLinesToPrint = getMinimumAmountOfLinesInFileToPrint();
        int i = 0;
        try {
            run.setFontSize(8);
            while (it.hasNext()) {
                if (i++ == amountLinesToPrint) {
                    run.setText(shrinkSymbol);
                    run.addBreak();
                    shouldWePrintFile = true;
                    break;
                }
                run.setText(it.next());
                run.addBreak();
                printedLines++;
            }
        } finally {
            LineIterator.closeQuietly(it);
        }
        return shouldWePrintFile;
    }

    private void printValidation(XWPFDocument document, SqlResponse sqlResponse,
                                 List<String> filePaths) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontSize(8);
        if (sqlResponse.getQuery() != null) {
            for (String str : sqlResponse.getQuery().split("\r\n")) {
                run.setText(str);
                run.addBreak();
            }
        }
        if (sqlResponse.getLink() != null) {
            if (sqlResponse.isSaveToWordFile()) {
                printOutputFile(document, sqlResponse.getLink(), null, sqlResponse.getTableMarkerResult(), filePaths);
            }
            if (sqlResponse.isSaveToZipFile()) {
                printFileLink(document, sqlResponse.getLink(), filePaths);
            }
        }
    }

    private XWPFTableRow setTableStyleAndCreateRow(XWPFTable table) {
        table.setWidth(7000);
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTJc jc = (tblPr.isSetJc() ? tblPr.getJc() : tblPr.addNewJc());
        STJc.Enum en = STJc.Enum.forInt(ParagraphAlignment.LEFT.getValue());
        jc.setVal(en);
        table.removeRow(0);
        return table.createRow();
    }

    private class TableContentInfo {

        public final String[] tableRows;
        public final String[] columnNames;
        public final int countOfColumns;
        public final int columnsInString = 8;
        public String table = "";
        public String query = "";
        public String[] description = new String[0];

        public TableContentInfo(String content) {
            final String[] blocks = content.split("\n,\n");
            if (blocks.length > 0) {
                this.table = blocks[0];
                if (blocks.length > 1) {
                    this.query = blocks[1];
                    if (blocks.length > 2) {
                        this.description = blocks[2].split("\n");
                    }
                }
            }
            this.tableRows = this.table.split("\n");
            this.columnNames = tableRows[0].split(",");
            this.countOfColumns = columnNames.length;
        }
    }
}
