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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.ei.component.ExportStrategiesRegistry;
import org.qubership.atp.mia.ei.component.ImportLoaderCommonConfiguration;
import org.qubership.atp.mia.ei.component.ImportLoaderCompound;
import org.qubership.atp.mia.ei.component.ImportLoaderDirectory;
import org.qubership.atp.mia.ei.component.ImportLoaderFile;
import org.qubership.atp.mia.ei.component.ImportLoaderHeaderConfiguration;
import org.qubership.atp.mia.ei.component.ImportLoaderPotHeaderConfiguration;
import org.qubership.atp.mia.ei.component.ImportLoaderProcess;
import org.qubership.atp.mia.ei.component.ImportLoaderSection;
import org.qubership.atp.mia.ei.executor.AtpMiaExportExecutor;
import org.qubership.atp.mia.ei.executor.AtpMiaImportExecutor;
import org.qubership.atp.mia.ei.service.AtpExportStrategy;
import org.qubership.atp.mia.ei.service.AtpImportStrategy;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.impl.executable.PotHeader;
import org.qubership.atp.mia.service.DeserializerConfigBaseTest;
import org.qubership.atp.mia.service.file.GridFsService;

import com.mongodb.client.gridfs.model.GridFSFile;

@ExtendWith(SkipTestInJenkins.class)
public class ExportImportBaseTest extends ConfigTestBean {

    protected AtpMiaExportExecutor atpMiaExportExecutor;
    protected AtpMiaImportExecutor atpMiaImportExecutor;
    protected ThreadLocal<ExportImportData> exportImportData = new ThreadLocal<>();
    protected ThreadLocal<ExportScope> exportScope = new ThreadLocal<>();
    protected ThreadLocal<ExportImportData> importDataAnotherProject = new ThreadLocal<>();
    protected ThreadLocal<Path> path = new ThreadLocal<>();

    protected ThreadLocal<Map<String, ProjectDirectory>> directoryConfigurations = new ThreadLocal<>();
    protected ThreadLocal<Map<String, ProjectFile>> fileConfigurations = new ThreadLocal<>();

    private void setUpConfiguration() {
        //Process settings
        List<ProcessConfiguration> processConfigurations = new ArrayList<>();
        processConfigurations.add(DeserializerConfigBaseTest.getBg());
        processConfigurations.add(DeserializerConfigBaseTest.getBgforswitcher());
        processConfigurations.add(DeserializerConfigBaseTest.getBgWithMarker());
        processConfigurations.add(DeserializerConfigBaseTest.getSoapTestData());
        processConfigurations.add(DeserializerConfigBaseTest.getDefaultRest());
        processConfigurations.add(DeserializerConfigBaseTest.getSshTestData());
        AtomicInteger id = new AtomicInteger(1);
        processConfigurations.forEach(p -> {
            p.setId(new UUID(3, id.getAndIncrement()));
            p.setProjectConfiguration(testProjectConfiguration.get());
        });
        //Compound settings
        List<CompoundConfiguration> compoundConfigurations = new ArrayList<>();
        compoundConfigurations.add(CompoundConfiguration.builder()
                .id(new UUID(2, 1))
                .name("Compound1")
                .referToInput("blabla")
                .processes(new ArrayList<ProcessConfiguration>() {{
                    add(processConfigurations.get(0));
                    add(processConfigurations.get(1));
                    add(processConfigurations.get(0));
                }})
                .projectConfiguration(testProjectConfiguration.get())
                .build());
        compoundConfigurations.add(CompoundConfiguration.builder()
                .id(new UUID(2, 2))
                .name("Compound2")
                .processes(new ArrayList<ProcessConfiguration>() {{
                    add(processConfigurations.get(0));
                    add(processConfigurations.get(2));
                }})
                .projectConfiguration(testProjectConfiguration.get())
                .build());
        //Section settings
        List<SectionConfiguration> sectionConfigurations = new ArrayList<>();
        sectionConfigurations.add(SectionConfiguration.builder()
                .id(new UUID(1, 1))
                .name("Section1")
                .place(0)
                .parentSection(null)
                .processes(new ArrayList<ProcessConfiguration>() {{
                    add(processConfigurations.get(0));
                    add(processConfigurations.get(1));
                }})
                .compounds(new ArrayList<CompoundConfiguration>() {{
                    add(compoundConfigurations.get(0));
                }})
                .projectConfiguration(testProjectConfiguration.get())
                .build());
        sectionConfigurations.add(SectionConfiguration.builder()
                .id(new UUID(1, 2))
                .name("Section2")
                .place(1)
                .parentSection(null)
                .processes(new ArrayList<ProcessConfiguration>() {{
                    add(processConfigurations.get(2));
                    add(processConfigurations.get(4));
                }})
                .projectConfiguration(testProjectConfiguration.get())
                .build());
        sectionConfigurations.add(SectionConfiguration.builder()
                .id(new UUID(1, 3))
                .name("Section3")
                .place(2)
                .parentSection(null)
                .projectConfiguration(testProjectConfiguration.get())
                .build());
        sectionConfigurations.add(SectionConfiguration.builder()
                .id(new UUID(1, 4))
                .name("Section1.1")
                .place(0)
                .parentSection(sectionConfigurations.get(0))
                .compounds(new ArrayList<CompoundConfiguration>() {{
                    add(compoundConfigurations.get(0));
                    add(compoundConfigurations.get(1));
                }})
                .processes(new ArrayList<ProcessConfiguration>() {{
                    add(processConfigurations.get(0));
                    add(processConfigurations.get(3));
                    add(processConfigurations.get(5));
                }})
                .projectConfiguration(testProjectConfiguration.get())
                .build());
        sectionConfigurations.get(0).setSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(3));
        }});
        //Set section in processes
        processConfigurations.get(0).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(0));
            add(sectionConfigurations.get(3));
        }});
        processConfigurations.get(1).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(0));
        }});
        processConfigurations.get(2).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(1));
        }});
        processConfigurations.get(3).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(3));
        }});
        processConfigurations.get(4).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(1));
        }});
        processConfigurations.get(5).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(3));
        }});
        //Set compound in processes
        processConfigurations.get(0).setInCompounds(new ArrayList<CompoundConfiguration>() {{
            add(compoundConfigurations.get(0));
            add(compoundConfigurations.get(1));
        }});
        processConfigurations.get(1).setInCompounds(new ArrayList<CompoundConfiguration>() {{
            add(compoundConfigurations.get(0));
        }});
        processConfigurations.get(2).setInCompounds(new ArrayList<CompoundConfiguration>() {{
            add(compoundConfigurations.get(1));
        }});
        //Set section in compounds
        compoundConfigurations.get(0).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(0));
            add(sectionConfigurations.get(3));
        }});
        compoundConfigurations.get(1).setInSections(new ArrayList<SectionConfiguration>() {{
            add(sectionConfigurations.get(3));
        }});
        //Set projectID in common configuration as configurations already created in ConfigTestBean
        testProjectConfiguration.get().getCommonConfiguration().getCommandShellPrefixes()
                .forEach(x -> x.setCommonConfiguration(testProjectConfiguration.get().getCommonConfiguration()));
        testProjectConfiguration.get().getCommonConfiguration().setProjectId(projectId.get());
        testProjectConfiguration.get().setGitUrl("");
        testProjectConfiguration.get().getHeaderConfiguration().setProjectId(projectId.get());
        testProjectConfiguration.get().setPotHeaderConfiguration(
                PotHeaderConfiguration.builder()
                        .projectId(projectId.get())
                        .headers(new ArrayList<PotHeader>() {{
                            add(new PotHeader("MainHeader", "Text", "Billing System", "MainHeader"));
                            add(new PotHeader("SubHeader", "Digit", "Billing System", "1"));
                        }})
                        .build()
        );

        //Create directories
        Map<String, ProjectDirectory> directoryConfigurationsMap = Stream.of(
                buildDirectory(new UUID(1, 7), "/rootDirectory0"),
                buildDirectory(new UUID(1, 8), "/rootDirectory0/Directory1"),
                buildDirectory(new UUID(1, 9), "/rootDirectory1"),
                buildDirectory(new UUID(1, 10), "/rootDirectory0/Directory2")
        ).collect(
                Collectors.toMap(
                        ProjectDirectory::getName,
                        Function.identity()
                )
        );

        // Create files
        Map<String, ProjectFile> fileConfigurationsMap = Stream.of(
                buildFileAndMockGridFs(new UUID(1, 15), "rootFile0", "testContent0"),
                buildFileAndMockGridFs(new UUID(1, 16), "rootDirectory0_File1.txt", "testContent1"),
                buildFileAndMockGridFs(new UUID(1, 17), "rootDirectory0_Directory1_File2.txt", "testContent2"),
                buildFileAndMockGridFs(new UUID(1, 18), "rootDirectory0_Directory1_File3.txt", "testContent3"),
                buildFileAndMockGridFs(new UUID(1, 19), "rootDirectory0_Directory2_File4.txt", "testContent4"),
                buildFileAndMockGridFs(new UUID(1, 20), "rootDirectory0_Directory2_File5.txt", "testContent5"),
                buildFileAndMockGridFs(new UUID(1, 21), "rootDirectory0_File6.txt", "testContent6")
        ).collect(
                Collectors.toMap(
                        ProjectFile::getName,
                        Function.identity()
                )
        );

        // set directory in directories
        directoryConfigurationsMap.get("/rootDirectory0").setDirectories(new ArrayList<ProjectDirectory>() {{
            add(directoryConfigurationsMap.get("/rootDirectory0/Directory1"));
            add(directoryConfigurationsMap.get("/rootDirectory0/Directory2"));
        }});
        // set parent directory in directory
        directoryConfigurationsMap.get("/rootDirectory0/Directory1")
                .setParentDirectory(directoryConfigurationsMap.get("/rootDirectory0"));
        directoryConfigurationsMap.get("/rootDirectory0/Directory2")
                .setParentDirectory(directoryConfigurationsMap.get("/rootDirectory0"));
        // set files in directories
        directoryConfigurationsMap.get("/rootDirectory0").setFiles(new ArrayList<ProjectFile>() {{
            add(fileConfigurationsMap.get("rootDirectory0_File1.txt"));
            add(fileConfigurationsMap.get("rootDirectory0_File6.txt"));
        }});
        directoryConfigurationsMap.get("/rootDirectory0/Directory1").setFiles(new ArrayList<ProjectFile>() {{
            add(fileConfigurationsMap.get("rootDirectory0_Directory1_File2.txt"));
            add(fileConfigurationsMap.get("rootDirectory0_Directory1_File3.txt"));
        }});
        directoryConfigurationsMap.get("/rootDirectory0/Directory2").setFiles(new ArrayList<ProjectFile>() {{
            add(fileConfigurationsMap.get("rootDirectory0_Directory2_File4.txt"));
            add(fileConfigurationsMap.get("rootDirectory0_Directory2_File5.txt"));
        }});
        // set directories in files
        fileConfigurationsMap.get("rootDirectory0_File1.txt")
                .setDirectory(directoryConfigurationsMap.get("/rootDirectory0"));
        fileConfigurationsMap.get("rootDirectory0_Directory1_File2.txt")
                .setDirectory(directoryConfigurationsMap.get("/rootDirectory0/Directory1"));
        fileConfigurationsMap.get("rootDirectory0_Directory1_File3.txt")
                .setDirectory(directoryConfigurationsMap.get("/rootDirectory0/Directory1"));
        fileConfigurationsMap.get("rootDirectory0_Directory2_File4.txt")
                .setDirectory(directoryConfigurationsMap.get("/rootDirectory0/Directory2"));
        fileConfigurationsMap.get("rootDirectory0_Directory2_File5.txt")
                .setDirectory(directoryConfigurationsMap.get("/rootDirectory0/Directory2"));
        fileConfigurationsMap.get("rootDirectory0_File6.txt")
                .setDirectory(directoryConfigurationsMap.get("/rootDirectory0"));
        //set directory field
        directoryConfigurations.set(directoryConfigurationsMap);
        fileConfigurations.set(fileConfigurationsMap);
        //set test projectConfiguration
        testProjectConfiguration.get().setSections(sectionConfigurations);
        testProjectConfiguration.get().setCompounds(compoundConfigurations);
        testProjectConfiguration.get().setProcesses(processConfigurations);
        testProjectConfiguration.get().setDirectories(new ArrayList<>(directoryConfigurationsMap.values()));
        testProjectConfiguration.get().setFiles(new ArrayList<>(fileConfigurationsMap.values()));
        when(projectConfigurationService.get().getConfigByProjectId(eq(projectId.get())))
                .thenReturn(testProjectConfiguration.get());
        when(projectConfigurationService.get().getConfiguration(eq(projectId.get())))
                .thenReturn(testProjectConfiguration.get());
    }

    @BeforeEach
    void exportImportBaseTestBeforeEach() {
        ObjectLoaderFromDiskService loaderToDisk = new ObjectLoaderFromDiskService();
        FileService eiFileService = new FileService();
        ExportStrategiesRegistry exportStrategiesRegistry = new ExportStrategiesRegistry(
                Arrays.asList(
                        new AtpExportStrategy(
                                eiFileService,
                                gridFsService.get(),
                                new ObjectSaverToDiskService(eiFileService, true),
                                projectConfigurationService.get()
                        )
                )
        );
        atpMiaExportExecutor = new AtpMiaExportExecutor(exportStrategiesRegistry);
        atpMiaImportExecutor = new AtpMiaImportExecutor(new AtpImportStrategy(
                Arrays.asList(
                        new ImportLoaderSection(loaderToDisk),
                        new ImportLoaderProcess(loaderToDisk),
                        new ImportLoaderCompound(loaderToDisk),
                        new ImportLoaderDirectory(loaderToDisk, gridFsService.get(), directoryConfigurationRepository.get(), projectConfigurationService.get()),
                        new ImportLoaderFile(loaderToDisk, gridFsService.get(), atpUserService, fileConfigurationRepository.get(), projectConfigurationService.get()),
                        new ImportLoaderSection(loaderToDisk),
                        new ImportLoaderCommonConfiguration(loaderToDisk),
                        new ImportLoaderHeaderConfiguration(loaderToDisk),
                        new ImportLoaderPotHeaderConfiguration(loaderToDisk)
                ),
                projectConfigurationService.get(),
                Mockito.mock(NotificationService.class)
        ));
        exportScope.set(new ExportScope());
        path.set(Paths.get("TestExportImport").resolve(projectId.get().toString()));
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP));
        importDataAnotherProject.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), new HashMap<>(), null, null, false));
        setUpConfiguration();
    }

    private ProjectDirectory buildDirectory(UUID id, String name) {
        return ProjectDirectory.builder()
                .id(id)
                .name(name)
                .projectConfiguration(testProjectConfiguration.get())
                .build();
    }

    private ProjectFile buildFileAndMockGridFs(UUID fileId, String name, String content) {
        GridFsService gridFsServiceMock = gridFsService.get();
        BsonValue objectId = new BsonObjectId();
        GridFSFile gridFSFile = new GridFSFile(objectId, name, 3, 0, new Date(), new Document());
        byte[] fileAsBytes = content.getBytes();

        when(gridFsServiceMock.getFile(objectId.asObjectId().getValue().toString())).thenReturn(Optional.of(gridFSFile));
        when(gridFsServiceMock.getByteArrayFromGridFsFile(gridFSFile)).thenReturn(fileAsBytes);
        when(gridFsRepository.get().save(any(FileMetaData.class), any(InputStream.class))).thenReturn(new ObjectId());

        return ProjectFile.builder()
                .id(fileId)
                .sourceId(fileId)
                .name(name)
                .gridFsObjectId(objectId.asObjectId().getValue().toString())
                .lastUpdateWhen(LocalDateTime.now())
                .lastUpdateBy("autotest")
                .size((long) content.length())
                .projectConfiguration(testProjectConfiguration.get())
                .build();
    }
}
