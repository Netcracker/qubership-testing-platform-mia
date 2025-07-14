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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.service.execution.CompoundService;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IMPORTANT!!! The first parameter in the request must be projectId, the second must be env,
 * the third must be ExecutionRequest.
 */
@RequestMapping(value = "/rest")
@RestController()
@Slf4j
@RequiredArgsConstructor
public class MiaExecutionController /*implements MiaExecutionControllerApi*/ {

    private final ProcessService processService;
    private final CompoundService compoundService;

    /**
     * Executes compound.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(),"
            + "#projectId, 'EXECUTE')")
    @PostMapping(value = "/flow/execute/compound")
    @AuditAction(auditAction = "Execute Compound initiated for compound \"{{#request.compound.name}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<LinkedList<ExecutionResponse>> executeCompound(
            @RequestParam(value = "projectId") UUID projectId,
            @RequestParam(value = "env") String env,
            @RequestBody() ExecutionRequest request) {
        return ResponseEntity.ok(compoundService.executeCompound(request, null));
    }

    /**
     * Executes current statement queries.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(),"
            + "#projectId, \"EXECUTE\")")
    @PostMapping(value = "/flow/execute/current/statement")
    @AuditAction(auditAction = "Execute Current Statement from Process - \"{{#executableName}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<List<SqlResponse>> executeCurrentStatement(
            @RequestParam(value = "projectId") UUID projectId,
            @RequestParam(value = "env") String env,
            @RequestBody ExecutionRequest request) {
        return ResponseEntity.ok(processService.executeCurrentStatement(request.getProcess()));
    }

    /**
     * Executes process.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(),"
            + "#projectId, 'EXECUTE')")
    @PostMapping(value = "/flow/execute/process")
    @AuditAction(auditAction = "Execute Process initiated for Process - \"{{#request"
            + ".process}}\" in Project - {{#projectId}}")
    public ResponseEntity<ExecutionResponse> executeProcess(@RequestParam(value = "projectId") UUID projectId,
                                                            @RequestParam(value = "env") String env,
                                                            @RequestBody ExecutionRequest request) {
        return ResponseEntity.ok(processService.executeProcess(request, null));
    }

    /**
     * Get NextBillDate.
     */
    @PreAuthorize("@entityAccess.checkAccess(T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(),"
            + "#projectId, \"EXECUTE\")")
    @PostMapping(value = "/flow/calculateNextBillDate")
    @AuditAction(auditAction = "getNextBillDate called in project - {{#projectId}} for environment - {{#env}}")
    public String getNextBillDate(@RequestParam(value = "projectId") UUID projectId,
                                  @RequestParam(value = "env") String env,
                                  @RequestBody ExecutionRequest request) {
        return processService.getNextBillDate();
    }
}
