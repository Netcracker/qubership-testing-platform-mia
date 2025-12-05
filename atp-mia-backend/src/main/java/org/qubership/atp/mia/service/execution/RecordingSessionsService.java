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

package org.qubership.atp.mia.service.execution;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.text.StringEscapeUtils;
import org.owasp.encoder.Encode;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.entity.PotExecutionStep;
import org.qubership.atp.mia.model.pot.entity.PotSession;
import org.qubership.atp.mia.repo.db.RecordingSessionRepository;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingSessionsService {

    private final RecordingSessionRepository recordingSessionRepository;
    private final MiaContext miaContext;
    private final AtpUserService atpUserService;

    /**
     * Save executed step to session.
     */
    public PotSession addExecutionStep(ExecutionResponse executionResponse) {
        UUID sessionId = miaContext.getFlowData().getSessionId();
        log.info("Save execution step for session ID #{}", sessionId);
        Optional<PotSession> recordingSessionOptional = recordingSessionRepository.findById(sessionId);
        PotSession recordingSession = recordingSessionOptional.isPresent()
                ? recordingSessionOptional.get()
                : recordingSessionRepository.save(
                new PotSession(sessionId, miaContext.getConfig(), atpUserService.getAtpUser()));
        recordingSessionRepository
                .save(recordingSession.addExecutionStep(generateExecutionStepFromExecutionResponse(executionResponse)));
        log.info("Save execution step for session ID Ended");
        return recordingSession;
    }

    /**
     * Delete session from list.
     */
    public void deleteSession(UUID sessionId) {
        try {
            recordingSessionRepository.deleteById(sessionId);
        } catch (Exception e) {
            log.error("Error during session {} remove.", sessionId, e);
        }
    }

    /**
     * Get session by ID.
     *
     * @param sessionId session ID
     * @return RecordingSession
     */
    public Optional<PotSession> getSession(UUID sessionId) {
        return recordingSessionRepository.findById(sessionId);
    }

    /**
     * Delete old sessions.
     */
    public void deleteOldSession() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -3);
        log.info("With Scheduled CleanPostgresqlPot Cron Job, {} records got deleted.",
                recordingSessionRepository.deleteByCreatedAtBefore(new Timestamp(calendar.getTimeInMillis())));
    }

    private PotExecutionStep generateExecutionStepFromExecutionResponse(ExecutionResponse executionResponse) {
        final PotExecutionStep potExecutionStep = new PotExecutionStep();
        potExecutionStep.setStepName(executionResponse.getProcessName());
        potExecutionStep.setEnvironmentName(miaContext.getFlowData().getEnvironment().getName());
        potExecutionStep.setExecutedCommand(executionResponse.getExecutedCommand());
        List<Link> outputFiles = new ArrayList<>();
        if (executionResponse.getCommandResponse() != null) {
            if (executionResponse.getCommandResponse().getCommandOutputs() != null) {
                for (CommandOutput commandOutput : executionResponse.getCommandResponse().getCommandOutputs()) {
                    outputFiles.add(getLinkForPot(commandOutput.getInternalPathToFile(), commandOutput.getLink()));
                }
            }
            if (executionResponse.getCommandResponse().getSqlResponse() != null) {
                final SqlResponse sqlResponse = executionResponse.getCommandResponse().getSqlResponse();
                potExecutionStep.setExecutedCommand(sqlResponse.getQuery());
                if (sqlResponse.getLink() != null) {
                    outputFiles.add(getLinkForPot(sqlResponse.getInternalPathToFile(), sqlResponse.getLink()));
                }
            }
        }
        potExecutionStep.setLinks(outputFiles);
        List<SqlResponse> validations = new ArrayList<>();
        if (executionResponse.getValidations() != null) {
            for (SqlResponse sqlResponse : executionResponse.getValidations()) {
                SqlResponse validation = new SqlResponse();
                validation.setData(sqlResponse.getData());
                validation.setTableName(sqlResponse.getTableName());
                validation.setQuery(sqlResponse.getQuery());
                validation.setDescription(sqlResponse.getDescription());
                validation.setRecords(sqlResponse.getRecords());
                validation.setTableMarkerResult(sqlResponse.getTableMarkerResult());
                validation.setInternalPathToFile(sqlResponse.getInternalPathToFile(), miaContext);
                validation.setLink(getLinkForPot(sqlResponse.getInternalPathToFile(), sqlResponse.getLink()));
                validation.setSaveToWordFile(sqlResponse.isSaveToWordFile());
                validation.setSaveToZipFile(sqlResponse.isSaveToZipFile());
                validation.setConnectionInfo(sqlResponse.getConnectionInfo());
                validations.add(validation);
            }
        }
        potExecutionStep.setValidations(validations);
        potExecutionStep.setProcessStatus(executionResponse.getProcessStatus());
        log.debug("Added process status for recording session from Response: {}", executionResponse.getProcessStatus());
        List<Exception> errors = new ArrayList<>();
        if (executionResponse.getCommandResponse() != null
                && executionResponse.getCommandResponse().getErrors() != null) {
            errors.addAll(executionResponse.getCommandResponse().getErrors());
        }
        if (executionResponse.getError() != null) {
            errors.add(executionResponse.getError());
        }
        potExecutionStep.setErrors(errors);
        return potExecutionStep;
    }

    /**
     * Get correct link of file for POT. Replace path be internal link.
     *
     * @param path internal link
     * @param link file link
     * @return link
     */
    private Link getLinkForPot(String path, Link link) {
        //return new Link(path, link.getName());
        String safeName = Encode.forHtml(link.getName());
        return new Link(path, safeName);
    }
}
