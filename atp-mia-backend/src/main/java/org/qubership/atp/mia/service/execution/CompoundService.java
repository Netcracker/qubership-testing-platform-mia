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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.integration.configuration.annotation.AtpSpanTag;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.configuration.CompoundHasNoProcessesException;
import org.qubership.atp.mia.exceptions.configuration.NotACompoundException;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.impl.request.ProcessRequest;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.SseEmitterService;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.HttpUtils;
import org.qubership.atp.mia.utils.Utils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompoundService {

    private final MiaContext miaContext;
    private final ProcessService processService;
    private final MetricsAggregateService metricsService;
    private final String miaEntityUrlFormat;
    private final SseEmitterService sseEmitterService;

    /**
     * Executes compound.
     */
    @AtpJaegerLog(spanTags = @AtpSpanTag(key = "compound.name", value = "#request.compound.name"))
    public LinkedList<ExecutionResponse> executeCompound(ExecutionRequest request, UUID sseId) {
        String compoundName = request.getCompound() == null ? null : request.getCompound().getName();
        log.info("Execute compound with name '{}'", compoundName);
        LinkedList<ExecutionResponse> responses = new LinkedList<>();
        ProjectConfiguration config = miaContext.getConfig();
        UUID projectId = config.getProjectId();
        metricsService.compoundExecutionWasStarted();
        if (request.getCompound() != null) {
            try {
                final CompoundConfiguration compound = config.getCompoundByName(compoundName);
                MDC.put("miaCompoundId", compound.getId().toString());
                if (skipByReferToInput(compound.getReferToInput())) {
                    String warning = "SKIPPED Compound '" + compound.getName()
                            + "' for project with ID " + projectId
                            + " due to '" + compound.getReferToInput() + "' parameter";
                    log.warn(warning);
                    ExecutionResponse response = new ExecutionResponse();
                    response.setProcessName(compound.getProcesses().get(0).getName());
                    response.setWarn(warning);
                    responses.add(response);
                    sseEmitterService.updateResponseAndSendToEmitter(response, sseId, true, 0);
                    return responses;
                }
                List<Switcher> systemSwitchersBe = miaContext.getHeaderConfiguration().getSystemSwitchers();
                processService.getActualStateOfSwitchers(systemSwitchersBe, request.getSystemSwitchers());
                boolean stopOnFail = Utils.getSystemSwitcherByName(Constants.SystemSwitcherNames.stopOnFail,
                        systemSwitchersBe).isValue();
                log.info("Define process to execute of compound with name '{}'", compoundName);
                List<ProcessConfiguration> processesList = compound.getProcesses();
                if (processesList == null) {
                    throw new CompoundHasNoProcessesException(compound.getName());
                }
                List<ProcessRequest> processesListFromRequest =
                        request.getCompound() != null && request.getCompound().getProcessList() != null
                                ? request.getCompound().getProcessList()
                                : new ArrayList<>();
                for (int processId = 0; processId < processesList.size(); processId++) {
                    ProcessConfiguration proc = processesList.get(processId);
                    ExecutionResponse response = new ExecutionResponse();
                    response.setProcessName(proc.getName());
                    try {
                        request.setProcess(proc.getName());
                        if (processId < processesListFromRequest.size()) {
                            ProcessRequest requestProcess = processesListFromRequest.get(processId);
                            if ("MIA_REEXECUTE_SKIP".equals(requestProcess.getName())) {
                                response.setWarn("Process skipped due to re-execution");
                                continue;
                            }
                            if (requestProcess.getName().equals(proc.getName())
                                    && requestProcess.getCommand() != null) {
                                if (!Strings.isNullOrEmpty(requestProcess.getCommand()
                                        .getValue())) {
                                    request.setCommand(requestProcess.getCommand().getValue());
                                } else {
                                    request.setCommand(null);
                                }
                                request.setRest(requestProcess.getCommand().getRest());
                            }
                        }
                        if (skipByReferToInput(proc.getProcessSettings().getReferToInput())) {
                            String warning = "SKIPPED process " + proc.getName()
                                    + " in scope of Compound '" + compound.getName()
                                    + "' for project with ID " + projectId
                                    + " due to '" + proc.getProcessSettings().getReferToInput() + "' parameter";
                            log.warn(warning);
                            response.setWarn(warning);
                        } else {
                            response = processService.executeProcess(request, null);
                            response.setEntityId(compound.getId());
                            response.setEntityUrl(HttpUtils.getMiaEntityUrl(
                                    miaEntityUrlFormat,
                                    miaContext.getProjectId(),
                                    compound.getId()));
                        }
                    } catch (MiaException e) {
                        log.error("{} Error during process execution {}", e.getReason(), e.getMessage());
                        response.setError(e);
                    } catch (Exception e) {
                        log.error("Error during process execution {}", e.getMessage());
                        response.setError(new MiaException(e));
                    } finally {
                        responses.add(response);
                        request.setProcess(null);
                        request.setCommand(null);
                        if (response.getGlobalVariables() != null) {
                            miaContext.getFlowData().addParameters(response.getGlobalVariables());
                        }
                    }
                    final boolean statusFail = response.getProcessStatus() != null
                            && response.getProcessStatus().getStatus() == Statuses.FAIL;
                    if (stopOnFail && (response.getError() != null || statusFail)) {
                        log.warn(ErrorCodes.MIA_0001_STOP_ON_FAIL.getMessage(proc.getName(),
                                response.getError() == null
                                        ? "response don't have error"
                                        : "status is FAIL"));
                        sseEmitterService.updateResponseAndSendToEmitter(response, sseId, true, processId);
                        break;
                    }
                    sseEmitterService.updateResponseAndSendToEmitter(response, sseId,
                            processId == processesList.size() - 1, processId);
                }
            } catch (ClassCastException e) {
                MiaException exception = new NotACompoundException(request.getCompound().getName());
                if (sseId != null) {
                    sseEmitterService.sendError(sseId, exception);
                } else {
                    throw exception;
                }
            }
        }
        return responses;
    }

    /**
     * Check if process/compound should be skip or not.
     *
     * @param referToInput name of input
     * @return true if should be skip, false otherwise
     */
    private boolean skipByReferToInput(String referToInput) {
        boolean skip = false;
        if (!Strings.isNullOrEmpty(referToInput)) {
            Map<String, String> parameters = miaContext.getFlowData().getParameters();
            skip = !parameters.containsKey(referToInput)
                    || parameters.entrySet().stream()
                    .anyMatch(stringStringEntry -> stringStringEntry.getKey().equals(referToInput)
                            && (Strings.isNullOrEmpty(stringStringEntry.getValue())
                            || "false".equals(stringStringEntry.getValue())));
        }
        return skip;
    }

}
