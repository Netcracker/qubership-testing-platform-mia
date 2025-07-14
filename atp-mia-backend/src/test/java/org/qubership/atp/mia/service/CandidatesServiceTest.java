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

package org.qubership.atp.mia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.controllers.api.dto.candidates.ExportCandidateDto;

class CandidatesServiceTest extends BaseUnitTestConfiguration {

    private ThreadLocal<CandidatesService> candidatesService = new ThreadLocal<>();

    @BeforeEach
    void candidatesServiceTestSetUp() {
        candidatesService.set(spy(new CandidatesService(projectConfigurationService.get())));
    }

    @Test
    void getCandidates() {
        when(projectConfigurationService.get().findByProjectId(projectId.get())).thenReturn(Optional.of(testProjectConfiguration.get()));
        List<ExportCandidateDto> candidates = candidatesService.get().getCandidates(projectId.get());
        assertEquals(3, candidates.size());
        assertEquals(1, candidates.get(0).getEntities().size());
        assertEquals(3, candidates.get(1).getEntities().size());
        assertEquals(1, candidates.get(2).getEntities().size());
        assertEquals("Child1", candidates.get(2).getEntities().get(0).getName());
    }
}
