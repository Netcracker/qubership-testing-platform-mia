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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.exceptions.responseerrors.MarkerRegexException;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.output.MarkedContent;
import org.qubership.atp.mia.model.pot.Marker;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.repo.impl.ProcessStatusRepository;
import org.qubership.atp.mia.service.DeserializerConfigBaseTest;

@ExtendWith(SkipTestInJenkins.class)
public class ProcessStatusRepositoryTest extends ConfigTestBean {
    private final String PATH_ALL_MARKERS = "src/test/resources/CommandOutput/outputWithAllMarkers.log";
    private final String PATH_PASS_MARKER = "src/test/resources/CommandOutput/outputWithPassMarker.log";
    private final String PATH_WARN_MARKER = "src/test/resources/CommandOutput/outputWithWarnMarker.log";
    private final String PATH_FAIL_MARKER = "src/test/resources/CommandOutput/outputWithFailMarker.log";
    private final String PATH_WITHOUT_MARKERS = "src/test/resources/CommandOutput/outputWithoutMarkers.log";
    private final String PATH_EMPTY_FILE = "src/test/resources/CommandOutput/emptyOutput.log";
    private final String PATH_NOT_EXISTING_FILE = "src/test/resources/CommandOutput/notExistingFile.log";
    private final String LINE_TO_CHECK = "this line contains %s to check";
    private final String PASSED = "PassedMarker";
    private final String WARNING = "WarningMarker";
    private final String FAILED = "FailedMarker";
    private final String REGEXP_INCORRECT = ":{tail}";
    private final ThreadLocal<ProcessStatusRepository> processStatusRepository = new ThreadLocal<>();

    @BeforeEach
    public void initProcessStatusRepositoryTest() {
        ProcessStatusRepository repository = new ProcessStatusRepository(miaContext.get());
        repository.setLimitSizeBytes(1000000L);
        processStatusRepository.set(repository);
    }

    @Test
    public void shouldContainLastLineFromContent_WhenNoContentAccordingToMarkers() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_WITHOUT_MARKERS);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent =
                executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "one more text");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.WARNING);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.WARNING, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "one more text");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "one more text");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
    }

    @Test
    public void shouldContainLastLineFromContentWhenNoMarkers() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse, false);
        mockContentFile(executionResponse, PATH_WITHOUT_MARKERS);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "one more text");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.WARNING);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.WARNING, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "one more text");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "one more text");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
    }

    @Test
    public void shouldNotChangeProcessStatusWhenPassedMarkerFound() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_PASS_MARKER);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, PASSED));
        assertEquals(Statuses.SUCCESS, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.WARNING);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.WARNING, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, PASSED));
        assertEquals(Statuses.SUCCESS, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, PASSED));
        assertEquals(Statuses.SUCCESS, markedContent.get(0).getState());
    }

    @Test
    public void shouldSetProcessStatusFailWhenPassedMarkerAndFailWhenNoPassedMarkersFoundTrue() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_WARN_MARKER);
        //action
        executionResponse.getProcessStatus().getMarker().setFailWhenNoPassedMarkersFound(true);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.WARNING);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(0).getState());
    }

    @Test
    public void shouldSetProcessStatusWarnWhenWarnMarkerFound() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_WARN_MARKER);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.WARNING, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.WARNING);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.WARNING, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(0).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(0).getState());
    }

    @Test
    public void shouldSetProcessStatusFailWhenFailMarkerFound() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_ALL_MARKERS);
        //action
        executionResponse.getProcessStatus().getMarker().setFailWhenNoPassedMarkersFound(true);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, FAILED));
        assertEquals(Statuses.FAIL, markedContent.get(0).getState());
        assertEquals(markedContent.get(1).getText(), String.format(LINE_TO_CHECK, PASSED));
        assertEquals(Statuses.SUCCESS, markedContent.get(1).getState());
        assertEquals(markedContent.get(2).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(2).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.WARNING);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, FAILED));
        assertEquals(Statuses.FAIL, markedContent.get(0).getState());
        assertEquals(markedContent.get(1).getText(), String.format(LINE_TO_CHECK, PASSED));
        assertEquals(Statuses.SUCCESS, markedContent.get(1).getState());
        assertEquals(markedContent.get(2).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(2).getState());
        //action
        executionResponse.getProcessStatus().setStatus(Statuses.FAIL);
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
        markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(markedContent.get(0).getText(), String.format(LINE_TO_CHECK, FAILED));
        assertEquals(Statuses.FAIL, markedContent.get(0).getState());
        assertEquals(markedContent.get(1).getText(), String.format(LINE_TO_CHECK, PASSED));
        assertEquals(Statuses.SUCCESS, markedContent.get(1).getState());
        assertEquals(markedContent.get(2).getText(), String.format(LINE_TO_CHECK, WARNING));
        assertEquals(Statuses.WARNING, markedContent.get(2).getState());
    }

    @Test
    public void actualReturnCodeIsEqualToExpected() {
        ExecutionResponse response = generateResponseWithCode(true, "200", Collections.singletonList("200"));
        ProcessStatus status = new ProcessStatus();
        status.setStatus(Statuses.SUCCESS);
        response.setProcessStatus(status);
        processStatusRepository.get().parseReturnCodeAndUpdateStatus(response);
        Assert.assertNotNull("Status null", response.getProcessStatus());
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
    }

    @Test
    public void shouldContainEmptyString_whenNoMarkersAndEmptyLogFile() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse, false);
        mockContentFile(executionResponse, PATH_EMPTY_FILE);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
    }

    @Test
    public void shouldContainEmptyString_whenPresentMarkersAndEmptyLogFile() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_EMPTY_FILE);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), "");
        assertEquals(Statuses.UNKNOWN, markedContent.get(0).getState());
    }

    @Test
    public void shouldReturnErrorLine_whenNoLogFile() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_NOT_EXISTING_FILE);
        String expectedError = ErrorCodes.MIA_2053_READ_FAIL_FILE_NOT_FOUND
                .getMessage(new File(PATH_NOT_EXISTING_FILE).toPath());
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(markedContent.get(0).getText(), expectedError);
        assertEquals(Statuses.FAIL, markedContent.get(0).getState());
    }

    /**
     * With new changes, checkContentAndGetNewStatus method calls only once from parseLogStatus method.
     * Hence the status should be SUCCESS instad of Failure.
     */
    @Test
    public void shouldMarkPass_whenSuccessAndFailMarkedContent() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        mockContentFile(executionResponse, PATH_WITHOUT_MARKERS);
        mockContentFile(executionResponse, PATH_FAIL_MARKER);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(3, markedContent.size());
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
    }

    @Test
    public void shouldTakeTenLines_whenSizeIsMoreThanTen() {
        int amount = 100;
        int expectedAmount = 10;
        ExecutionResponse executionResponse = new ExecutionResponse();
        LinkedList<MarkedContent> markedContent = runParsingLogWithFixedLinesAmount(amount, PATH_WITHOUT_MARKERS, executionResponse)
                .get(0).getMarkedContent();
        assertEquals(expectedAmount, markedContent.size());
    }

    @Test
    public void shouldTakeSevenLines_whenSizeIsSeven() {
        int amount = 7;
        ExecutionResponse executionResponse = new ExecutionResponse();
        LinkedList<MarkedContent> markedContent = runParsingLogWithFixedLinesAmount(amount, PATH_WITHOUT_MARKERS, executionResponse)
                .get(0).getMarkedContent();
        assertEquals(amount, markedContent.size());
    }

    @Test
    public void shouldTakeZeroLines_whenSizeIsZero() {
        int amount = 0;
        ExecutionResponse executionResponse = new ExecutionResponse();
        LinkedList<MarkedContent> markedContent = runParsingLogWithFixedLinesAmount(amount, PATH_WITHOUT_MARKERS, executionResponse)
                .get(0).getMarkedContent();
        assertEquals(amount, markedContent.size());
    }

    @Test
    public void shouldTake5LinesBoth_whenDoubleOutput() {
        int amount = 5;
        ExecutionResponse executionResponse = new ExecutionResponse();
        mockContentFile(executionResponse, PATH_WITHOUT_MARKERS);
        List<CommandOutput> out = runParsingLogWithFixedLinesAmount(amount, PATH_WITHOUT_MARKERS, executionResponse);
        assertEquals(amount, out.get(0).getMarkedContent().size());
        assertEquals(amount, out.get(1).getMarkedContent().size());
    }

    /**
     * Logs should be marked for every new input.
     * It checks that Command marker from Process should be immutable,
     * especially after evaluation.
     */
    @Test
    public void evaluateInParseMarkers_shouldNotChangeFlowDataMarker() {
        //preparation
        ProcessConfiguration process = DeserializerConfigBaseTest.getBgWithMarker();
        Command command = process.getProcessSettings().getCommand();
        ExecutionResponse executionResponse = new ExecutionResponse();
        executionResponse.setProcessStatus(new ProcessStatus());
        Marker expectMarker = getMarker(PASSED, null, null);
        expectMarker.setFailWhenNoPassedMarkersFound(true);
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter("passMarker", PASSED);
        //action
        processStatusRepository.get().parseLogMarkers(executionResponse.getProcessStatus(), command);
        //check
        assertEquals(expectMarker, executionResponse.getProcessStatus().getMarker());
        assertNotEquals(DeserializerConfigBaseTest.getBgWithMarker().getProcessSettings().getCommand().getMarker(), command.getMarker());
    }

    /**
     * If we have warningMarker = "" (empty marker), then it shouldn't be parsed (treated as null).
     */
    @Test
    public void shouldNotMarkEveryLineWithWarning() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse, false);
        executionResponse.getProcessStatus().setMarker(getMarker(PASSED, "", null));
        mockContentFile(executionResponse, PATH_PASS_MARKER);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        LinkedList<MarkedContent> markedContent = executionResponse.getCommandResponse().getCommandOutputs().get(0).getMarkedContent();
        assertEquals(1, markedContent.size());
        assertEquals(Statuses.SUCCESS, markedContent.getFirst().getState());
        assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
    }

    @Test
    public void bodyShouldHaveErrorDescription_whenMarkerFailed() {
        //preparation
        String expectedErr = "passed markers not found and \"Fail when No Passed Markers Found\" flag is true";
        ExecutionResponse executionResponse = new ExecutionResponse();
        preparationForParseLogStatus(executionResponse);
        executionResponse.getProcessStatus().setMarker(getMarker(PASSED, null, FAILED, true));
        mockContentFile(executionResponse, PATH_WITHOUT_MARKERS);
        //action
        processStatusRepository.get().parseLogStatus(executionResponse);
        //check result
        Exception err = executionResponse.getError();
        Assert.assertNotNull("Error field shouldn't be null!", err);
        Assert.assertEquals("Error message not equal to expected", expectedErr, err.getMessage());
    }

    @Test
    public void expectedCodesWithRegularExpression() {
        ExecutionResponse response = generateResponseWithCode(true, "404", Collections.singletonList("40*"));
        ProcessStatus ps = new ProcessStatus();
        ps.setStatus(Statuses.SUCCESS);
        response.setProcessStatus(ps);
        processStatusRepository.get().parseReturnCodeAndUpdateStatus(response);
        Assert.assertNotNull("Status null", response.getProcessStatus());
        Assert.assertEquals(Statuses.SUCCESS, response.getProcessStatus().getStatus());
    }

    @Test
    public void actualReturnCodeNotEqualToExpected() {
        ExecutionResponse response = generateResponseWithCode(true, "200", Collections.singletonList("40*"));
        processStatusRepository.get().parseReturnCodeAndUpdateStatus(response);
        Assert.assertNotNull("Status null", response.getProcessStatus());
        Assert.assertEquals(Statuses.FAIL, response.getProcessStatus().getStatus());
    }

    @Test
    public void actualReturnCodeIsNotEqual_regexCheck() {
        ExecutionResponse response = generateResponseWithCode(true, "2000", Collections.singletonList("20*"));
        processStatusRepository.get().parseReturnCodeAndUpdateStatus(response);
        Assert.assertNotNull("Status null", response.getProcessStatus());
        Assert.assertEquals("2000 should not match 20* regex (* is like \\\\d)", Statuses.FAIL,
                response.getProcessStatus().getStatus());
    }

    @Test
    public void actualReturnCodeIsNotEqual_regexCheckSymbol() {
        ExecutionResponse response = generateResponseWithCode(true, "200a", Collections.singletonList("20*"));
        processStatusRepository.get().parseReturnCodeAndUpdateStatus(response);
        Assert.assertNotNull("Status null", response.getProcessStatus());
        Assert.assertEquals("20a should not match 20* regex (* is like \\\\d)", Statuses.FAIL,
                response.getProcessStatus().getStatus());
    }

    @Test
    public void shouldCopyMarkerFromCommandToProcessAndEvaluate() {
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        executionResponse.setProcessStatus(new ProcessStatus());
        Command command = new Command();
        command.setMarker(getMarker(":passM", ":warnM", ":failM"));
        Marker expectMarker = getMarker(PASSED, WARNING, FAILED);
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter("passM", PASSED);
        flowData.addParameter("warnM", WARNING);
        flowData.addParameter("failM", FAILED);
        //action
        processStatusRepository.get().parseLogMarkers(executionResponse.getProcessStatus(), command);
        //check
        assertEquals(expectMarker, executionResponse.getProcessStatus().getMarker());
    }

    @Test
    public void passMarkerShouldThrowException_whenIncorrectRegexp() {
        checkParseMarkerWillThrowException_whenIncorrectRegexpInMarker(getMarker(REGEXP_INCORRECT, null, null));
    }

    @Test
    public void warnMarkerShouldThrowException_whenIncorrectRegexp() {
        checkParseMarkerWillThrowException_whenIncorrectRegexpInMarker(getMarker(null, REGEXP_INCORRECT, null));
    }

    @Test
    public void failMarkerShouldThrowException_whenIncorrectRegexp() {
        checkParseMarkerWillThrowException_whenIncorrectRegexpInMarker(getMarker(null, null, REGEXP_INCORRECT));
    }

    private void checkParseMarkerWillThrowException_whenIncorrectRegexpInMarker(Marker marker) {
        String markerRegex;
        String erroredMarker;
        if (marker.getFailedMarkersForLog() != null) {
            erroredMarker = "failed";
            markerRegex = marker.getFailedMarkersForLog().get(0);
        } else if (marker.getWarnMarkersForLog() != null) {
            erroredMarker = "warn";
            markerRegex = marker.getWarnMarkersForLog().get(0);
        } else if (marker.getPassedMarkerForLog() != null) {
            erroredMarker = "passed";
            markerRegex = marker.getPassedMarkerForLog().get(0);
        } else {
            erroredMarker = null;
            markerRegex = null;
            Assert.fail("Make sure you set Marker before use this method!");
        }
        //expected
        MarkerRegexException expected = new MarkerRegexException(erroredMarker, markerRegex);
        //preparation
        ExecutionResponse executionResponse = new ExecutionResponse();
        ProcessStatus processStatus = new ProcessStatus();
        processStatus.setMarker(marker);
        processStatus.setStatus(Statuses.SUCCESS);
        executionResponse.setProcessStatus(processStatus);
        mockContentFile(executionResponse, PATH_WITHOUT_MARKERS);
        //action
        try {
            processStatusRepository.get().parseLogStatus(executionResponse);
            Assert.fail("MiaException MIA-0260 should be thrown");
        } catch (MarkerRegexException e) {
            Assert.assertEquals(e.getMessage(), expected.getMessage());
        }
    }

    private List<CommandOutput> runParsingLogWithFixedLinesAmount(int amount, String filePath) {
        return runParsingLogWithFixedLinesAmount(amount, filePath, new ExecutionResponse());
    }

    private List<CommandOutput> runParsingLogWithFixedLinesAmount(int amount, String filePath, ExecutionResponse resp) {
        preparationForParseLogStatus(resp);
        mockContentFile(resp, filePath);
        processStatusRepository.get().parseLogStatus(resp, amount);
        return resp.getCommandResponse().getCommandOutputs();
    }

    private void preparationForParseLogStatus(ExecutionResponse executionResponse) {
        preparationForParseLogStatus(executionResponse, true);
    }

    private void preparationForParseLogStatus(ExecutionResponse executionResponse, boolean isNeedToSetMarker) {
        ProcessStatus processStatus = new ProcessStatus();
        if (isNeedToSetMarker) {
            processStatus.setMarker(getMarker(PASSED, WARNING, FAILED));
        }
        processStatus.setStatus(Statuses.SUCCESS);
        executionResponse.setProcessStatus(processStatus);
    }

    private Marker getMarker(String pass, String warn, String fail) {
        return getMarker(pass, warn, fail, false);
    }

    private Marker getMarker(String pass, String warn, String fail, boolean failWhenNoPass) {
        Marker marker = Marker.builder().build();
        if (pass != null) {
            marker.setPassedMarkerForLog(Collections.singletonList(pass));
        }
        if (warn != null) {
            marker.setWarnMarkersForLog(Collections.singletonList(warn));
        }
        if (fail != null) {
            marker.setFailedMarkersForLog(Collections.singletonList(fail));
        }
        marker.setFailWhenNoPassedMarkersFound(failWhenNoPass);
        return marker;
    }

    private void mockContentFile(ExecutionResponse executionResponse, String filePath) {
        CommandOutput commandOutput = new CommandOutput(filePath, filePath, true, miaContext.get());
        LinkedList<CommandOutput> listOutputs;
        try {
            listOutputs = executionResponse.getCommandResponse().getCommandOutputs();
        } catch (NullPointerException ignore) {
            listOutputs = new LinkedList<>();
        }
        listOutputs.add(commandOutput);
        executionResponse.setCommandResponse(new CommandResponse(listOutputs));
    }

    private ExecutionResponse generateResponseWithCode(boolean checkCodeFlag, String statusCode,
                                                       List<String> expectedCodes) {
        HashMap<String, String> connectionInfo = new HashMap<String, String>() {{
            put("code", statusCode);
        }};
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setConnectionInfo(connectionInfo);
        commandResponse.setCheckStatusCodeFlag(checkCodeFlag);
        if (checkCodeFlag) {
            commandResponse.setStatusCode(statusCode);
            commandResponse.setExpectedCodes(expectedCodes);
        }
        ExecutionResponse response = new ExecutionResponse();
        response.setCommandResponse(commandResponse);
        return response;
    }
}
