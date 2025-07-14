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
import org.qubership.atp.mia.controllers.api.MiaServiceControllerApi;
import org.qubership.atp.mia.service.execution.TimeShiftService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MiaServiceController implements MiaServiceControllerApi {

    private final GridFsService gridFsService;
    private final TimeShiftService timeShiftService;

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'READ')")
    @AuditAction(auditAction = "Get dbSize called from project {{#projectId}}")
    public ResponseEntity<String> dbSize(UUID projectId) {
        return ResponseEntity.ok(gridFsService.getCollectionsSize());
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'READ')")
    @AuditAction(auditAction = "Time shifting check in Project : {{#projectId}}")
    public ResponseEntity<Boolean> getTimeShifting(UUID projectId, String systemId) {
        return ResponseEntity.ok(timeShiftService.checkTimeShifting(systemId));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Update TimeShifting value (Enable/Disable), in Project : {{#projectId}}")
    public ResponseEntity<Boolean> updateTimeShifting(UUID projectId, UUID systemId, Boolean value) {
        timeShiftService.updateTimeShifting(systemId, value);
        return ResponseEntity.ok(true);
    }
}
