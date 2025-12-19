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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.responseerrors.CantParseExpectedCodeException;
import org.qubership.atp.mia.exceptions.responseerrors.CodeNotPresentWhenFlagOnException;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.output.MarkedContent;
import org.qubership.atp.mia.model.pot.Marker;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProcessStatusRepository {

    private final MiaContext miaContext;
    @Setter
    @Value("${response.file.size.limit.bytes:1000000}")
    private Long limitSizeBytes;

    /**
     * Pass markers from command to processStatus with evaluation it according to FlowData.
     *
     * @param processStatus processStatus
     * @param command       command
     */
    public void parseLogMarkers(ProcessStatus processStatus, Command command) {
        if (command.getMarker() != null) {
            processStatus.setMarker(command.getMarker());
            Marker marker = processStatus.getMarker();
            if (marker.getPassedMarkerForLog() != null) {
                marker.setPassedMarkerForLog(marker.getPassedMarkerForLog().stream()
                        .map(miaContext::evaluate)
                        .collect(Collectors.toList()));
            }
            if (marker.getFailedMarkersForLog() != null) {
                marker.setFailedMarkersForLog(marker.getFailedMarkersForLog().stream()
                        .map(miaContext::evaluate)
                        .collect(Collectors.toList()));
            }
            if (marker.getWarnMarkersForLog() != null) {
                marker.setWarnMarkersForLog(marker.getWarnMarkersForLog().stream()
                        .map(miaContext::evaluate)
                        .collect(Collectors.toList()));
            }
        }
    }

    /**
     * Update process status according to log and markers.
     */
    public void parseLogStatus(ExecutionResponse executionResponse) {
        parseLogStatus(executionResponse, miaContext.getConfig().getCommonConfiguration().getLinesAmount());
    }

    /**
     * Update process status according to log and markers.
     *
     * @param executionResponse response.
     * @param linesAmount       amount of last lines to show in case if there is no markers.
     */
    public void parseLogStatus(ExecutionResponse executionResponse, int linesAmount) {
        Marker marker = executionResponse.getProcessStatus().getMarker();
        if (executionResponse.getCommandResponse() != null
                && executionResponse.getCommandResponse().getCommandOutputs() != null
                && !executionResponse.getCommandResponse().getCommandOutputs().isEmpty()) {
            boolean isFirst = true;
            for (CommandOutput logCommandOutput : executionResponse.getCommandResponse().getCommandOutputs()) {
                try {
                    if (marker != null) {
                        log.info("Add log Status on entry marker is: [{}]", marker);
                        //Update contents of logs
                        List<String> content = logCommandOutput.contentFromFile();
                        logCommandOutput.setMarkedContent(new LinkedList<>());
                        for (String s : content) {
                            Statuses statuses = marker.checkLineForMarker(s);
                            if (!statuses.equals(Statuses.UNKNOWN)) {
                                logCommandOutput.addContent(s, statuses);
                            }
                        }
                    }
                } catch (MiaException e) {
                    handlerFileNotFoundErr(executionResponse, logCommandOutput, linesAmount, e);
                }
                if (logCommandOutput.getMarkedContent().isEmpty()) {
                    logCommandOutput.saveLatestLineToContent(linesAmount, limitSizeBytes);
                }
                if (isFirst) {
                    checkContentAndGetNewStatus(logCommandOutput, executionResponse, marker);
                }
                isFirst = false;
            }
        }
    }

    private void checkContentAndGetNewStatus(CommandOutput logOutput, ExecutionResponse res, Marker marker) {
        MiaException error = res.getError();
        Statuses oldStatus = res.getProcessStatus().getStatus();
        Statuses newStatus = null;
        if (logOutput.containsMarkedContentWithState(Statuses.FAIL)) {
            //Update process status according to log status
            log.info("Set process status FAIL: found FAIL marker");
            newStatus = Statuses.FAIL;
        } else if (logOutput.containsMarkedContentWithState(Statuses.WARNING)
                && !oldStatus.equals(Statuses.FAIL)) {
            log.info("Set process status WARNING: found warning marker");
            newStatus = Statuses.WARNING;
        } else if (logOutput.containsMarkedContentWithState(Statuses.SUCCESS)
                && !oldStatus.equals(Statuses.FAIL)
                && !oldStatus.equals(Statuses.WARNING)) {
            log.info("Set process status SUCCESS: found passed marker");
            newStatus = Statuses.SUCCESS;
        }
        if (marker != null
                && marker.getPassedMarkerForLog() != null
                && !marker.getPassedMarkerForLog().isEmpty()
                && marker.isFailWhenNoPassedMarkersFound()
                && !logOutput.containsMarkedContentWithState(Statuses.SUCCESS)
                && !oldStatus.equals(Statuses.FAIL)) {
            String errMsg = "passed markers not found and \"Fail when No Passed Markers Found\" flag is true";
            log.info("Set process status FAIL: {}", errMsg);
            error = new MiaException(errMsg);
            newStatus = Statuses.FAIL;
        }
        res.getProcessStatus().setStatus(newStatus == null ? oldStatus : newStatus);
        res.setError(error);
    }

    private void handlerFileNotFoundErr(ExecutionResponse executionResponse,
                                        CommandOutput logCommandOutput,
                                        int linesAmount,
                                        MiaException e) {
        String expected = ErrorCodes.MIA_2053_READ_FAIL_FILE_NOT_FOUND.getCode();
        if (e.getMessage().contains(expected)) {
            try {
                List<MarkedContent> content = executionResponse.getCommandResponse()
                        .getCommandOutputs().getFirst().getMarkedContent();
                if (content != null && !content.isEmpty()) {
                    return;
                }
            } catch (NullPointerException ignore) {
                log.debug("content is empty, accessing last line [npe = {}]", e.getMessage());
            }
            logCommandOutput.saveLatestLineToContent(linesAmount, limitSizeBytes);
        } else {
            throw e;
        }
    }

    /**
     * Parses rest response and sets {@link Statuses#FAIL} if there is unexpected code in response.
     * In case if flag 'Check Status Code' turned off this funciton skipped.
     *
     * @param response http response
     */
    public void parseReturnCodeAndUpdateStatus(ExecutionResponse response) {
        if (response.getCommandResponse().isCheckStatusCodeFlag()) {
            List<String> codes = response.getCommandResponse().getExpectedCodes();
            String statusCode = response.getCommandResponse().getStatusCode();
            if (codes != null && !codes.isEmpty()) {
                boolean matches = false;
                for (String code : codes) {
                    if (code.contains("*")) {
                        code = code.replaceAll("\\*", "\\\\d");
                    }
                    try {
                        if (statusCode.matches(code)) {
                            matches = true;
                            break;
                        }
                    } catch (PatternSyntaxException e) {
                        throw new CantParseExpectedCodeException();
                    }
                }
                if (!matches) {
                    if (response.getProcessStatus() == null) {
                        response.setProcessStatus(new ProcessStatus());
                    }
                    response.getProcessStatus().setStatus(Statuses.FAIL);
                }
            } else {
                throw new CodeNotPresentWhenFlagOnException();
            }
        }
    }
}
