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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.javers.common.string.PrettyValuePrinter;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversCoreProperties;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChangeMetadata;
import org.javers.core.diff.changetype.PropertyChangeType;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.CdoSnapshotBuilder;
import org.javers.core.metamodel.object.CdoSnapshotState;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.core.metamodel.object.SnapshotType;
import org.javers.core.metamodel.object.ValueObjectId;
import org.javers.core.metamodel.type.UnknownType;
import org.javers.shadow.Shadow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.CompareEntityResponseDto;
import org.qubership.atp.mia.controllers.api.dto.CompoundHistoryChangeDto;
import org.qubership.atp.mia.controllers.api.dto.FileHistoryChangeDto;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemDto;
import org.qubership.atp.mia.controllers.api.dto.HistoryItemResponseDto;
import org.qubership.atp.mia.controllers.api.dto.SectionHistoryChangeDto;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.service.history.impl.CompoundRetrieveHistoryService;
import org.qubership.atp.mia.service.history.impl.CompoundVersioningMapper;
import org.qubership.atp.mia.service.history.impl.FileRetrieveHistoryService;
import org.qubership.atp.mia.service.history.impl.FileVersioningMapper;
import org.qubership.atp.mia.service.history.impl.ProjectConfigurationRetrieveHistoryService;
import org.qubership.atp.mia.service.history.impl.ProjectConfigurationVersioningMapper;
import org.qubership.atp.mia.service.history.impl.SectionRetrieveHistoryService;
import org.qubership.atp.mia.service.history.impl.SectionVersioningMapper;

public class RetrieveHistoryServiceTest {
    private final ThreadLocal<Javers> javers = new ThreadLocal<>();
    private final ThreadLocal<CommitMetadata> metadata = new ThreadLocal<>();
    private final ThreadLocal<SectionRetrieveHistoryService> sectionRetrieveHistoryService = new ThreadLocal<>();
    private final ThreadLocal<ProjectConfigurationRetrieveHistoryService> projectConfigurationRetrieveHistoryService =
            new ThreadLocal<>();

    private final ThreadLocal<CompoundRetrieveHistoryService> compoundRetrieveHistoryService =
            new ThreadLocal<>();
    private static final ModelMapper mapper = new ModelMapper();
    private static final SectionVersioningMapper sectionVersioningMapper = new SectionVersioningMapper(mapper);
    private static final ProjectConfigurationVersioningMapper projectConfigurationVersioningMapper =
            new ProjectConfigurationVersioningMapper(mapper);
    private static final CompoundVersioningMapper compoundVersioningMapper = new CompoundVersioningMapper(mapper);
    private final ThreadLocal<FileRetrieveHistoryService> fileRetrieveHistoryService = new ThreadLocal<>();
    private static final FileVersioningMapper fileVersioningMapper = new FileVersioningMapper(mapper);

    @BeforeEach
    public void setUp() {
        Javers javersMock = mock(Javers.class);
        CommitMetadata commitMetadataMock = mock(CommitMetadata.class);
        javers.set(javersMock);
        metadata.set(commitMetadataMock);
        sectionRetrieveHistoryService.set(
                new SectionRetrieveHistoryService(javersMock, sectionVersioningMapper));
        projectConfigurationRetrieveHistoryService.set(
                new ProjectConfigurationRetrieveHistoryService(javersMock, projectConfigurationVersioningMapper));
        compoundRetrieveHistoryService.set(new CompoundRetrieveHistoryService(javersMock, compoundVersioningMapper));
        fileRetrieveHistoryService.set(new FileRetrieveHistoryService(javersMock, fileVersioningMapper));
    }

    @Test
    public void getEntitiesByVersionsTest_SectionConfiguration_ReturnHistoryItem() {
        UUID cdoId = UUID.randomUUID();
        Shadow shadow = mock(Shadow.class);

        SectionConfiguration section1 = createSection("section 1", "process 1", "process 2");
        SectionConfiguration section2 = createSection("section 2", "process 2");

        when(javers.get().findShadows(any())).thenReturn(Collections.singletonList(shadow));
        doReturn(section1, section2).when(shadow).get();
        CommitMetadata commitMetadata = new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                null, CommitId.valueOf(BigDecimal.valueOf(800.0)));
        when(shadow.getCommitMetadata()).thenReturn(commitMetadata);
        CdoSnapshot snapshot = CdoSnapshotBuilder.cdoSnapshot()
                .withManagedType(new UnknownType("entity"))
                .withCommitMetadata(commitMetadata)
                .withState(mock(CdoSnapshotState.class))
                .withType(SnapshotType.UPDATE)
                .withGlobalId(mock(ValueObjectId.class))
                .build();
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));

        List<CompareEntityResponseDto> result = sectionRetrieveHistoryService.get()
                .getEntitiesByVersions(cdoId, Arrays.asList("1", "2"));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("1", result.get(0).getRevision());
        Assertions.assertEquals("2", result.get(1).getRevision());
        Assertions.assertTrue(result.get(0).getCompareEntity() instanceof SectionHistoryChangeDto);
        Assertions.assertTrue(result.get(1).getCompareEntity() instanceof SectionHistoryChangeDto);
        SectionHistoryChangeDto entity1 = (SectionHistoryChangeDto) result.get(0).getCompareEntity();
        SectionHistoryChangeDto entity2 = (SectionHistoryChangeDto) result.get(1).getCompareEntity();
        Assertions.assertEquals(2, entity1.getProcesses().size());
        Assertions.assertEquals(1, entity2.getProcesses().size());
        Assertions.assertEquals("author", entity1.getModifiedBy());
        Assertions.assertEquals("author", entity2.getModifiedBy());
        Assertions.assertEquals(2, entity1.getCompounds().size());
        Assertions.assertEquals(2, entity2.getCompounds().size());
        //Assertions.assertEquals(1, entity1.getChildSections().size());
        //Assertions.assertEquals(1, entity2.getChildSections().size());
    }

    @Test
    public void getAllHistoryTest_SectionConfiguration_ReturnAllHistory() {
        UUID cdoId = UUID.randomUUID();
        GlobalId globalId = new InstanceId(SectionConfiguration.class.getTypeName(), cdoId, cdoId.toString());
        Changes changes = new Changes(createChanges(globalId, "name", "order"),
                new PrettyValuePrinter(new JaversCoreProperties.PrettyPrintDateFormats()));

        when(javers.get().findChanges(any())).thenReturn(changes);
        CdoSnapshot snapshot = CdoSnapshotBuilder.cdoSnapshot()
                .withManagedType(new UnknownType("entity"))
                .withCommitMetadata(new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                        null, new CommitId(1L, 1)))
                .withState(mock(CdoSnapshotState.class))
                .withType(SnapshotType.UPDATE)
                .withGlobalId(mock(ValueObjectId.class))
                .withVersion(1L)
                .build();
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));
        when(metadata.get().getId()).thenReturn(new CommitId(1L, 1));
        when(metadata.get().getCommitDate()).thenReturn(LocalDateTime.now());

        HistoryItemResponseDto response = sectionRetrieveHistoryService.get().getAllHistory(cdoId, 0, 10);

        HistoryItemDto historyItem = response.getHistoryItems().get(0);
        Assertions.assertEquals(2, historyItem.getChanged().size());
        Assertions.assertEquals("name", historyItem.getChanged().get(0));
        Assertions.assertEquals("order", historyItem.getChanged().get(1));
    }

    @Test
    public void getEntitiesByVersionsTest_CompoundConfiguration_ReturnHistoryItem() {
        UUID cdoId = UUID.randomUUID();
        Shadow shadow = mock(Shadow.class);

        CompoundConfiguration compound1 = createCompounds("compound 1", "process 1", "process 2");
        CompoundConfiguration compound2 = createCompounds("compound 2", "process 2");

        when(javers.get().findShadows(any())).thenReturn(Collections.singletonList(shadow));
        doReturn(compound1, compound2).when(shadow).get();
        CommitMetadata commitMetadata = new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                null, CommitId.valueOf(BigDecimal.valueOf(800.0)));
        when(shadow.getCommitMetadata()).thenReturn(commitMetadata);
        CdoSnapshot snapshot = CdoSnapshotBuilder.cdoSnapshot()
                .withManagedType(new UnknownType("entity"))
                .withCommitMetadata(commitMetadata)
                .withState(mock(CdoSnapshotState.class))
                .withType(SnapshotType.UPDATE)
                .withGlobalId(mock(ValueObjectId.class))
                .build();
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));

        List<CompareEntityResponseDto> result = compoundRetrieveHistoryService.get()
                .getEntitiesByVersions(cdoId, Arrays.asList("1", "2"));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("1", result.get(0).getRevision());
        Assertions.assertEquals("2", result.get(1).getRevision());
        Assertions.assertTrue(result.get(0).getCompareEntity() instanceof CompoundHistoryChangeDto);
        Assertions.assertTrue(result.get(1).getCompareEntity() instanceof CompoundHistoryChangeDto);
        CompoundHistoryChangeDto entity1 = (CompoundHistoryChangeDto) result.get(0).getCompareEntity();
        CompoundHistoryChangeDto entity2 = (CompoundHistoryChangeDto) result.get(1).getCompareEntity();
        Assertions.assertEquals(2, entity1.getProcesses().size());
        Assertions.assertEquals(1, entity2.getProcesses().size());
        Assertions.assertEquals("author", entity1.getModifiedBy());
        Assertions.assertEquals("author", entity2.getModifiedBy());
        Assertions.assertEquals(1, entity1.getInSections().size());
        Assertions.assertEquals(1, entity2.getInSections().size());
    }

    @Test
    public void getAllHistoryTest_CompoundConfiguration_ReturnAllHistory() {
        UUID cdoId = UUID.randomUUID();
        GlobalId globalId = new InstanceId(CompoundConfiguration.class.getTypeName(), cdoId, cdoId.toString());
        Changes changes = new Changes(createChanges(globalId, "name", "inSections", "processes", "referToInput"),
                new PrettyValuePrinter(new JaversCoreProperties.PrettyPrintDateFormats()));

        when(javers.get().findChanges(any())).thenReturn(changes);
        CdoSnapshot snapshot = CdoSnapshotBuilder.cdoSnapshot()
                .withManagedType(new UnknownType("entity"))
                .withCommitMetadata(new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                        null, new CommitId(1L, 1)))
                .withState(mock(CdoSnapshotState.class))
                .withType(SnapshotType.UPDATE)
                .withGlobalId(mock(ValueObjectId.class))
                .withVersion(1L)
                .build();
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));
        when(metadata.get().getId()).thenReturn(new CommitId(1L, 1));
        when(metadata.get().getCommitDate()).thenReturn(LocalDateTime.now());

        HistoryItemResponseDto response = sectionRetrieveHistoryService.get().getAllHistory(cdoId, 0, 10);

        HistoryItemDto historyItem = response.getHistoryItems().get(0);
        Assertions.assertEquals(4, historyItem.getChanged().size());
        Assertions.assertEquals("name", historyItem.getChanged().get(0));
        Assertions.assertEquals("inSections", historyItem.getChanged().get(1));
        Assertions.assertEquals("processes", historyItem.getChanged().get(2));
        Assertions.assertEquals("referToInput", historyItem.getChanged().get(3));
    }

    @Test
    public void getEntitiesByVersionsTest_ProjectFile_ReturnHistoryItem() {
        UUID cdoId = UUID.randomUUID();
        Shadow shadow = mock(Shadow.class);

        Timestamp createdWhen = new Timestamp(new Date().getTime());
        ProjectFile file1 = ProjectFile.builder().name("file 1").createdWhen(createdWhen)
                .modifiedWhen(createdWhen)
                .directory(ProjectDirectory.builder().name("parent directory").build())
                .gridFsObjectId("object 1").build();
        ProjectFile file2 = ProjectFile.builder().name("file 2").createdWhen(createdWhen)
                .modifiedWhen(createdWhen)
                .directory(ProjectDirectory.builder().name("parent directory").build())
                .gridFsObjectId("object 2").build();

        when(javers.get().findShadows(any())).thenReturn(Collections.singletonList(shadow));
        doReturn(file1, file2).when(shadow).get();
        CommitMetadata commitMetadata = new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                null, CommitId.valueOf(BigDecimal.valueOf(800.0)));
        when(shadow.getCommitMetadata()).thenReturn(commitMetadata);
        CdoSnapshot snapshot = CdoSnapshotBuilder.cdoSnapshot()
                .withManagedType(new UnknownType("entity"))
                .withCommitMetadata(commitMetadata)
                .withState(mock(CdoSnapshotState.class))
                .withType(SnapshotType.UPDATE)
                .withGlobalId(mock(ValueObjectId.class))
                .build();
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));

        List<CompareEntityResponseDto> result = fileRetrieveHistoryService.get()
                .getEntitiesByVersions(cdoId, Arrays.asList("1", "2"));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("1", result.get(0).getRevision());
        Assertions.assertEquals("2", result.get(1).getRevision());
        Assertions.assertTrue(result.get(0).getCompareEntity() instanceof FileHistoryChangeDto);
        Assertions.assertTrue(result.get(1).getCompareEntity() instanceof FileHistoryChangeDto);
        FileHistoryChangeDto entity1 = (FileHistoryChangeDto) result.get(0).getCompareEntity();
        FileHistoryChangeDto entity2 = (FileHistoryChangeDto) result.get(1).getCompareEntity();
        Assertions.assertEquals(createdWhen.getTime(), entity1.getCreatedWhen(), 10);
        Assertions.assertEquals(createdWhen.getTime(), entity2.getCreatedWhen(), 10);
        Assertions.assertEquals(createdWhen.getTime(), entity1.getModifiedWhen(), 10);
        Assertions.assertEquals(createdWhen.getTime(), entity2.getModifiedWhen(), 10);
        Assertions.assertEquals("author", entity1.getModifiedBy());
        Assertions.assertEquals("author", entity2.getModifiedBy());
    }

    @Test
    public void getAllHistoryTest_ProjectFile_ReturnAllHistory() {
        UUID cdoId = UUID.randomUUID();
        GlobalId globalId = new InstanceId(ProjectFile.class.getTypeName(), cdoId, cdoId.toString());
        Changes changes = new Changes(createChanges(globalId, "name", "size"),
                new PrettyValuePrinter(new JaversCoreProperties.PrettyPrintDateFormats()));

        when(javers.get().findChanges(any())).thenReturn(changes);
        CdoSnapshot snapshot = CdoSnapshotBuilder.cdoSnapshot()
                .withManagedType(new UnknownType("entity"))
                .withCommitMetadata(new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                        null, new CommitId(1L, 1)))
                .withState(mock(CdoSnapshotState.class))
                .withType(SnapshotType.UPDATE)
                .withGlobalId(mock(ValueObjectId.class))
                .withVersion(1L)
                .build();
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));
        when(metadata.get().getId()).thenReturn(new CommitId(1L, 1));
        when(metadata.get().getCommitDate()).thenReturn(LocalDateTime.now());

        HistoryItemResponseDto response = fileRetrieveHistoryService.get().getAllHistory(cdoId, 0, 10);

        HistoryItemDto historyItem = response.getHistoryItems().get(0);
        Assertions.assertEquals(2, historyItem.getChanged().size());
        Assertions.assertEquals("name", historyItem.getChanged().get(0));
        Assertions.assertEquals("size", historyItem.getChanged().get(1));
    }

    private SectionConfiguration createSection(String name, String ... childProcesses) {
        SectionConfiguration section = new SectionConfiguration();
        section.setName(name);
        List<ProcessConfiguration> processes = new ArrayList<>();
        for (String processName : childProcesses) {
            processes.add(ProcessConfiguration.builder().name(processName).build());
        }
        section.setProcesses(processes);
        section.setSections(Collections.singletonList(SectionConfiguration.builder().name("parent section").build()));
        section.setCompounds(Arrays.asList(
                CompoundConfiguration.builder().name("compound 1").build(),
                CompoundConfiguration.builder().name("compound 2").build()
        ));
        section.setModifiedWhen(new Timestamp(new Date().getTime()));
        section.setCreatedWhen(new Timestamp(new Date().getTime()));
        return section;
    }

    private CompoundConfiguration createCompounds(String name, String ... childProcesses) {
        CompoundConfiguration compound = new CompoundConfiguration();
        compound.setName(name);
        List<ProcessConfiguration> processes = new ArrayList<>();
        for (String processName : childProcesses) {
            processes.add(ProcessConfiguration.builder().name(processName).build());
        }
        compound.setProcesses(processes);
        compound.setInSections(Collections.singletonList(SectionConfiguration.builder().name("parent section").build()));
        compound.setModifiedWhen(new Timestamp(new Date().getTime()));
        compound.setCreatedWhen(new Timestamp(new Date().getTime()));
        return compound;
    }

    private List<Change> createChanges(GlobalId globalId, String ... fields) {
        List<Change> valueChanges = new ArrayList<>();
        for (String field : fields) {
            ValueChange change = new ValueChange(
                    new PropertyChangeMetadata(
                            globalId,
                            field,
                            Optional.of(metadata.get()),
                            PropertyChangeType.PROPERTY_VALUE_CHANGED
                    ),
                    RandomStringUtils.random(3),
                    RandomStringUtils.random(3)
            );
            valueChanges.add(change);
        }
        return valueChanges;
    }
}
