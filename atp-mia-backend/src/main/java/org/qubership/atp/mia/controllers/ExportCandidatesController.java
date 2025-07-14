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

import org.qubership.atp.mia.controllers.api.CandidatesApi;
import org.qubership.atp.mia.controllers.api.dto.candidates.ExportCandidateDto;
import org.qubership.atp.mia.service.CandidatesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController()
public class ExportCandidatesController implements CandidatesApi {

    private final CandidatesService candidatesService;

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"READ\")")
    public ResponseEntity<List<ExportCandidateDto>> getCandidates(UUID projectId) {
        return ResponseEntity.ok(candidatesService.getCandidates(projectId));
    }
}
