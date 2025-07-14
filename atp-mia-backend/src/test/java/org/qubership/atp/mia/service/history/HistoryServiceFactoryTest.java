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

package org.qubership.atp.mia.service.history;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.model.DateAuditorEntity;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.service.history.impl.HistoryServiceFactory;
import org.qubership.atp.mia.service.history.impl.ProjectConfigurationRetrieveHistoryService;
import org.qubership.atp.mia.service.history.impl.SectionRetrieveHistoryService;

public class HistoryServiceFactoryTest {
    private static ThreadLocal<HistoryServiceFactory> historyServiceFactory = new ThreadLocal<>();

    private static SectionRetrieveHistoryService sectionRetrieveHistoryService;
    private static ProjectConfigurationRetrieveHistoryService projectConfigurationRetrieveHistoryService;

    @BeforeAll
    static public void setUp() {
        sectionRetrieveHistoryService = mock(SectionRetrieveHistoryService.class);
        when(sectionRetrieveHistoryService.getItemType()).thenCallRealMethod();
        when(sectionRetrieveHistoryService.getEntityClass()).thenCallRealMethod();

        projectConfigurationRetrieveHistoryService = mock(ProjectConfigurationRetrieveHistoryService.class);
        when(projectConfigurationRetrieveHistoryService.getItemType()).thenCallRealMethod();
        when(projectConfigurationRetrieveHistoryService.getEntityClass()).thenCallRealMethod();
    }

    @BeforeEach
    public void beforeEach() {
        historyServiceFactory.set(new HistoryServiceFactory(
                Collections.emptyList(),
                Arrays.asList(sectionRetrieveHistoryService, projectConfigurationRetrieveHistoryService)));
    }

    @Test
    public void getRetrieveHistoryServiceTest_SectionConfiguration_ReturnSectionRetrieveHistoryService() {
        Optional<RetrieveHistoryService<? extends DateAuditorEntity>> retrieveHistoryService =
                historyServiceFactory.get().getRetrieveHistoryService("section");

        Assertions.assertEquals(SectionConfiguration.class, retrieveHistoryService.get().getEntityClass());
    }

    @Test
    public void getRetrieveHistoryServiceTest_ProjectConfiguration_ProjectConfigurationRetrieveHistoryService() {
        Optional<RetrieveHistoryService<? extends DateAuditorEntity>> retrieveHistoryService =
                historyServiceFactory.get().getRetrieveHistoryService("projectconfiguration");

        Assertions.assertEquals(ProjectConfiguration.class,
                retrieveHistoryService.get().getEntityClass());
    }
}
