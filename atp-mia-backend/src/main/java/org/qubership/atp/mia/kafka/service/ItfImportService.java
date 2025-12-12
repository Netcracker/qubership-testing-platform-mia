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

package org.qubership.atp.mia.kafka.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.mia.exceptions.itflite.ItfEmptyIdException;
import org.qubership.atp.mia.exceptions.itflite.ItfMethodEmptyException;
import org.qubership.atp.mia.exceptions.itflite.ItfMiaPathEmptyException;
import org.qubership.atp.mia.exceptions.itflite.ItfProcessNameEmptyException;
import org.qubership.atp.mia.exceptions.itflite.ItfProjectIdEmptyException;
import org.qubership.atp.mia.exceptions.itflite.ItfSameRequestInProgressException;
import org.qubership.atp.mia.exceptions.itflite.ItfUrlEmptyException;
import org.qubership.atp.mia.exceptions.itflite.ItfUuidEmptyException;
import org.qubership.atp.mia.exceptions.itflite.ProcessNotCreatedException;
import org.qubership.atp.mia.kafka.model.KafkaRequestImport;
import org.qubership.atp.mia.kafka.model.KafkaRequestImportDetails;
import org.qubership.atp.mia.kafka.model.KafkaResponseImport;
import org.qubership.atp.mia.kafka.model.KafkaResponseImportStatus;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.model.impl.executable.Soap;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.configuration.ProcessConfigurationService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.configuration.SectionConfigurationService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "kafka.enable")
public class ItfImportService {

    private final MiaContext miaContext;
    private final ProjectConfigurationService configurationService;
    private final ProcessConfigurationService processConfigurationService;
    private final SectionConfigurationService sectionConfigurationService;
    private final LockManager lockManager;
    private final ItfImportFinishNotificationService itfImportFinishNotificationService;
    private final Map<String, String> requestMap = new HashMap<>();

    /**
     * error message response to ITF.
     */
    public KafkaResponseImport generateErrorImportResponse(KafkaRequestImport request, Exception e) {
        KafkaResponseImport response = generateImportResponse(request);
        response.setStatus(KafkaResponseImportStatus.ERROR);
        response.setErrorMessage(e.getMessage());
        return response;
    }

    /**
     * successful message response to ITF.
     */
    public KafkaResponseImport generateFinishedImportResponse(KafkaRequestImport request, String processName) {
        KafkaResponseImport response = generateImportResponse(request);
        response.setStatus(KafkaResponseImportStatus.DONE);
        response.setMiaUrl(miaContext.prepareMiaURL(request.getMiaPath(), processName));
        response.setErrorMessage("");
        return response;
    }

    /**
     * Validate Itf request paramter.
     * Create thread
     * map rest and soap type request
     * create process
     */
    public void importRequest(KafkaRequestImport request) {
        miaContext.setContext(UUID.fromString(request.getProjectId()), null);
        lockManager.executeWithLock("itf request " + request.getMiaPath() + " " + request.getProjectId(), () -> {
            if (validateRequestInputs(request)) {
                String mapKey = request.getMiaPath() + request.getProjectId();
                if (requestMap.containsKey(mapKey) && !requestMap.get(mapKey)
                        .equals(request.getRequest().getId())) {
                    throw new ItfSameRequestInProgressException(requestMap.get(mapKey));
                }
                requestMap.put(mapKey, request.getRequest().getId());
                try {
                    execute(request);
                } finally {
                    requestMap.remove(mapKey);
                }
            }
        });
    }

    private void execute(KafkaRequestImport request) {
        final String processName = (request.getMiaProcessName() + "_" + request.getId()).replaceAll("[- ]", "");
        Utils.nameProcessValidator(processName);
        log.info("Create process for ID: {} and name: {}", request.getId(), processName);
        processConfigurationService.addProcess(
                configurationService.getConfigByProjectId(UUID.fromString(request.getProjectId())),
                processConfigurationService.toDto(mapToProcess(request, processName)));
        log.info("Process {} is created.", processName);
        if (miaContext.getConfig().getProcesses().stream().anyMatch(p -> p.getName().equals(processName))) {
            log.info("Sending response to Itf for ID: {} and process : {} ", request.getId(), processName);
            sendResponseToItf(request, processName);
        } else {
            throw new ProcessNotCreatedException();
        }
    }

    private KafkaResponseImport generateImportResponse(@NonNull KafkaRequestImport request) {
        final String requestId = request.getId();
        final String itfRequestId = request.getRequest().getId();
        return new KafkaResponseImport(requestId, itfRequestId);
    }

    private ProcessConfiguration mapToProcess(KafkaRequestImport request, String processName) {
        final Command newCommand;
        String url = Utils.urlEvaluator(request.getRequest().getUrl(),
                request.getRequest().getQueryParameters());
        String body = request.getRequest().getBody() != null
                ? request.getRequest().getBody().getContent()
                : "";
        if (request.getRequest().getTransportType()
                == KafkaRequestImportDetails.KafkaRequestImportType.REST) {
            String headers = Utils.prepareRestHeaders(request.getRequest().getRequestHeaders());
            newCommand = Command.builder().rest(Rest.builder()
                            .method(request.getRequest().getHttpMethod())
                            .body(body)
                            .endpoint(url)
                            .headers(headers)
                            .build())
                    .type(request.getRequest().getTransportType().toString())
                    .name(request.getMiaProcessName())
                    .build();
        } else {
            LinkedHashMap<String, String> soapDefaultValue = new LinkedHashMap<>();
            soapDefaultValue.put("default_key", "default_value");
            newCommand = Command.builder().soap(Soap.builder()
                            .endpoint(url)
                            .request(body)
                            .build())
                    .type(request.getRequest().getTransportType().toString())
                    .name(request.getMiaProcessName())
                    .atpValues(soapDefaultValue)
                    .build();
        }
        return ProcessConfiguration.builder()
                .name(processName)
                .inSections(Collections.singletonList(
                        sectionConfigurationService.findSectionByPath(UUID.fromString(request.getProjectId()),
                                request.getMiaPath())))
                .sections(Collections.singletonList(
                        sectionConfigurationService.findSectionByPath(UUID.fromString(request.getProjectId()),
                                request.getMiaPath()).getName()))
                .processSettings(ProcessSettings.builder()
                        .name(processName)
                        .command(newCommand)
                        .build())
                .build();
    }

    private String sendResponseToItf(KafkaRequestImport request, String processName) {
        KafkaResponseImport response = generateFinishedImportResponse(request, processName);
        itfImportFinishNotificationService.notify(UUID.fromString(request.getId()), response);
        return processName;
    }

    private boolean validateRequestInputs(KafkaRequestImport request) {
        if (Strings.isNullOrEmpty(request.getId())) {
            throw new ItfUuidEmptyException();
        } else if (Strings.isNullOrEmpty(request.getProjectId())) {
            throw new ItfProjectIdEmptyException();
        } else if (Strings.isNullOrEmpty(request.getMiaPath())) {
            throw new ItfMiaPathEmptyException();
        } else if (Strings.isNullOrEmpty(request.getMiaProcessName())) {
            throw new ItfProcessNameEmptyException();
        } else if (Strings.isNullOrEmpty(request.getRequest().getId())) {
            throw new ItfEmptyIdException();
        } else if (Strings.isNullOrEmpty(request.getRequest().getHttpMethod())) {
            throw new ItfMethodEmptyException();
        } else if (Strings.isNullOrEmpty(request.getRequest().getUrl())) {
            throw new ItfUrlEmptyException();
        }
        return true;
    }
}
