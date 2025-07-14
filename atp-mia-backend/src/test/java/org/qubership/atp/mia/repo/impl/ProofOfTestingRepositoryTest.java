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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.utils.FileUtils.newFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.ws.Holder;

import org.apache.commons.io.FileUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;
import org.qubership.atp.mia.model.pot.entity.PotExecutionStep;
import org.qubership.atp.mia.model.pot.entity.PotSession;
import org.qubership.atp.mia.repo.db.RecordingSessionRepository;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.execution.RecordingSessionsService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.shaded.org.apache.commons.io.FilenameUtils;

@ExtendWith(SkipTestInJenkins.class)
public class ProofOfTestingRepositoryTest extends ConfigTestBean {

    private final String defaultProcessName = "sql-run";
    private final String defaultOutputName = "output_file";
    private final String potFilename = "testPOTfile.docx";

    private static final String RED = "fd483d";
    private static final String GREEN = "008000";
    private static final String defaultTableName = "output_table";
    private final ThreadLocal<Path> TEST_DIRECTORY = new ThreadLocal<>();
    private final ThreadLocal<PotSession> session = new ThreadLocal<>();
    private final ThreadLocal<File> fileToSave = new ThreadLocal<>();
    private final ThreadLocal<RecordingSessionRepository> recordingSessionRepository = new ThreadLocal<>();
    private final ThreadLocal<RecordingSessionsService> recordingSessionsServiceTest = new ThreadLocal<>();
    private final ThreadLocal<ProofOfTestingRepository> proofOfTestingRepository = new ThreadLocal<>();

    @MockBean
    private ShellRepository shellRepositoryMock;

    @AfterEach
    public void cleanFile() {
        try {
            FileUtils.forceDelete(TEST_DIRECTORY.get().toFile());
        } catch (IOException | IllegalArgumentException e) {
            // ignore
        }
    }

    @BeforeEach
    public void beforeProofOfTestingRepositoryTest() {
        TEST_DIRECTORY.set(Paths.get("src").resolve("test").resolve("resources").resolve("testData").resolve("pot")
                .resolve("" + System.currentTimeMillis()).resolve("" + new Random().nextInt(10000)));
        TEST_DIRECTORY.get().toFile().mkdirs();
        fileToSave.set(TEST_DIRECTORY.get().resolve(potFilename).toFile());
        AtpUserService atpUserService = mock(AtpUserService.class);
        recordingSessionRepository.set(mock(RecordingSessionRepository.class));
        recordingSessionsServiceTest.set(new RecordingSessionsService(recordingSessionRepository.get(),
                miaContext.get(), atpUserService));
        UUID sessionId = UUID.randomUUID();
        session.set(new PotSession(sessionId, miaContext.get().getConfig(), "user"));
        doReturn(null).when(miaFileService.get()).getFile(anyString());
        recordingSessionsServiceTest.set(
                new RecordingSessionsService(recordingSessionRepository.get(), miaContext.get(), atpUserService));
        proofOfTestingRepository.set(new ProofOfTestingRepository(shellRepositoryMock, miaFileService.get(),
                queryDriverFactory.get(), miaContext.get(), recordingSessionsServiceTest.get(), ""));
        when(recordingSessionRepository.get().findById(sessionId)).thenReturn(Optional.of(session.get()));
        when(recordingSessionRepository.get().save(any(PotSession.class))).thenAnswer(i -> i.getArguments()[0]);
        miaContext.get().getFlowData().setSessionId(sessionId);
    }

    public void correctFilePath(List<Link> links) {
        if (links == null || links.size() == 0) {
            Assert.fail("Links is empty");
        }
        links.forEach(f -> f.setPath(TEST_DIRECTORY.get().resolve(f.getName()).toString()));
    }

    public Map<String, CommandResponse> fillCommandResponse(Map<String, List<Link>> commandOutputs,
                                                            Link sqlOutput, LinkedList<Exception> errors) {
        return commandOutputs.entrySet().stream().sequential()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> fillCommandResponse(e.getValue(), sqlOutput, errors),
                        (u, v) -> u,
                        LinkedHashMap::new));
    }

    public CommandResponse fillCommandResponse(List<Link> commandOutputs, Link sqlOutput,
                                               LinkedList<Exception> errors) {
        CommandResponse response = new CommandResponse();
        if (commandOutputs != null) {
            LinkedList<CommandOutput> commandOutputList = new LinkedList<>();
            commandOutputs.forEach(link -> commandOutputList.add(new CommandOutput(link.getPath(), null, true, miaContext.get())));
            response.setCommandOutputs(commandOutputList);
        }
        if (sqlOutput != null) {
            SqlResponse sqlResponse = new SqlResponse();
            sqlResponse.setQuery(sqlOutput.getName());
            sqlResponse.setInternalPathToFile(sqlOutput.getPath(), miaContext.get());
            sqlResponse.setLink(sqlOutput);
            response.setSqlResponse(sqlResponse);
        }
        if (errors != null) {
            response.setErrors(errors);
        }
        return response;
    }

    public List<ExecutionResponse> fillExecutionResponse(Map<String, CommandResponse> namesToResponses,
                                                         List<Statuses> statuses,
                                                         List<List<SqlResponse>> validations) {
        return fillExecutionResponse(namesToResponses, statuses, validations, null);
    }

    public List<ExecutionResponse> fillExecutionResponse(Map<String, CommandResponse> namesToResponses,
                                                         List<Statuses> statuses,
                                                         List<List<SqlResponse>> validations,
                                                         List<MiaException> errors) {
        if (namesToResponses.size() == 0) {
            Assert.fail("you forgot to add any items to map names to response");
        }
        if (statuses.size() == 0) {
            Assert.fail("you forgot to add any items to statuses");
        }
        if (namesToResponses.size() != statuses.size()) {
            Assert.fail("Size of statuses should be equal to namesToResponses! "
                    + "Remember every process name will have related status.");
        } else if (validations != null && validations.size() != namesToResponses.size()) {
            Assert.fail("Size of validations (if not null) should be equal to namesToResponses! "
                    + "Remember every process name will have related validation.");
        } else if (errors != null && errors.size() != namesToResponses.size()) {
            Assert.fail("Size of errors (if not null) should be equal to namesToResponses! "
                    + "Remember every process name will have related validation.");
        }
        int i = 0;
        List<ExecutionResponse> responses = new ArrayList<>();
        for (Map.Entry<String, CommandResponse> entry : namesToResponses.entrySet()) {
            List<SqlResponse> validation = validations == null ? null : validations.get(i);
            MiaException err = errors == null ? null : errors.get(i);
            responses.add(fillExecutionResponse(entry.getKey(), statuses.get(i++), entry.getValue(), validation, err));
        }
        return responses;
    }

    public PotSession fillRecordingSession(UUID oldSessionId, UUID projectId, String environmentName,
                                           List<PotExecutionStep> steps) {
        PotSession recordingSession = new PotSession(oldSessionId, miaContext.get().getConfig(), "user");
        steps.forEach(recordingSession::addExecutionStep);
        return recordingSession;
    }

    public PotExecutionStep fillStep(String name) {
        PotExecutionStep step = new PotExecutionStep();
        step.setStepName(name);
        step.setProcessStatus(new ProcessStatus());
        step.setExecutedCommand(name + "command");
        step.setLinks(Collections.singletonList(generateOutputFile(name)));
        step.setErrors(Collections.singletonList(new Exception(name)));
        step.setValidations(Collections.singletonList(
                fillValidation(name + "validation", name + "query", true, true)));
        return step;
    }

    public List<PotExecutionStep> fillSteps(int amount, String name) {
        return IntStream.range(0, amount).sequential()
                .mapToObj(i -> fillStep(name + i))
                .collect(Collectors.toList());
    }

    public SqlResponse fillValidation(Link link, String query, TableMarkerResult tmr) {
        SqlResponse validation = new SqlResponse();
        validation.setConnectionInfo(null);
        validation.setQuery(query);
        validation.setTableMarkerResult(tmr);
        validation.setInternalPathToFile(link.getPath(), miaContext.get());
        validation.setLink(link);
        validation.setSaveToWordFile(true);
        validation.setSaveToZipFile(false);
        return validation;
    }

    public List<Exception> generateExceptions(int amount) {
        return IntStream.range(0, amount).sequential()
                .mapToObj(i -> new Exception("error" + i))
                .collect(Collectors.toList());
    }

    public Link generateOutputFile(String filename) {
        return generateOutputFile(filename, 1);
    }

    public Link generateOutputFile(String filename, int contentLines) {
        File file = TEST_DIRECTORY.get().resolve(filename).toFile();
        try {
            BufferedWriter wr = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < contentLines; i++) {
                wr.write(filename + "\n");
            }
            wr.close();
        } catch (IOException e) {
            Assert.fail("can't generate output file: " + e.getMessage());
        }
        return new Link(file.getAbsolutePath(), filename);
    }

    public Map<String, List<Link>> generateOutputFileLinks(int size, String processNamePrefix,
                                                           String fileNamePrefix,
                                                           int contentLines) {
        Map<String, List<Link>> result = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            result.put(processNamePrefix + i,
                    Collections.singletonList(generateOutputFile(fileNamePrefix + i + ".txt", contentLines)));
        }
        return result;
    }

    public Map<String, List<Link>> generateOutputFileLinks(int size, String processNamePrefix, String
            fileNamePrefix) {
        Map<String, List<Link>> result = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            result.put(processNamePrefix + i,
                    Collections.singletonList(generateOutputFile(fileNamePrefix + i + ".txt")));
        }
        return result;
    }

    public List<Statuses> generateStatusesList(int amount) {
        return IntStream.range(0, amount).sequential().mapToObj(i -> Statuses.SUCCESS).collect(Collectors.toList());
    }

    public List<List<SqlResponse>> generateValidation(int amountProcesses, int amountValidationInProcess,
                                                      String query, boolean isRowSuccess, boolean isColumnSuccess) {
        List<String> names = IntStream.range(0, amountProcesses).sequential()
                .mapToObj(i -> defaultTableName + i + ".csv").collect(Collectors.toList());
        List<List<SqlResponse>> result = new LinkedList<>();
        for (String name : names) {
            List<SqlResponse> validationPerProcess = new LinkedList<>();
            for (int i = 0; i < amountValidationInProcess; i++) {
                validationPerProcess.add(fillValidation(name, query, isRowSuccess, isColumnSuccess));
            }
            result.add(validationPerProcess);
        }
        return result;
    }

    public List<List<SqlResponse>> generateValidation(int amountProcesses, String query,
                                                      boolean isRowSuccess, boolean isColumnSuccess) {
        List<String> names = IntStream.range(0, amountProcesses).sequential()
                .mapToObj(i -> defaultTableName + i + ".csv").collect(Collectors.toList());
        return generateValidation(names, query, isRowSuccess, isColumnSuccess);
    }

    public List<List<SqlResponse>> generateValidation(List<String> names, String query,
                                                      boolean isRowSuccess, boolean isColumnSuccess) {
        List<List<SqlResponse>> result = new LinkedList<>();
        for (String name : names) {
            result.add(Collections.singletonList(fillValidation(name, query, isRowSuccess, isColumnSuccess)));
        }
        return result;
    }

    @Test
    public void saveProofOfTesting_containsLinksToOutputFiles() {
        // prepare data
        int amount = 5;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName);
        List<Statuses> statuses = generateStatusesList(amount);
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, null, null);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, null);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        Map<File, List<File>> unzippedFiles = unzipFiles(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
        validatePotLinks(unzippedFiles);
    }

    @Test
    public void saveProofOfTesting_errorInCommandResponseShouldBe() {
        // prepare data
        int amount = 5;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName, 15);
        List<Statuses> statuses = generateStatusesList(amount);
        LinkedList<Exception> errors = new LinkedList<>(generateExceptions(amount));
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, null, errors);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, null);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
    }

    @Test
    public void saveProofOfTesting_errorInExecutionResponseShouldBe() {
        // prepare data
        int amount = 5;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName, 15);
        List<Statuses> statuses = generateStatusesList(amount);
        LinkedList<Exception> errors = new LinkedList<>(generateExceptions(amount));
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, null, null);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, null);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
    }

    @Test
    public void saveProofOfTesting_multiValidationForOneProcessShouldWork() {
        // prepare data
        int amount = 5;
        int amountValidation = 15;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName, 15);
        List<Statuses> statuses = generateStatusesList(amount);
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, null, null);
        List<List<SqlResponse>> validations =
                generateValidation(amount, amountValidation, "SELECT1", true, true);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, validations);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        Map<File, List<File>> potAndOutputs = unzipFiles(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
        validatePotValidation(potAndOutputs, validations);
        // TODO check validation printed successfully
    }

    @Test
    public void saveProofOfTesting_sqlValidationShouldTransformWithoutProblem() {
        // prepare data
        int amount = 5;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName, 15);
        List<Statuses> statuses = generateStatusesList(amount);
        List<List<SqlResponse>> validations = generateValidation(amount, 15, "SELECT1", true, true);
        Link sqlLink = validations.get(0).get(0).getLink();
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, sqlLink, null);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, null);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
    }

    @Test
    public void saveProofOfTesting_validationsCorrectColorFail() {
        // prepare data
        int amount = 5;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName, 15);
        List<Statuses> statuses = generateStatusesList(amount);
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, null, null);
        List<List<SqlResponse>> validations = generateValidation(amount, "SELECT1", false, false);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, validations);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        Map<File, List<File>> unzippedFiles = unzipFiles(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
        validatePotValidation(unzippedFiles, validations);
    }

    @Test
    public void saveProofOfTesting_validationsCorrectColorSuccess() {
        // prepare data
        int amount = 5;
        Map<String, List<Link>> procNamesToLinks =
                generateOutputFileLinks(amount, defaultProcessName, defaultOutputName, 15);
        List<Statuses> statuses = generateStatusesList(amount);
        Map<String, CommandResponse> command = fillCommandResponse(procNamesToLinks, null, null);
        List<List<SqlResponse>> validations = generateValidation(amount, "SELECT1", true, true);
        List<ExecutionResponse> responses = fillExecutionResponse(command, statuses, validations);
        addExecutionSteps(responses);
        // save POT
        List<Link> resultFiles = proofOfTestingRepository.get().saveProofOfTesting(session.get(), fileToSave.get());
        correctFilePath(resultFiles);
        Map<File, List<File>> unzippedFiles = unzipFiles(resultFiles);
        // assert
        Assert.assertEquals(1, resultFiles.size());
        validatePotValidation(unzippedFiles, validations);
    }

    public Map<File, List<File>> unzipFiles(List<Link> files) {
        if (files.size() == 0) {
            Assert.fail("POT output Files is empty");
        }
        if (files.size() > 1) {
            Assert.fail("POT output files size is more than 1. If implementation changed, apply it here.");
        }
        Map<File, List<File>> result = new HashMap<>();
        for (Link link : files) {
            try {
                result.putAll(unzipFiles(link));
            } catch (IOException e) {
                Assert.fail("IOException during unzip file: " + link.getPath() + " error: " + e.getMessage());
            }
        }
        return result;
    }

    public Map<File, List<File>> unzipFiles(Link file) throws IOException {
        Map<File, List<File>> unzippedFiles = new HashMap<>();
        List<File> outputFiles = new ArrayList<>();
        String fileZip = file.getPath();
        File destDir = Paths.get(file.getPath()).getParent().resolve("test_zip").toFile();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory inside zip " + newFile);
                }
            } else {
                if (FilenameUtils.getExtension(newFile.getName()).equals("docx")) {
                    unzippedFiles.put(newFile, outputFiles);
                } else {
                    outputFiles.add(newFile);
                }
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return unzippedFiles;
    }

    public void validatePotColoredTableValidationCorrectly(File potFile, List<List<SqlResponse>> validationInfo) {
        int tableI = 1;
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(potFile))) {
            List<XWPFTable> tables = document.getTables();
            if (tables.size() - 1 != validationInfo.stream().mapToInt(i -> i.size()).sum()) {
                Assert.fail("Validation tables is not equal than tables in POT file (1 is for environment)."
                        + "pot: " + (tables.size() - 1)
                        + " validation: " + validationInfo.stream().mapToInt(i -> i.size()).sum());
            }
            for (List<SqlResponse> sqlResponses : validationInfo) {
                XWPFTable table = tables.get(tableI++);
                for (SqlResponse sqlResponse : sqlResponses) {
                    if (sqlResponse.getTableMarkerResult() == null) {
                        Assert.fail("TableMarkerResult is null for table from file:"
                                + sqlResponse.getInternalPathToFile());
                    }
                    List<TableMarkerResult.TableMarkerColumnStatus> colStatuses =
                            sqlResponse.getTableMarkerResult().getColumnStatuses();
                    List<XWPFTableCell> firstRowCells = table.getRows().get(0).getTableCells();
                    List<XWPFTableCell> lastRowCells = table.getRows().get(table.getRows().size() - 1).getTableCells();
                    for (int i = 0; i < lastRowCells.size(); i++) {
                        String colName = firstRowCells.get(i).getText().trim();
                        Optional<TableMarkerResult.TableMarkerColumnStatus> validationColName = colStatuses.stream()
                                .filter(s -> s.getColumnName().trim().equalsIgnoreCase(colName)).findAny();
                        if (validationColName.isPresent()) {
                            if (validationColName.get().getStatus().equals(Statuses.SUCCESS)) {
                                String errMsg = "Cell is not green when status success! tableIndex: " + tableI
                                        + " colIndex" + i + " colHeader: " + firstRowCells.get(i).getText();
                                Assert.assertTrue(errMsg, lastRowCells.get(i).getColor().equalsIgnoreCase(GREEN));
                            } else if (validationColName.get().getStatus().equals(Statuses.FAIL)) {
                                String errMsg = "Cell is not red when status fail! tableIndex: " + tableI
                                        + " colIndex" + i + " colHeader: " + firstRowCells.get(i).getText();
                                Assert.assertTrue(errMsg, lastRowCells.get(i).getColor().equalsIgnoreCase(RED));
                            }
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Assert.fail("POT file not found: " + potFile.getAbsolutePath());
        } catch (IOException e) {
            Assert.fail("POT file IOError: " + potFile.getAbsolutePath() + ", error:" + e.getMessage());
        }
    }

    public void validatePotContainsLinksEqualOutputFileName(File potFile, List<File> outputFiles) {
        List<String> outputFilesCopy = outputFiles.stream().map(File::getName).collect(Collectors.toList());
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(potFile))) {
            for (XWPFHyperlink hyperlink : document.getHyperlinks()) {
                Optional r = outputFilesCopy.stream().filter(fn -> hyperlink.getURL().contains(fn)).findAny();
                if (!r.isPresent()) {
                    Assert.fail("Hyperlink doesn't contain any outputfile name: " + hyperlink.getURL());
                }
            }
        } catch (FileNotFoundException e) {
            Assert.fail("POT file not found: " + potFile.getAbsolutePath());
        } catch (IOException e) {
            Assert.fail("POT file IOError: " + potFile.getAbsolutePath() + ", error:" + e.getMessage());
        }
    }

    public void validatePotContainsOutputFileName(File potFile, List<File> outputFiles) {
        List<String> outputFilesCopy = outputFiles.stream().map(File::getName).collect(Collectors.toList());
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(potFile))) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                outputFilesCopy.remove(text);
            }
            if (outputFilesCopy.size() > 0) {
                Assert.fail("This filenames is not present in POT document: " + String.join(", ", outputFilesCopy));
            }
        } catch (FileNotFoundException e) {
            Assert.fail("POT file not found: " + potFile.getAbsolutePath());
        } catch (IOException e) {
            Assert.fail("POT file IOError: " + potFile.getAbsolutePath() + ", error:" + e.getMessage());
        }
    }

    public void validatePotLinks(Map<File, List<File>> unzippedFiles) {
        for (Map.Entry<File, List<File>> entry : unzippedFiles.entrySet()) {
            validatePotContainsOutputFileName(entry.getKey(), entry.getValue());
            validatePotContainsLinksEqualOutputFileName(entry.getKey(), entry.getValue());
        }
    }

    public void validatePotValidation(Map<File, List<File>> unzippedFiles, List<List<SqlResponse>> validationInfo) {
        for (Map.Entry<File, List<File>> entry : unzippedFiles.entrySet()) {
            validatePotColoredTableValidationCorrectly(entry.getKey(), validationInfo);
        }
    }

    private PotSession addExecutionStep(ExecutionResponse executionResponse) {
        return recordingSessionsServiceTest.get().addExecutionStep(executionResponse);
    }

    private void addExecutionSteps(List<ExecutionResponse> responses) {
        Holder<PotSession> recordingSession = new Holder<>();
        responses.forEach(r -> recordingSession.value = addExecutionStep(r));
        session.set(recordingSession.value);
    }

    private ExecutionResponse fillExecutionResponse(String processName, Statuses status,
                                                    CommandResponse commandResponse,
                                                    List<SqlResponse> validations,
                                                    MiaException error) {
        ProcessStatus processStatus = new ProcessStatus();
        processStatus.setStatus(status);
        return fillExecutionResponse(processName, processStatus, commandResponse, validations, error);
    }

    private ExecutionResponse fillExecutionResponse(String processName, ProcessStatus status,
                                                    CommandResponse commandResponse,
                                                    List<SqlResponse> validations,
                                                    MiaException error) {
        ExecutionResponse response = new ExecutionResponse();
        response.setProcessName(processName);
        response.setExecutedCommand(processName);
        response.setCommandResponse(commandResponse);
        response.setValidations(validations);
        response.setError(error);
        response.setProcessStatus(status);
        return response;
    }

    private SqlResponse fillValidation(String name, String query, boolean isRowSuccess,
                                       boolean isColumnSuccess) {
        TableMarkerResult tmr = new TableMarkerResult();
        if (isRowSuccess) {
            tmr.setTableRowCount("1", "1", Statuses.SUCCESS);
        } else {
            tmr.setTableRowCount("5", "1", Statuses.FAIL);
        }
        List<String> cols = Arrays.asList("NAME", "VAL1", "DATA1", "DATA2", "DATA3");
        List<String> expCols = Arrays.asList(name, "v1", "v1", "v2", "v2");
        List<String> failCols = Arrays.asList(name, "v1", "wrong", "null", "null");
        if (isColumnSuccess) {
            for (int i = 0; i < cols.size(); i++) {
                tmr.addColumnStatus(cols.get(i), Statuses.SUCCESS, expCols.get(i), expCols.get(i));
            }
        } else {
            for (int i = 0; i < cols.size(); i++) {
                Statuses status = expCols.get(i).equals(failCols.get(i)) ? Statuses.SUCCESS : Statuses.FAIL;
                tmr.addColumnStatus(cols.get(i), status, expCols.get(i), failCols.get(i));
            }
        }
        Link link = new Link(TEST_DIRECTORY.get().resolve(name).toString(), name);
        try (BufferedWriter wr = new BufferedWriter(new FileWriter(link.getPath()))) {
            wr.write("," + String.join(",", cols) + "\n");
            wr.write("ER," + String.join(",", expCols) + "\n");
            wr.write("AR," + String.join(",", isColumnSuccess ? expCols : failCols) + "\n");
        } catch (IOException e) {
            Assert.fail("fill validation error: " + e.getMessage());
        }
        return fillValidation(link, query, tmr);
    }
}
