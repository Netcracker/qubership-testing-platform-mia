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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.repo.impl.ProofOfTestingRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequestMapping(value = "/rest")
@RestController()
@RequiredArgsConstructor
public class MiaProofOfTestingController /*implements ProofOfTestingControllerApi*/ {

    private final MiaContext miaContext;
    private final ProofOfTestingRepository proofOfTestingRepository;

    /**
     * Start record session process.
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @PostMapping(value = "/pot/session/start")
    @AuditAction(auditAction = "Start MIA POT Session in project {{#projectId}}")
    public UUID startSession(@RequestParam(value = "projectId") UUID projectId,
                             @RequestParam(value = "sessionId", required = false) UUID oldSessionId) {
        return UUID.randomUUID();
    }

    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @PostMapping(value = "/pot/save")
    @AuditAction(auditAction = "Save MIA POT for sessionId {{#sessionId}} in project {{#projectId}}")
    public List<Link> saveProofOfTesting(@RequestParam(value = "projectId") UUID projectId,
                                         @RequestParam(value = "sessionId") UUID sessionId,
                                         @RequestBody ExecutionRequest request) {
        return proofOfTestingRepository.downloadProofOfTesting();
    }
}
