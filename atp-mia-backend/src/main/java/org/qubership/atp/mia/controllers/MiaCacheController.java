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

import java.util.UUID;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.mia.controllers.api.MiaCacheControllerApi;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.cache.MiaCacheService;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.qubership.atp.mia.service.execution.SshExecutionHelperService;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MiaCacheController implements MiaCacheControllerApi {

    private final MiaCacheService miaCacheService;
    private final SshExecutionHelperService sshExecutionHelperService;
    private final ProcessService processService;
    private final MiaContext miaContext;
    private final CacheManager cacheManager;

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Reset DB Cache for project : {{#projectId}} for environment : {{#env}}")
    public ResponseEntity<Boolean> resetDbCache(UUID projectId, String env) {
        miaContext.setContext(ExecutionRequest.builder().sessionId(UUID.randomUUID()).build(), projectId, env);
        return ResponseEntity.ok(processService.resetDbCache());
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Reset Environment Caches for project - {{#projectId}}")
    public ResponseEntity<Boolean> resetEnvironmentCaches(UUID projectId) {
        miaCacheService.clearEnvironmentsCache(cacheManager, projectId);
        sshExecutionHelperService.resetCache();
        return ResponseEntity.ok(true);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Reset Pool Cache for project - {{#projectId}}")
    public ResponseEntity<Boolean> resetPoolCache(UUID projectId) {
        sshExecutionHelperService.resetCache();
        return ResponseEntity.ok(true);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    public ResponseEntity<Boolean> resetConfigurationCache(UUID projectId) {
        miaCacheService.clearConfigurationCache(projectId);
        return ResponseEntity.ok(true);
    }
}
