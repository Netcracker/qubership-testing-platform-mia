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

package org.qubership.atp.mia.controllers;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.SseEmitterService;
import org.qubership.atp.mia.service.execution.CompoundService;
import org.qubership.atp.mia.service.execution.MiaExecutionThreadPool;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final CompoundService compoundService;
    private final ProcessService processService;
    private final ContextRepository contextRepository;
    private final MetricsAggregateService metricsService;
    private final MiaContext miaContext;
    private final MiaExecutionThreadPool miaExecutionThreadPool;
    private final SseEmitterService sseEmitterService;

    /**
     * Endpoint to create SSE-emitter.
     *
     * @return created emitter for particular request identifier
     */
    @AuditAction(auditAction = "Connect to SSE emitter with id '{{#sseId}}' for the '{{#projectId}}' project")
    @PreAuthorize("@entityAccess.checkAccess(#projectId,'EXECUTE')")
    @GetMapping(value = "/sse/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam UUID projectId,
                              @RequestParam UUID sseId,
                              @RequestHeader(value = HttpHeaders.AUTHORIZATION) String token) throws IOException {
        SseEmitter emitter = sseEmitterService.getEmitter(sseId);
        if (emitter == null) {
            emitter = sseEmitterService.generateAndConfigureEmitter(sseId, token);
        }
        return emitter;
    }

    /**
     * Executes compound.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(),"
            + "#projectId, 'EXECUTE')")
    @PostMapping(value = "/sse/compound")
    @AuditAction(auditAction = "Execute for compound \"{{#request.compound.name}}\" in Project - {{#projectId}}")
    public void executeCompound(
            @RequestParam(value = "projectId") UUID projectId,
            @RequestParam(value = "env") String env,
            @RequestParam(value = "sseId") UUID sseId,
            @RequestBody() ExecutionRequest request) {
        executeCompoundOrProcess(projectId, env, sseId, request, compoundService::executeCompound);
    }

    /**
     * Executes compound.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(),"
            + "#projectId, 'EXECUTE')")
    @PostMapping(value = "/sse/process")
    @AuditAction(auditAction = "Execute process \"{{#request.process}}\" in Project - {{#projectId}}")
    public void executeProcess(
            @RequestParam(value = "projectId") UUID projectId,
            @RequestParam(value = "env") String env,
            @RequestParam(value = "sseId") UUID sseId,
            @RequestBody() ExecutionRequest request) {
        executeCompoundOrProcess(projectId, env, sseId, request, processService::executeProcess);
    }

    private void executeCompoundOrProcess(UUID projectId, String env, UUID sseId, ExecutionRequest request,
                                          BiConsumer<ExecutionRequest, UUID> consumer) {
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        miaExecutionThreadPool.execute(DelegatingSecurityContextRunnable.create(() -> {
            try {
                MdcUtils.setContextMap(mdcMap);
                log.debug("Set mia context with project ID '{}'", projectId);
                miaContext.setContext(request, projectId, env);
                if (request.getFlowData() != null && request.getFlowData().getParameters() != null) {
                    metricsService.requestContextSize(request.getFlowData().getParameters().size());
                }
                consumer.accept(request, sseId);
            } catch (MiaException e) {
                sseEmitterService.sendError(sseId, e);
            } catch (Exception e) {
                sseEmitterService.sendError(sseId, new MiaException(e));
            } finally {
                contextRepository.removeContext();
            }
        }, SecurityContextHolder.getContext()));
    }
}
