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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.model.Constants.DEFAULT_PROJECT_NAME;
import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.controllers.api.dto.CompoundDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessSettingsDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectDirectoryDto;
import org.qubership.atp.mia.controllers.api.dto.SectionDto;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Input;
import org.qubership.atp.mia.model.impl.executable.Prerequisite;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.TableMarker;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.repo.configuration.ProjectConfigurationRepository;
import org.qubership.atp.mia.repo.db.RecordingSessionRepository;
import org.qubership.atp.mia.repo.impl.ShellRepository;
import org.qubership.atp.mia.repo.impl.TestDataRepository;
import org.qubership.atp.mia.service.cache.MiaCacheService;
import org.qubership.atp.mia.service.configuration.ConfigurationFileDeserializer;
import org.qubership.atp.mia.service.configuration.ConfigurationFileSerializer;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.qubership.atp.mia.service.execution.TestDataService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.service.git.GitService;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.common.collect.ImmutableList;

@ExtendWith(SkipTestInJenkins.class)
@SpringBootTest(classes = {ModelMapper.class})
public class BaseUnitTestConfiguration extends ConfigTestBean {

    public static final ThreadLocal<ConfigurationFileDeserializer> configurationFileDeserializer = new ThreadLocal<>();
    public static final ThreadLocal<ConfigurationFileSerializer> configurationFileSerializer = new ThreadLocal<>();
    public static final ThreadLocal<GitService> gitService = new ThreadLocal<>();
    public static final ThreadLocal<Path> miaConfigPath = new ThreadLocal<>();
    public static final ThreadLocal<ServletContext> servletContext =  new ThreadLocal<>();
    public static final ThreadLocal<MiaCacheService> miaCacheService =  new ThreadLocal<>();
    public static final ThreadLocal<TestDataService> testDataService = new ThreadLocal<>();
    public static final ThreadLocal<ShellRepository> sshRepo = new ThreadLocal<>();
    public static final ThreadLocal<TestDataRepository> testDataRepository = new ThreadLocal();
    public static final ThreadLocal<SqlExecutionHelperService> sqlService = new ThreadLocal<>();
    public String projectChildDirectoryName = "Child1";

    @BeforeEach
    public void BaseUnitTestConfiguration_initialize() {
        configurationFileDeserializer.set(mock(ConfigurationFileDeserializer.class));
        configurationFileSerializer.set(mock(ConfigurationFileSerializer.class));
        gitService.set(mock(GitService.class));
        miaConfigPath.set(mock(Path.class));
        projectConfigurationRepository.set(mock(ProjectConfigurationRepository.class));
        recordingSessionRepository.set(mock(RecordingSessionRepository.class));
        servletContext.set(mock(ServletContext.class));
        gridFsService.set(mock(GridFsService.class));
        miaCacheService.set(mock(MiaCacheService.class));
        testDataRepository.set(mock(TestDataRepository.class));
        miaFileService.set(spy(new MiaFileService(gridFsService.get(), miaContext.get(), projectConfigurationService.get())));
        testDataService.set(spy(new TestDataService(sshRepo.get(), testDataRepository.get(), miaContext.get(),
                sqlService.get(), miaFileService.get())));
        BaseUnitTestConfiguration_prepareEnvironment();
        BaseUnitTestConfiguration_prepareProjectConfiguration();
    }

    public void BaseUnitTestConfiguration_prepareEnvironment() {
        int randomId = (int) (Math.random() * 1000);
        UUID envId = UUID.randomUUID();
        UUID systemId = UUID.randomUUID();
        System testSystem2 = System.builder()
                .id(systemId)
                .name(TEST_SYSTEM_NAME + randomId)
                .environmentId(envId)
                .connections(Arrays.asList())
                .build();
        Environment testEnvironment2 = Environment.builder()
                .projectId(projectId.get())
                .id(envId)
                .name(TEST_ENVIRONMENT_NAME + randomId)
                .systems(Arrays.asList(testSystem2))
                .build();
        Project testProject2 = Project.builder()
                .id(projectId.get())
                .name(DEFAULT_PROJECT_NAME + randomId)
                .environments(Arrays.asList(testEnvironment2.getId()))
                .build();
        when(environmentsService.get().getEnvironmentsByProject(eq(projectId.get())))
                .thenReturn(Arrays.asList(testEnvironment2));
        when(environmentsService.get().getEnvironmentsFull(eq(envId), eq(projectId.get()))).thenReturn(testEnvironment2);
        when(environmentsService.get().getProjects()).thenReturn(Arrays.asList(testProject2));
        when(environmentsService.get().getProject(eq(projectId.get()))).thenReturn(testProject2);
        miaContext.get().setContext(projectId.get(), null);
    }

    public void BaseUnitTestConfiguration_prepareProjectConfiguration() {

        UUID projectChildDirectoryId = UUID.randomUUID();

        //  Section Configuration
        List<SectionConfiguration> sectionConfigurations = new ArrayList<>();
        SectionConfiguration sectionConfiguration = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("CM")
                .place(0)
                .parentSection(null)
                .sections(new ArrayList<>())
                .compounds(new ArrayList<>())
                .processes(new ArrayList<>())
                .projectConfiguration(testProjectConfiguration.get())
                .build();
        sectionConfigurations.add(sectionConfiguration);

        // Compound Configuration
        List<CompoundConfiguration> compounds = new ArrayList<>();
        CompoundConfiguration compound = CompoundConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Compound Name")
                .projectConfiguration(testProjectConfiguration.get())
                .inSections(sectionConfigurations)
                .processes(Arrays.asList(
                        ProcessConfiguration.builder()
                                .id(UUID.randomUUID())
                                .name("SQL_GPARAMS")
                                .processSettings(ProcessSettings.builder()
                                        .command(Command.builder()
                                                .name("GPARAMS")
                                                .type("SQL")
                                                .values(new LinkedHashSet<String>() {{add("some command");}})
                                                .build())
                                        .build())
                                .projectConfiguration(testProjectConfiguration.get())
                                .inSections(sectionConfigurations)
                                .inCompounds(compounds)
                                .build(),
                        ProcessConfiguration.builder()
                                .id(UUID.randomUUID())
                                .name("SSH_BG")
                                .processSettings(ProcessSettings.builder()
                                        .command(Command.builder()
                                                .name("BG")
                                                .type("SSH")
                                                .values(new LinkedHashSet<String>() {{add("some command");}})
                                                .build())
                                        .build())
                                .projectConfiguration(testProjectConfiguration.get())
                                .inSections(sectionConfigurations)
                                .inCompounds(compounds)
                                .build()
                )).build();
        compounds.add(compound);

        // Process configuration
        // Setting Inputs, Prereqisites, validations, etc
        String systemBillingSystem = "Billing System";
        final Input inputBG =
                new Input("bill_period", "list", listToSet("1", "2")).setLabel("Bill Period").setRequired(true);
        final Prerequisite prerequisitesBG = new Prerequisite("SSH", systemBillingSystem, "echo 1;");
        final List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(prerequisitesBG);
        final Command commandBG = new Command("BG", "SSH", systemBillingSystem, listToSet("BG -a \"-a :accountNumber\""));
        commandBG.setLogFileNameFormat("Custom_log_name.log");
        TableMarker marker1 = new TableMarker();
        marker1.setTableRowCount(">0");
        final Validation validation1 = new Validation("SQL", systemBillingSystem, "select * from ACCOUNTDETAILS where "
                + "account_num = :accountNumber")
                .setTableName("AccountDetails")
                .setReferToCommandExecution(ImmutableList.of("BG -a \"-a :accountNumber\""))
                .setTableMarker(marker1);
        TableMarker marker2 = new TableMarker();
        HashMap<String, String> expectedResults = new HashMap<>();
        expectedResults.put("account_num", ":accountNumber");
        marker2.setExpectedResultForQuery(expectedResults);
        final Validation validation2 = new Validation("SQL", systemBillingSystem, "select bill_period, payment_method_id "
                + "from ACCOUNTDETAILS where account_num = :accountNumber")
                .setTableName("AccountDetails")
                .setReferToCommandExecution(ImmutableList.of("BG -a \"-a :accountNumber\""))
                .setTableMarker(marker2);

        // Prepare Process with above Inputs, Pre-requisites, Validations, etc
        List<ProcessConfiguration> processes = new ArrayList<>();
        ProcessConfiguration ProcessConfiguration1 = ProcessConfiguration.builder()
                .id(UUID.randomUUID())
                .name("SSH_BG")
                .processSettings(new ProcessSettings().toBuilder()
                        .name("SSH_BG")
                        .inputs(ImmutableList.of(inputBG))
                        .prerequisites(prerequisites)
                        .command(commandBG)
                        .validations(ImmutableList.of(validation1, validation2))
                        .build())
                .inCompounds(null)
                .inSections(sectionConfigurations)
                .projectConfiguration(testProjectConfiguration.get())
                .build();

        processes.add(ProcessConfiguration1);

        // Add Directories to project Configuration

        List<ProjectDirectory> directories = new ArrayList();
        UUID parentDirectoryId = UUID.randomUUID();
        ProjectDirectory projectDirectory = ProjectDirectory.builder()
                .id(projectChildDirectoryId)
                .name(projectChildDirectoryName)
                .directories(new ArrayList<>())
                .files(new ArrayList<>())
                .projectConfiguration(testProjectConfiguration.get())
                .build();
        directories.add(projectDirectory);

        // Linking Sections, ProjectConfiguration <-> Compounds, ProjectConfiguration
        sectionConfiguration.setCompounds(compounds);
        testProjectConfiguration.get().setCompounds(compounds);
        testProjectConfiguration.get().setProcesses(processes);
        testProjectConfiguration.get().setSections(sectionConfigurations);
        testProjectConfiguration.get().setDirectories(directories);
    }

    @AfterEach
    public void afterMiaFileControllerTest() {
        if(projectId.get() != null) {
            File file = new File("PROJECT_FOLDER/"+projectId.get());
            FileUtils.deleteFolder(file, true);
        }
    }

    public SectionDto getSectionDto(UUID sectionId, String sectionName, int position, UUID parentSectionId) {
        SectionDto sectionDto = new SectionDto();
        sectionDto.setId(sectionId);
        sectionDto.setName(sectionName);
        sectionDto.setPlace(position);
        sectionDto.setParentSection(parentSectionId);
        return sectionDto;
    }

    public CompoundDto getCompoundDto(UUID sectionId, UUID compoundId, String compoundName, UUID processId1,
                                       String processName1,
                                       UUID processId2,
                                       String processName2) {
        CompoundDto compoundDto = new CompoundDto();
        compoundDto.setId(compoundId);
        compoundDto.setName(compoundName);
        List<UUID> inSections = new ArrayList<>();
        inSections.add(sectionId);
        compoundDto.setInSections(inSections);
        List<ProcessDto> processDtos = new ArrayList<>();
        ProcessDto processDto1 = new ProcessDto();
        processDto1.setId(processId1);
        processDto1.setName(processName1);
        processDtos.add(processDto1);
        ProcessDto processDto2 = new ProcessDto();
        processDto2.setId(processId2);
        processDto2.setName(processName2);
        processDtos.add(processDto2);
        compoundDto.setProcesses(processDtos);
        return compoundDto;
    }

    public ProcessDto getProcessDto(UUID sectionId, UUID processId, String processName) {
        ProcessDto processDto = new ProcessDto();
        processDto.setId(processId);
        processDto.setName(processName);
        List<UUID> inSections = new ArrayList<>();
        inSections.add(sectionId);
        processDto.setInSections(inSections);

        ProcessSettingsDto processSettings = new ProcessSettingsDto();
        processDto.setProcessSettings(processSettings);
        return processDto;
    }

    public ProjectDirectoryDto getDirectoryDto(UUID directoryId, String directoryName, UUID parentDirectoryId) {
        ProjectDirectoryDto projectDirectoryDto = new ProjectDirectoryDto();
        projectDirectoryDto.setId(directoryId);
        projectDirectoryDto.setName(directoryName);
        projectDirectoryDto.setParentDirectoryId(parentDirectoryId);
        return projectDirectoryDto;
    }

}
