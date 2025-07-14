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

package org.qubership.atp.mia.ei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.compareProjectAndEiDirectories;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.compareProjectAndEiDirectoriesIdDiffers;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.compareProjectAndEiFiles;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.compareProjectAndEiFilesIdDiffers;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.readEntity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.mia.ei.component.ImportLoaderProcess;
import org.qubership.atp.mia.ei.executor.AtpMiaImportExecutor;
import org.qubership.atp.mia.ei.service.AtpImportStrategy;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.configuration.CommandPrefix;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.ei.ExportImportCommonConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportPotHeaderConfiguration;

public class ImportTest extends ExportImportBaseTest {

    @Test
    public void import_notAtp() {
        MiaException thrown = assertThrows(
                MiaException.class,
                () -> {
                    Map<String, Set<String>> scopes = new HashMap<String, Set<String>>() {{
                        put(ExportImportEntities.MIA_SECTION.name(), new HashSet<>());
                    }};
                    exportScope.set(new ExportScope(scopes));
                    exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP));
                    AtpMiaImportExecutor atpMiaImportExecutor = new AtpMiaImportExecutor(new AtpImportStrategy(
                            Collections.singletonList(new ImportLoaderProcess(new ObjectLoaderFromDiskService())),
                            projectConfigurationService.get(),
                            Mockito.mock(NotificationService.class)
                    ));
                    atpMiaImportExecutor.importData(exportImportData.get(), path.get());
                },
                "Expected exportToFolder to throw, but it didn't"
        );
        assertEquals("MIA-7001: MIA does not support import type '" + ExportImportEntities.MIA_SECTION.name() + "'", thrown.getMessage());
    }

    @Test
    public void import_TheSameProject() {
        Map<String, Set<String>> scopes = new HashMap<String, Set<String>>() {{
            put(ExportImportEntities.MIA_SECTION.name(), new HashSet() {{
                add(new UUID(1, 1));
                add(new UUID(1, 2));
                add(new UUID(1, 4));
                add(new UUID(1, 5));
                add(new UUID(1, 6));
            }});
            put(ExportImportEntities.MIA_DIRECTORY.name(), new HashSet() {{
                add(directoryConfigurations.get().get("/rootDirectory0"));
                add(directoryConfigurations.get().get("/rootDirectory1"));
                add(directoryConfigurations.get().get("/rootDirectory0/Directory1"));
            }});
            put(ExportImportEntities.MIA_FILES.name(), new HashSet() {{
                add(fileConfigurations.get().get("rootFile0"));
                add(fileConfigurations.get().get("rootDirectory0_File1.txt"));
                add(fileConfigurations.get().get("rootDirectory0_File6.txt"));
                add(fileConfigurations.get().get("rootDirectory0_Directory1_File2.txt"));
                add(fileConfigurations.get().get("rootDirectory0_Directory1_File3.txt"));
            }});
            put(ExportImportEntities.MIA_COMMON_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
            put(ExportImportEntities.MIA_HEADER_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
            put(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
        }};
        exportScope.set(new ExportScope(scopes));
        Path importPath = Paths.get("src/test/resources/ei/import/TheSameProject/");
        ExportImportData data = new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP);
        exportImportData.set(data);
        ArgumentCaptor<ProjectConfiguration> captor = ArgumentCaptor.forClass(ProjectConfiguration.class);
        atpMiaImportExecutor.importData(exportImportData.get(), importPath);
        verify(projectConfigurationService.get(), times(1)).updateProjectWithReplicationOff(captor.capture(), eq(true));
        ProjectConfiguration config = captor.getValue();

        //Check SECTIONS
        assertEquals(6, config.getSections().size());
        //Check section1
        SectionConfiguration sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section1")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(1, sectionToCheck.getCompounds().size());
        assertEquals(new UUID(2, 1), sectionToCheck.getCompounds().get(0).getId());
        assertEquals(2, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 1))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 2))));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section2
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(2, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 3))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 5))));
        assertEquals(2, sectionToCheck.getSections().size());

        //Check section3
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section3")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(1, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 7))));
        assertEquals(1, sectionToCheck.getSections().size());
        assertTrue(sectionToCheck.getSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 6))));

        //Check section4
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2.1")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 2), sectionToCheck.getParentSection().getId());
        assertEquals(2, sectionToCheck.getCompounds().size());
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 1))));
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 2))));
        assertEquals(4, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 1))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 4))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 6))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 7))));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section5
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2.2")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 2), sectionToCheck.getParentSection().getId());
        assertEquals(1, sectionToCheck.getCompounds().size());
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 3))));
        assertEquals(1, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 7))));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section6
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section3.1")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 3), sectionToCheck.getParentSection().getId());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(0, sectionToCheck.getProcesses().size());
        assertEquals(0, sectionToCheck.getSections().size());

        //Check PROCESSES
        assertEquals(7, config.getProcesses().size());
        //Check process1
        ProcessConfiguration processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 1))).findAny().get();
        assertEquals("SSH_BG_rename", processToCheck.getName());
        assertEquals(3, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 1))));
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 2))));
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 3))));
        assertEquals(2, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 1))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));

        //Check process2
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 2))).findAny().get();
        assertEquals("SSH_BG", processToCheck.getName());
        assertEquals(1, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 1))));
        assertEquals(1, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 1))));

        //Check process3
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 3))).findAny().get();
        assertEquals("SSH_MARKER", processToCheck.getName());
        assertEquals(2, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 2))));
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 3))));
        assertEquals(1, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 2))));

        //Check process7
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 7))).findAny().get();
        assertTrue(processToCheck.getName().startsWith("SSH_BG_ei_"));
        assertEquals(1, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 3))));
        assertEquals(3, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 3))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 5))));

        //Check COMPOUNDS
        assertEquals(3, config.getCompounds().size());
        //Check compound 1
        CompoundConfiguration compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getId().equals(new UUID(2, 1))).findAny().get();
        assertEquals("Compound1_rename", compoundToCheck.getName());
        assertEquals("blabla", compoundToCheck.getReferToInput());
        assertEquals(2, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 1))));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getId());
        assertEquals(new UUID(3, 2), compoundToCheck.getProcesses().get(1).getId());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(2).getId());

        //Check compound 2
        compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getId().equals(new UUID(2, 2))).findAny().get();
        assertEquals("Compound2", compoundToCheck.getName());
        assertEquals(1, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));
        assertEquals(2, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getId());
        assertEquals(new UUID(3, 3), compoundToCheck.getProcesses().get(1).getId());

        //Check compound 3
        compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getId().equals(new UUID(2, 3))).findAny().get();
        assertTrue(compoundToCheck.getName().startsWith("Compound2_ei_"));
        assertEquals(1, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 5))));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getId());
        assertEquals(new UUID(3, 3), compoundToCheck.getProcesses().get(1).getId());
        assertEquals(new UUID(3, 7), compoundToCheck.getProcesses().get(2).getId());

        //Validate imported sizes

        //Check directories
        //Check /rootDirectory0
        UUID directoryId = directoryConfigurations.get().get("/rootDirectory0").getId();
        compareProjectAndEiDirectories(directoryId, importPath, testProjectConfiguration.get().getDirectories());

        //Check /rootDirectory1
        directoryId = directoryConfigurations.get().get("/rootDirectory1").getId();
        compareProjectAndEiDirectories(directoryId, importPath, testProjectConfiguration.get().getDirectories());

        //Check /rootDirectory0/Directory1
        directoryId = directoryConfigurations.get().get("/rootDirectory0/Directory1").getId();
        compareProjectAndEiDirectories(directoryId, importPath, testProjectConfiguration.get().getDirectories());

        //Check files
        //Check rootFile0
        UUID fileId = fileConfigurations.get().get("rootFile0").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_File1.txt
        fileId = fileConfigurations.get().get("rootDirectory0_File1.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_File6.txt
        fileId = fileConfigurations.get().get("rootDirectory0_File6.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_Directory1_File2.txt
        fileId = fileConfigurations.get().get("rootDirectory0_Directory1_File2.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_Directory1_File3.txt
        fileId = fileConfigurations.get().get("rootDirectory0_Directory1_File3.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check configurations
        //Check common configuration
        ExportImportCommonConfiguration expectedCommonConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportCommonConfiguration.class);
        expectedCommonConfiguration.setId(projectId.get());
        assertEquals(expectedCommonConfiguration, new ExportImportCommonConfiguration(testProjectConfiguration.get().getCommonConfiguration()));

        //Check header configuration
        ExportImportHeaderConfiguration expectedHeaderConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportHeaderConfiguration.class);
        expectedHeaderConfiguration.setId(projectId.get());
        assertEquals(expectedHeaderConfiguration, new ExportImportHeaderConfiguration(testProjectConfiguration.get().getHeaderConfiguration()));

        //Check POT header configuration
        ExportImportPotHeaderConfiguration expectedPotHeaderConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportPotHeaderConfiguration.class);
        expectedPotHeaderConfiguration.setId(projectId.get());
        assertEquals(expectedPotHeaderConfiguration, new ExportImportPotHeaderConfiguration(testProjectConfiguration.get().getPotHeaderConfiguration()));
    }

    @Test
    public void import_AnotherProject() {
        Map<String, Set<String>> scopes = new HashMap<String, Set<String>>() {{
            put(ExportImportEntities.MIA_SECTION.name(), new HashSet() {{
                add(new UUID(1, 1));
                add(new UUID(1, 2));
                add(new UUID(1, 4));
                add(new UUID(1, 5));
                add(new UUID(1, 6));
            }});
            put(ExportImportEntities.MIA_DIRECTORY.name(), new HashSet() {{
                add(directoryConfigurations.get().get("/rootDirectory0"));
                add(directoryConfigurations.get().get("/rootDirectory1"));
                add(directoryConfigurations.get().get("/rootDirectory0/Directory1"));
            }});
            put(ExportImportEntities.MIA_FILES.name(), new HashSet() {{
                add(fileConfigurations.get().get("rootFile0"));
                add(fileConfigurations.get().get("rootDirectory0_File1.txt"));
                add(fileConfigurations.get().get("rootDirectory0_File6.txt"));
                add(fileConfigurations.get().get("rootDirectory0_Directory1_File2.txt"));
                add(fileConfigurations.get().get("rootDirectory0_Directory1_File3.txt"));
            }});
            put(ExportImportEntities.MIA_COMMON_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
            put(ExportImportEntities.MIA_HEADER_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
            put(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
        }};
        exportScope.set(new ExportScope(scopes));
        Path importPath = Paths.get("src/test/resources/ei/import/AnotherProject/");
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), new HashMap<>(), null, null, false));
        ValidationResult validationResult = atpMiaImportExecutor.validateData(exportImportData.get(), importPath);
        assertEquals(23, validationResult.getReplacementMap().size());
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), validationResult.getReplacementMap(), null, null, false));
        ArgumentCaptor<ProjectConfiguration> captor = ArgumentCaptor.forClass(ProjectConfiguration.class);
        atpMiaImportExecutor.importData(exportImportData.get(), importPath);
        verify(projectConfigurationService.get(), times(1)).updateProjectWithReplicationOff(captor.capture(), eq(true));
        ProjectConfiguration config = captor.getValue();

        //Check SECTIONS
        assertEquals(6, config.getSections().size());
        //Check section1
        SectionConfiguration sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section1")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(1, sectionToCheck.getCompounds().size());
        assertEquals(new UUID(2, 1), sectionToCheck.getCompounds().get(0).getId());
        assertEquals(2, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 1))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 2))));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section2
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(2, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 3))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 5))));
        assertEquals(2, sectionToCheck.getSections().size());

        //Check section3
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section3")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(1, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getName().startsWith("SSH_BG_ei_")));
        assertEquals(1, sectionToCheck.getSections().size());
        assertTrue(sectionToCheck.getSections().stream().anyMatch(p -> p.getName().equals("Section3.1")));

        //Check section4
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2.1")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 2), sectionToCheck.getParentSection().getId());
        assertEquals(2, sectionToCheck.getCompounds().size());
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 1))));
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 2))));
        assertEquals(4, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 1))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 4))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getId().equals(new UUID(3, 6))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getName().startsWith("SSH_BG_ei_")));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section5
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2.2")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 2), sectionToCheck.getParentSection().getSourceId());
        assertEquals(1, sectionToCheck.getCompounds().size());
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getName().startsWith("Compound2_ei_")));
        assertEquals(1, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getName().startsWith("SSH_BG_ei_")));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section6
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section3.1")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 3), sectionToCheck.getParentSection().getId());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(0, sectionToCheck.getProcesses().size());
        assertEquals(0, sectionToCheck.getSections().size());

        //Check PROCESSES
        assertEquals(7, config.getProcesses().size());
        //Check process1
        ProcessConfiguration processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 1))).findAny().get();
        assertEquals("SSH_BG_rename", processToCheck.getName());
        assertEquals(3, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 1))));
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 2))));
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getName().startsWith("Compound2_ei_")));
        assertEquals(2, processToCheck.getInSections().size());

        //Check process2
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 2))).findAny().get();
        assertEquals("SSH_BG", processToCheck.getName());
        assertEquals(1, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 1))));
        assertEquals(1, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 1))));

        //Check process3
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getId().equals(new UUID(3, 3))).findAny().get();
        assertEquals("SSH_MARKER", processToCheck.getName());
        assertEquals(2, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getId().equals(new UUID(2, 2))));
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getName().startsWith("Compound2_ei_")));
        assertEquals(1, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 2))));

        //Check process7
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getName().startsWith("SSH_BG_ei_")).findAny().get();
        assertEquals(1, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getName().startsWith("Compound2_ei_")));
        assertEquals(3, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 3))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> new UUID(1, 5).equals(p.getSourceId())));

        //Check COMPOUNDS
        assertEquals(3, config.getCompounds().size());
        //Check compound 1
        CompoundConfiguration compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getId().equals(new UUID(2, 1))).findAny().get();
        assertEquals("Compound1_rename", compoundToCheck.getName());
        assertEquals("blabla", compoundToCheck.getReferToInput());
        assertEquals(2, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 1))));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getId());
        assertEquals(new UUID(3, 2), compoundToCheck.getProcesses().get(1).getId());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(2).getId());

        //Check compound 2
        compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getId().equals(new UUID(2, 2))).findAny().get();
        assertEquals("Compound2", compoundToCheck.getName());
        assertEquals(1, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getId().equals(new UUID(1, 4))));
        assertEquals(2, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getId());
        assertEquals(new UUID(3, 3), compoundToCheck.getProcesses().get(1).getId());

        //Check compound 3
        compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getName().startsWith("Compound2_ei_")).findAny().get();
        assertEquals(1, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> new UUID(1, 5).equals(p.getSourceId())));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getId());
        assertEquals(new UUID(3, 3), compoundToCheck.getProcesses().get(1).getId());
        assertEquals(new UUID(3, 7), compoundToCheck.getProcesses().get(2).getSourceId());

        //Check directories
        //Check /rootDirectory0
        UUID directoryId = directoryConfigurations.get().get("/rootDirectory0").getId();
        compareProjectAndEiDirectories(directoryId, importPath, testProjectConfiguration.get().getDirectories());

        //Check /rootDirectory1
        directoryId = directoryConfigurations.get().get("/rootDirectory1").getId();
        compareProjectAndEiDirectories(directoryId, importPath, testProjectConfiguration.get().getDirectories());

        //Check /rootDirectory0/Directory1
        directoryId = directoryConfigurations.get().get("/rootDirectory0/Directory1").getId();
        compareProjectAndEiDirectories(directoryId, importPath, testProjectConfiguration.get().getDirectories());

        //Check files
        //Check rootFile0
        UUID fileId = fileConfigurations.get().get("rootFile0").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_File1.txt
        fileId = fileConfigurations.get().get("rootDirectory0_File1.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_File6.txt
        fileId = fileConfigurations.get().get("rootDirectory0_File6.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_Directory1_File2.txt
        fileId = fileConfigurations.get().get("rootDirectory0_Directory1_File2.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_Directory1_File3.txt
        fileId = fileConfigurations.get().get("rootDirectory0_Directory1_File3.txt").getId();
        compareProjectAndEiFiles(fileId, importPath, testProjectConfiguration.get().getFiles());

        //Check configurations
        //Check common configuration
        ExportImportCommonConfiguration expectedCommonConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportCommonConfiguration.class);
        expectedCommonConfiguration.setId(projectId.get());
        assertEquals(expectedCommonConfiguration, new ExportImportCommonConfiguration(testProjectConfiguration.get().getCommonConfiguration()));

        //Check header configuration
        ExportImportHeaderConfiguration expectedHeaderConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportHeaderConfiguration.class);
        expectedHeaderConfiguration.setId(projectId.get());
        assertEquals(expectedHeaderConfiguration, new ExportImportHeaderConfiguration(testProjectConfiguration.get().getHeaderConfiguration()));

        //Check POT header configuration
        ExportImportPotHeaderConfiguration expectedPotHeaderConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportPotHeaderConfiguration.class);
        expectedPotHeaderConfiguration.setId(projectId.get());
        assertEquals(expectedPotHeaderConfiguration, new ExportImportPotHeaderConfiguration(testProjectConfiguration.get().getPotHeaderConfiguration()));
    }

    @Test
    public void import_NewProject_Full() {
        testProjectConfiguration.set(ProjectConfiguration.builder()
                .projectId(projectId.get())
                .projectName(Constants.DEFAULT_PROJECT_NAME)
                .gitUrl("")
                .lastLoadedWhen(LocalDateTime.now().minusYears(1))
                .headerConfiguration(HeaderConfiguration.builder()
                        .switchers(Arrays.asList(
                                Switcher.builder()
                                        .value(false)
                                        .actionType("SQL")
                                        .name(SwitcherSQL1)
                                        .display("Sysdatevalue Uncomment/comment")
                                        .actionTrue("update gparams set name='SYSdateValue' where name='#SYSdateValue'")
                                        .build(),
                                Switcher.builder()
                                        .value(true)
                                        .actionType("SQL")
                                        .name(SwitcherSQL2)
                                        .display("SysdateOverride comment")
                                        .actionFalse("update gparams set name='SYSdateoverride' where name='#SYSdateOverride'")
                                        .build()
                        ))
                        .build())
                .commonConfiguration(CommonConfiguration.builder()
                        .commandShellPrefixes(Arrays.asList(CommandPrefix.builder()
                                .system(testSystem.get().getName())
                                .prefixes(new LinkedHashMap<String, String>() {{
                                    put("accountNumber", "echo \"Something with accountNumber %s\"");
                                    put("genevaDate", "echo \"Something with genevaDate %s\"");
                                    put("infinys_root", "cd %s\nsource infinys.env");
                                    put("workingDirectory", "mkdir -p %s");
                                    put("exportGenevaDate", "export GENEVA_FIXEDDATE=\":genevaDate\"");
                                    put("fullTraceMode", "export TRACE_LEVEL=FULL\nexport TRACE_ALL=ON");
                                }})
                                .build()))
                        .build())
                .build());
        when(projectConfigurationService.get().getConfigByProjectId(eq(projectId.get())))
                .thenReturn(testProjectConfiguration.get());
        when(projectConfigurationService.get().getConfiguration(eq(projectId.get())))
                .thenReturn(testProjectConfiguration.get());
        Map<String, Set<String>> scopes = new HashMap<String, Set<String>>() {{
            put(ExportImportEntities.MIA_SECTION.name(), new HashSet() {{
                add(new UUID(1, 1));
                add(new UUID(1, 2));
                add(new UUID(1, 3));
                add(new UUID(1, 4));
                add(new UUID(1, 5));
                add(new UUID(1, 6));
            }});
            put(ExportImportEntities.MIA_DIRECTORY.name(), new HashSet() {{
                add(directoryConfigurations.get().get("/rootDirectory0"));
                add(directoryConfigurations.get().get("/rootDirectory1"));
                add(directoryConfigurations.get().get("/rootDirectory0/Directory1"));
            }});
            put(ExportImportEntities.MIA_FILES.name(), new HashSet() {{
                add(fileConfigurations.get().get("rootFile0"));
                add(fileConfigurations.get().get("rootDirectory0_File1.txt"));
                add(fileConfigurations.get().get("rootDirectory0_File6.txt"));
                add(fileConfigurations.get().get("rootDirectory0_Directory1_File2.txt"));
                add(fileConfigurations.get().get("rootDirectory0_Directory1_File3.txt"));
            }});
            put(ExportImportEntities.MIA_COMMON_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
            put(ExportImportEntities.MIA_HEADER_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
            put(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION.name(), new HashSet() {{
                add(projectId.get());
            }});
        }};
        exportScope.set(new ExportScope(scopes));
        Path importPath = Paths.get("src/test/resources/ei/import/NewProject/");
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), new HashMap<>(), null, null, false));
        ValidationResult validationResult = atpMiaImportExecutor.validateData(exportImportData.get(), importPath);
        assertEquals(24, validationResult.getReplacementMap().size());
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), validationResult.getReplacementMap(), null, null, true));
        ArgumentCaptor<ProjectConfiguration> captor = ArgumentCaptor.forClass(ProjectConfiguration.class);
        atpMiaImportExecutor.importData(exportImportData.get(), importPath);
        verify(projectConfigurationService.get(), times(1)).updateProjectWithReplicationOff(captor.capture(), eq(true));
        ProjectConfiguration config = captor.getValue();
        //Check SECTIONS
        assertEquals(6, config.getSections().size());
        //Check section1
        SectionConfiguration sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section1")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(1, sectionToCheck.getCompounds().size());
        assertEquals(new UUID(2, 1), sectionToCheck.getCompounds().get(0).getSourceId());
        assertEquals(2, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 1))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 2))));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check section2
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(2, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 3))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 5))));
        assertEquals(2, sectionToCheck.getSections().size());

        //Check section3
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section3")).findAny().get();
        assertNull(sectionToCheck.getParentSection());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(1, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 7))));
        assertEquals(1, sectionToCheck.getSections().size());
        assertTrue(sectionToCheck.getSections().stream().anyMatch(p -> p.getName().equals("Section3.1")));

        //Check section4
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2.1")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 2), sectionToCheck.getParentSection().getSourceId());
        assertEquals(2, sectionToCheck.getCompounds().size());
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getSourceId().equals(new UUID(2, 1))));
        assertTrue(sectionToCheck.getCompounds().stream().anyMatch(p -> p.getSourceId().equals(new UUID(2, 2))));
        assertEquals(4, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 1))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 4))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 6))));
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 7))));
        assertEquals(0, sectionToCheck.getSections().size());

        CompoundConfiguration compoundToRenameFrom2_1 = sectionToCheck.getCompounds().stream().filter(p -> p.getName().startsWith("Compound2")).findFirst().get();

        //Check section5
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section2.2")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 2), sectionToCheck.getParentSection().getSourceId());
        assertEquals(1, sectionToCheck.getCompounds().size());
        assertEquals(1, sectionToCheck.getProcesses().size());
        assertTrue(sectionToCheck.getProcesses().stream().anyMatch(p -> p.getSourceId().equals(new UUID(3, 7))));
        assertEquals(0, sectionToCheck.getSections().size());

        //Check compound in section 2.1 or 2.2 was renamed
        // in import we have two compounds with name Compound2, so one of them should be renamed, but which one...
        CompoundConfiguration compoundToRenameFrom2_2 = sectionToCheck.getCompounds().stream().filter(p -> p.getName().startsWith("Compound2")).findFirst().get();
        assertTrue(compoundToRenameFrom2_1.getName().equals("Compound2") && compoundToRenameFrom2_2.getName().contains("Compound2_ei_")
                || compoundToRenameFrom2_2.getName().equals("Compound2") && compoundToRenameFrom2_1.getName().contains("Compound2_ei_"));

        //Check section6
        sectionToCheck = config.getSections().stream().filter(s -> s.getName().equals("Section3.1")).findAny().get();
        assertNotNull(sectionToCheck.getParentSection());
        assertEquals(new UUID(1, 3), sectionToCheck.getParentSection().getSourceId());
        assertEquals(0, sectionToCheck.getCompounds().size());
        assertEquals(0, sectionToCheck.getProcesses().size());
        assertEquals(0, sectionToCheck.getSections().size());

        //Check PROCESSES
        assertEquals(7, config.getProcesses().size());
        //Check process1
        ProcessConfiguration processToCheck = config.getProcesses().stream()
                .filter(s -> s.getSourceId().equals(new UUID(3, 1))).findAny().get();
        assertEquals("SSH_BG_rename", processToCheck.getName());
        assertEquals(4, processToCheck.getInCompounds().size());
        assertEquals(2, processToCheck.getInCompounds().stream().filter(p -> p.getSourceId().equals(new UUID(2, 1))).count());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getSourceId().equals(new UUID(2, 2))));
        assertEquals(2, processToCheck.getInSections().size());

        //Check process2
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getSourceId().equals(new UUID(3, 2))).findAny().get();
        assertEquals(1, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getSourceId().equals(new UUID(2, 1))));
        assertEquals(1, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 1))));

        ProcessConfiguration processSshBgToRename1 = processToCheck;

        //Check process7
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getSourceId().equals(new UUID(3, 7))).findAny().get();
        assertEquals(2, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getName().equals("Compound2")));
        assertEquals(3, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 3))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 4))));
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> new UUID(1, 5).equals(p.getSourceId())));

        //Check compound in section 2.1 or 2.2 was renamed
        // in import we have two compounds with name Compound2, so one of them should be renamed, but which one...
        ProcessConfiguration processSshBgToRename2 = processToCheck;
        assertTrue(processSshBgToRename1.getName().equals("SSH_BG") && processSshBgToRename2.getName().contains("SSH_BG_ei_")
                || processSshBgToRename2.getName().equals("SSH_BG") && processSshBgToRename1.getName().contains("SSH_BG_ei_"));

        //Check process3
        processToCheck = config.getProcesses().stream()
                .filter(s -> s.getSourceId().equals(new UUID(3, 3))).findAny().get();
        assertEquals("SSH_MARKER", processToCheck.getName());
        assertEquals(2, processToCheck.getInCompounds().size());
        assertTrue(processToCheck.getInCompounds().stream().anyMatch(p -> p.getSourceId().equals(new UUID(2, 2))));
        assertEquals(1, processToCheck.getInSections().size());
        assertTrue(processToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 2))));

        //Check COMPOUNDS
        assertEquals(3, config.getCompounds().size());
        //Check compound 1
        CompoundConfiguration compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getSourceId().equals(new UUID(2, 1))).findAny().get();
        assertEquals("Compound1_rename", compoundToCheck.getName());
        assertEquals("blabla", compoundToCheck.getReferToInput());
        assertEquals(2, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 4))));
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 1))));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getSourceId());
        assertEquals(new UUID(3, 2), compoundToCheck.getProcesses().get(1).getSourceId());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(2).getSourceId());

        //Check compound 2
        compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getSourceId().equals(new UUID(2, 2))).findAny().get();
        assertEquals(1, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> p.getSourceId().equals(new UUID(1, 4))));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getSourceId());
        assertEquals(new UUID(3, 3), compoundToCheck.getProcesses().get(1).getSourceId());
        assertEquals(new UUID(3, 7), compoundToCheck.getProcesses().get(2).getSourceId());

        //Check compound 3
        compoundToCheck = config.getCompounds().stream()
                .filter(s -> s.getSourceId().equals(new UUID(2, 3))).findAny().get();
        assertEquals(1, compoundToCheck.getInSections().size());
        assertTrue(compoundToCheck.getInSections().stream().anyMatch(p -> new UUID(1, 5).equals(p.getSourceId())));
        assertEquals(3, compoundToCheck.getProcesses().size());
        assertEquals(new UUID(3, 1), compoundToCheck.getProcesses().get(0).getSourceId());
        assertEquals(new UUID(3, 3), compoundToCheck.getProcesses().get(1).getSourceId());
        assertEquals(new UUID(3, 7), compoundToCheck.getProcesses().get(2).getSourceId());

        //Check directories
        //Check /rootDirectory0
        compareProjectAndEiDirectoriesIdDiffers("/rootDirectory0", importPath, testProjectConfiguration.get().getDirectories());

        //Check /rootDirectory1
        compareProjectAndEiDirectoriesIdDiffers("/rootDirectory1", importPath, testProjectConfiguration.get().getDirectories());

        //Check /rootDirectory0/Directory1
        compareProjectAndEiDirectoriesIdDiffers("/rootDirectory0/Directory1", importPath, testProjectConfiguration.get().getDirectories());

        //Check files
        //Check rootFile0
        compareProjectAndEiFilesIdDiffers("rootFile0", importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_File1.txt
        compareProjectAndEiFilesIdDiffers("rootDirectory0_File1.txt", importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_File6.txt
        compareProjectAndEiFilesIdDiffers("rootDirectory0_File6.txt", importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_Directory1_File2.txt
        compareProjectAndEiFilesIdDiffers("rootDirectory0_Directory1_File2.txt", importPath, testProjectConfiguration.get().getFiles());

        //Check rootDirectory0_Directory1_File3.txt
        compareProjectAndEiFilesIdDiffers("rootDirectory0_Directory1_File3.txt", importPath, testProjectConfiguration.get().getFiles());

        //Check configurations
        //Check common configuration
        ExportImportCommonConfiguration expectedCommonConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportCommonConfiguration.class);
        expectedCommonConfiguration.setId(projectId.get());
        assertEquals(expectedCommonConfiguration, new ExportImportCommonConfiguration(testProjectConfiguration.get().getCommonConfiguration()));

        //Check header configuration
        ExportImportHeaderConfiguration expectedHeaderConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportHeaderConfiguration.class);
        expectedHeaderConfiguration.setId(projectId.get());
        assertEquals(expectedHeaderConfiguration, new ExportImportHeaderConfiguration(testProjectConfiguration.get().getHeaderConfiguration()));

        //Check POT header configuration
        ExportImportPotHeaderConfiguration expectedPotHeaderConfiguration =
                readEntity(importPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportPotHeaderConfiguration.class);
        expectedPotHeaderConfiguration.setId(projectId.get());
        assertEquals(expectedPotHeaderConfiguration, new ExportImportPotHeaderConfiguration(testProjectConfiguration.get().getPotHeaderConfiguration()));
    }
}
