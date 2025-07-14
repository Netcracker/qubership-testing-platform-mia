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

package org.qubership.atp.mia.service.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.configuration.DuplicateDirectoryException;
import org.qubership.atp.mia.exceptions.configuration.FIleNotFoundException;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.service.BaseUnitTestConfiguration;

public class ProjectConfigurationServiceUnitTest extends BaseUnitTestConfiguration {

    @Test
    public void getCompoundById() {
        CompoundConfiguration compoundConfiguration =
                compoundConfigurationService.get().getCompoundById(testProjectConfiguration.get(),
                        testProjectConfiguration.get().getCompounds().get(0).getId());
        Assert.assertNotNull(compoundConfiguration);
        Assert.assertEquals(compoundConfiguration.getName(),
                testProjectConfiguration.get().getCompounds().get(0).getName());
    }

    @Test
    public void getCompoundById_WhenCompoundNotFound() {
        UUID compoundId = UUID.randomUUID();
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            compoundConfigurationService.get().getCompoundById(testProjectConfiguration.get(), compoundId);
        });
        Assert.assertEquals("MIA-0067: Compound with name/id '"+compoundId+"' not found!", thrown.getMessage());
    }

    @Test
    public void addDuplicateCompound() {
        String compoundName = "Compound Name";
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            compoundConfigurationService.get().addCompound(testProjectConfiguration.get(),
                    getCompoundDto(UUID.randomUUID(), UUID.randomUUID(), compoundName,
                            UUID.randomUUID(), "Process1", UUID.randomUUID(), "Process2"));
        });
        // Assert that the exception message is as expected
        Assert.assertEquals("MIA-0072: Compound with name "+compoundName+" already present!", thrown.getMessage());
    }

    @Test
    public void addNewCompound_ExpectErrorInRealUpdate() {
        String compoundName = "Compound Name New";
        compoundConfigurationService.get().addCompound(testProjectConfiguration.get(),
                getCompoundDto(UUID.randomUUID(),
                UUID.randomUUID(), compoundName,
                UUID.randomUUID(), "Process1", UUID.randomUUID(), "Process2"));
        verify(projectConfigurationService.get(), times(1))
                .synchronizeConfiguration(eq(testProjectConfiguration.get().getProjectId()), any(), eq(false));
    }

    // Process Test Cases
    @Test
    public void addDuplicateProcess() {
        //TO-DO: projectConfigurationService1 to have processes also similar to compounds. And then test this.
        String processName = "SSH_BG";
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            processConfigurationService.get().addProcess(testProjectConfiguration.get(),
                    getProcessDto(UUID.randomUUID(), UUID.randomUUID(), processName));
        });
        // Assert that the exception message is as expected
        Assert.assertEquals("MIA-0073: Process with name "+processName+" already present!", thrown.getMessage());
    }

    @Test
    public void addProcess_ExpectErrorInRealUpdate() {
        String processName = "Process1";
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            processConfigurationService.get().addProcess(testProjectConfiguration.get(),
                    getProcessDto(UUID.randomUUID(), UUID.randomUUID(), processName));
        });
        // Assert that the exception message is as expected
        Assert.assertTrue(thrown.getMessage().contains("MIA-0082: Problem occurred during process creation. Exception"
                + " java"
                + ".lang.NullPointerException"));
    }

    // Section Test Cases
    @Test
    public void addDuplicateSection() {
        String sectionName = "CM";
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            sectionConfigurationService.get().addSection(testProjectConfiguration.get(),
                    getSectionDto(UUID.randomUUID(), sectionName, 0, null));
        });
        Assert.assertEquals("MIA-0071: Section with name '"+sectionName+"' already present", thrown.getMessage());
    }

    @Test
    public void addNewSection() {
        String sectionName = "SECTION_NAME";
        sectionConfigurationService.get().addSection(testProjectConfiguration.get(),
                getSectionDto(UUID.randomUUID(), sectionName, 0, null));
        verify(projectConfigurationService.get(), times(1))
                .synchronizeConfiguration(eq(testProjectConfiguration.get().getProjectId()), any(), eq(false));
    }

    // Directory Test Cases
    @Test
    public void getDirectoryById() {
        ProjectDirectory projectDirectory =
                directoryConfigurationService.get().getDirectoryById(testProjectConfiguration.get(),
                        testProjectConfiguration.get().getDirectories().get(0).getId());
        Assert.assertNotNull(projectDirectory);
        Assert.assertEquals(projectDirectory.getName(), projectChildDirectoryName);
    }

    @Test
    public void getDirectoryByNotExistId_ExpectException() {
        UUID directoryId = UUID.randomUUID();
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            directoryConfigurationService.get().getDirectoryById(testProjectConfiguration.get(), directoryId);
        });
        Assert.assertEquals("MIA-0069: Directory with name/id '"+directoryId+"' not found!", thrown.getMessage());
    }

    @Test
    public void addDirectory() {
        directoryConfigurationService.get().addDirectory(testProjectConfiguration.get(),
                getDirectoryDto(null,
                        "Child2",
                        null));
    }

    @Test
    public void addDuplicateDirectory() {
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            directoryConfigurationService.get().addDirectory(testProjectConfiguration.get(),
                    getDirectoryDto(null,
                            projectChildDirectoryName,
                            null));
        });
        Assert.assertTrue(thrown instanceof DuplicateDirectoryException);
        Assert.assertEquals("MIA-0074: Directory with name "+projectChildDirectoryName+" already present!", thrown.getMessage());
    }

    // File Test Cases
    @Test
    public void getProjectFile_WhichDoesnotExist() {
        Path path = Paths.get("src\\main\\config\\project");
        miaConfigPath.set(mock(Path.class));
        when(miaConfigPath.get().resolve(anyString())).thenReturn(path);
        UUID fileId = UUID.randomUUID();
        when(projectConfigurationRepository.get().save(any())).thenReturn(testProjectConfiguration.get());
        MiaException thrown = Assert.assertThrows(MiaException.class, () -> {
            fileConfigurationService.get().getProjectFile(testProjectConfiguration.get().getProjectId(), fileId,
                    servletContext.get());
        });
        Assert.assertTrue(thrown instanceof FIleNotFoundException);
        Assert.assertEquals("MIA-0070: Project file with name/id '"+fileId+"' not found!", thrown.getMessage());
    }
}
