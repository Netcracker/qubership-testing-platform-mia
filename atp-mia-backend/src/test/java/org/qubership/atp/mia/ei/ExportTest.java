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

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.getFirstFileFromPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.ei.ExportImportCommonConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportCompound;
import org.qubership.atp.mia.model.ei.ExportImportDirectory;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportFile;
import org.qubership.atp.mia.model.ei.ExportImportHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportIdentifier;
import org.qubership.atp.mia.model.ei.ExportImportPotHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportProcess;
import org.qubership.atp.mia.model.ei.ExportImportSection;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.util.FileUtils;

public class ExportTest extends ExportImportBaseTest {

    private final Path testPath = Paths.get("src/test/resources/ei/export/");
    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    void exportAfterEach() throws IOException {
        FileUtils.deleteRecursive(path.get());
    }

    @Test
    public void export_allSections() throws IOException {
        //Prepare
        Map<String, Set<String>> importExportEntities = new HashMap<>();
        importExportEntities.put(ExportImportEntities.MIA_SECTION.getValue(),
                testProjectConfiguration.get().getSections().stream().map(s -> s.getId().toString()).collect(Collectors.toSet()));
        importExportEntities.put(ExportImportEntities.MIA_PROCESSES.getValue(),
                testProjectConfiguration.get().getProcesses().stream().map(p -> p.getId().toString()).collect(Collectors.toSet()));
        importExportEntities.put(ExportImportEntities.MIA_COMPOUNDS.getValue(),
                testProjectConfiguration.get().getCompounds().stream().map(c -> c.getId().toString()).collect(Collectors.toSet()));
        exportScope.get().setEntities(importExportEntities);
        //call
        atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());
        //check
        assertTrue(path.get().toFile().exists());
        for (int i = 1; i <= 2; i++) {
            checkFile(new UUID(2, i), ExportImportEntities.MIA_SECTION, ExportImportCompound.class);
        }
        for (int i = 1; i <= 6; i++) {
            checkFile(new UUID(3, i), ExportImportEntities.MIA_SECTION, ExportImportProcess.class);
        }
        for (int i = 1; i <= 4; i++) {
            checkFile(new UUID(1, i), ExportImportEntities.MIA_SECTION, ExportImportSection.class);
        }
    }

    @Test
    public void export_notAtp() {
        MiaException thrown = assertThrows(
                MiaException.class,
                () -> {
                    exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.NTT));
                    atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());
                },
                "Expected exportToFolder to throw, but it didn't"
        );
        assertEquals("MIA-7000: MIA does not support export type '" + ExportFormat.NTT + "'", thrown.getMessage());
    }

    @Test
    public void export_whenCommonConfiguration_thenOnlyItExported() {
        //Prepare
        Map<String, Set<String>> importExportEntities = new HashMap<>();
        importExportEntities.put(ExportImportEntities.MIA_COMMON_CONFIGURATION.getValue(), new HashSet<String>(){{
            add(projectId.get().toString());
        }});
        exportScope.get().setEntities(importExportEntities);
        //Call
        atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());
        //Check result folder exist
        assertTrue(path.get().toFile().exists(), "result folder not exist!");
        checkAmountOfExportedEntities(1, path.get());
        //Check configuration export file
        checkConfiguration(ExportImportCommonConfiguration.class);
    }

    @Test
    public void export_whenHeaderConfiguration_thenOnlyItExported() {
        //Prepare
        Map<String, Set<String>> importExportEntities = new HashMap<>();
        importExportEntities.put(ExportImportEntities.MIA_HEADER_CONFIGURATION.getValue(), new HashSet<String>(){{
            add(projectId.get().toString());
        }});
        exportScope.get().setEntities(importExportEntities);
        //Call
        atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());
        //Check result folder exist
        assertTrue(path.get().toFile().exists(), "result folder not exist!");
        checkAmountOfExportedEntities(1, path.get());
        //Check configuration export file
        checkConfiguration(ExportImportHeaderConfiguration.class);
    }

    @Test
    public void export_whenPotHeaderConfiguration_thenOnlyItExported() {
        //Prepare
        Map<String, Set<String>> importExportEntities = new HashMap<>();
        importExportEntities.put(ExportImportEntities.MIA_POT_HEADER_CONFIGURATION.getValue(), new HashSet<String>(){{
            add(projectId.get().toString());
        }});
        exportScope.get().setEntities(importExportEntities);
        //Call
        atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());
        //Check result folder exist
        assertTrue(path.get().toFile().exists(), "result folder not exist!");
        checkAmountOfExportedEntities(1, path.get());
        //Check configuration export file
        checkConfiguration(ExportImportPotHeaderConfiguration.class);
    }

    @Test
    public void export_whenCertainFileAndDirectory_thenOnlyItExported() {
        //Expect
        ProjectDirectory rootDirectory0 = directoryConfigurations.get().get("/rootDirectory0");
        ProjectDirectory rootDirectory0_Directory1 = directoryConfigurations.get().get("/rootDirectory0/Directory1");
        ProjectDirectory rootDirectory0_Directory2 = directoryConfigurations.get().get("/rootDirectory0/Directory2");
        ProjectFile rootDirectory0_Directory1_File2 = fileConfigurations.get().get("rootDirectory0_Directory1_File2.txt");
        ProjectFile rootDirectory0_Directory1_File3 = fileConfigurations.get().get("rootDirectory0_Directory1_File3.txt");

        //Prepare
        Map<String, Set<String>> importExportEntities = new HashMap<>();
        Set<String> directoriesToExport = new HashSet<String>() {{
            add(rootDirectory0.getId().toString());
            add(rootDirectory0_Directory1.getId().toString());
            add(rootDirectory0_Directory2.getId().toString());
        }};
        importExportEntities.put(ExportImportEntities.MIA_DIRECTORY.getValue(), directoriesToExport);

        Set<String> filesToExport = new HashSet<String>() {{
            add(rootDirectory0_Directory1_File2.getId().toString());
            add(rootDirectory0_Directory1_File3.getId().toString());
        }};
        importExportEntities.put(ExportImportEntities.MIA_FILES.getValue(), filesToExport);
        exportScope.get().setEntities(importExportEntities);

        //Call
        atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());

        //Check result folder exist
        assertTrue(path.get().toFile().exists(), "result folder not exist!");
        //Check amount of exported entities
        checkAmountOfExportedEntities(directoriesToExport.size(),
                path.get(), ExportImportEntities.MIA_FILES.getValue(), ExportImportDirectory.class.getSimpleName());
        checkAmountOfExportedEntities(filesToExport.size(),
                path.get(), ExportImportEntities.MIA_FILES.getValue(), ExportImportFile.class.getSimpleName());
        checkAmountOfExportedEntities(filesToExport.size(),
                path.get(), ExportImportEntities.MIA_FILES.getValue(), ExportImportEntities.GRID_FS_FILE.getValue());

        // Check directories
        directoriesToExport.stream()
                .map(UUID::fromString)
                .forEach(dirId -> checkFile(dirId, ExportImportEntities.MIA_FILES, ExportImportDirectory.class));

        // Check gridFsFiles
        for (String fileId : filesToExport) {
            checkMiaFile(UUID.fromString(fileId));
            checkGridFsFile(UUID.fromString(fileId));
        }
    }

    @Test
    public void export_whenAllDirFiles_thenAllExported() {
        //Prepare
        Set<String> directoryIdSet = directoryConfigurations.get().values().stream()
                .map(ProjectDirectory::getId)
                .map(UUID::toString)
                .collect(Collectors.toSet());

        Set<String> fileIdSet = fileConfigurations.get().values().stream()
                .map(ProjectFile::getId)
                .map(UUID::toString)
                .collect(Collectors.toSet());

        exportScope.get().setEntities(new HashMap<String, Set<String>>() {{
            put(ExportImportEntities.MIA_DIRECTORY.getValue(), directoryIdSet);
            put(ExportImportEntities.MIA_FILES.getValue(), fileIdSet);
        }});

        //Call
        atpMiaExportExecutor.exportToFolder(exportImportData.get(), path.get());

        //Check result folder exist
        assertTrue(path.get().toFile().exists(), "result folder not exist!");
        //Check amount of exported entities
        checkAmountOfExportedEntities(directoryIdSet.size(),
                path.get(), ExportImportEntities.MIA_FILES.getValue(), ExportImportDirectory.class.getSimpleName());
        checkAmountOfExportedEntities(fileIdSet.size(),
                path.get(), ExportImportEntities.MIA_FILES.getValue(), ExportImportFile.class.getSimpleName());
        checkAmountOfExportedEntities(fileIdSet.size(),
                path.get(), ExportImportEntities.MIA_FILES.getValue(), ExportImportEntities.GRID_FS_FILE.getValue());

        // Check directories
        for (String directoryId : directoryIdSet) {
            checkFile(UUID.fromString(directoryId), ExportImportEntities.MIA_FILES, ExportImportDirectory.class);
        }
        // Check files
        for (String fileId : fileIdSet) {
            checkMiaFile(UUID.fromString(fileId));
            checkGridFsFile(UUID.fromString(fileId));
        }
    }

    private void checkFile(UUID fileId, ExportImportEntities entity, Class<?> clazz) {
        checkFile(
                resolveFilePath(testPath, entity, clazz, fileId),
                resolveFilePath(path.get(), entity, clazz, fileId),
                clazz
        );
    }

    private void checkFile(File expectFile, File actualFile, Class<?> clazz) {
        assertTrue(expectFile.exists(), "file not exist: " + expectFile);
        assertTrue(actualFile.exists(), "file not exist: " + actualFile);
        try {
            assertEquals(mapper.readValue(expectFile, clazz), mapper.readValue(actualFile, clazz));
        } catch (IOException e) {
            fail("Can't read file" + e.getMessage());
        }
    }

    private <T extends ExportImportIdentifier> void checkConfiguration(Class<T> clazz) {
        File expectedFile = getFirstFileFromPath(testPath, ExportImportEntities.MIA_PROJECT_CONFIGURATION, clazz);
        File actualFile = getFirstFileFromPath(path.get(), ExportImportEntities.MIA_PROJECT_CONFIGURATION, clazz);
        try {
            T expectedConfig = mapper.readValue(expectedFile, clazz);
            T actualConfig = mapper.readValue(actualFile, clazz);
            expectedConfig.setId(actualConfig.getId());

            assertEquals(expectedConfig, actualConfig);
        } catch (IOException e) {
            fail("Can't read file" + e.getMessage());
        }
    }

    private void checkMiaFile(UUID fileId) {
        try {
            ExportImportFile expectedFile = mapper.readValue(
                    resolveFilePath(testPath, ExportImportEntities.MIA_FILES, ExportImportFile.class, fileId),
                    ExportImportFile.class
            );
            ExportImportFile actualFile = mapper.readValue(
                    resolveFilePath(path.get(), ExportImportEntities.MIA_FILES, ExportImportFile.class, fileId),
                    ExportImportFile.class
            );

            assertEquals(expectedFile.getId(), actualFile.getId());
            assertEquals(expectedFile.getName(), actualFile.getName());
            assertEquals(expectedFile.getDirectory(), actualFile.getDirectory());
        } catch (IOException e) {
            fail("Can't read file" + e.getMessage());
        }
    }

    private void checkGridFsFile(UUID fileId) {
        File expectedFile =
                getFiles(resolvePath(testPath, ExportImportEntities.MIA_FILES.getValue(), "gridFsFile", fileId.toString()).toFile())[0];
        File actualFile =
                getFiles(resolvePath(path.get(), ExportImportEntities.MIA_FILES.getValue(), "gridFsFile", fileId.toString()).toFile())[0];
        try {
            assertEquals(
                    Files.readAllLines(expectedFile.getAbsoluteFile().toPath()),
                    Files.readAllLines(actualFile.getAbsoluteFile().toPath())
            );
        } catch (IOException e) {
            fail("Can't read file" + e.getMessage());
        }
    }

    private void checkAmountOfExportedEntities(int expectedSize, Path path) {
        checkAmountOfExportedEntities(expectedSize, path, "");
    }

    private void checkAmountOfExportedEntities(int expectedSize, Path path, String... paths) {
        path = resolvePath(path, paths);

        int actual = getFiles(path.toFile()).length;
        assertEquals(expectedSize, actual,
                String.format("Expected %03d exported folders, but found %03d", expectedSize, actual));
    }

    private static File[] getFiles(File file) {
        if (nonNull(file) && nonNull(file.listFiles())) {
            assertTrue(file.listFiles().length > 0);
            return file.listFiles();
        }
        throw new RuntimeException("File not found " + file.getPath());
    }

    private static File resolveFilePath(Path path, ExportImportEntities entity, Class<?> clazz, UUID fileId) {
        return resolvePath(path, entity.getValue(), clazz.getSimpleName(), fileId + ".json").toFile();
    }

    private static Path resolvePath(Path path, String... paths) {
        for (String resolvePath : paths) {
            path = path.resolve(resolvePath);
        }
        return path;
    }
}
