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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.readEntity;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.readExportDirectory;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.readExportFile;
import static org.qubership.atp.mia.ei.ExportImportTestUtils.validateFilenameChanged;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.mia.exceptions.ei.MiaImportParentDirectoryNotFound;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportCommonConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportDirectory;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportFile;
import org.qubership.atp.mia.model.ei.ExportImportHeaderConfiguration;
import org.qubership.atp.mia.model.ei.ExportImportPotHeaderConfiguration;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;

public class ImportTestCornerCases extends ExportImportBaseTest {

    private final ThreadLocal<Path> testPath = new ThreadLocal<>();

    @AfterEach
    void importAfterEach() throws IOException {
        io.undertow.util.FileUtils.deleteRecursive(path.get());
    }

    @Test
    public void import_whenConfiguration_thenImported(TestInfo testInfo) {
        // Prepare
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        ExportImportCommonConfiguration expectedCommonConfiguration =
                readEntity(testPath.get(), ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportCommonConfiguration.class);
        expectedCommonConfiguration.setId(projectId.get());

        ExportImportHeaderConfiguration expectedHeaderConfiguration =
                readEntity(testPath.get(), ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportHeaderConfiguration.class);
        expectedHeaderConfiguration.setId(projectId.get());

        ExportImportPotHeaderConfiguration expectedPotHeaderConfiguration =
                readEntity(testPath.get(), ExportImportEntities.MIA_PROJECT_CONFIGURATION, ExportImportPotHeaderConfiguration.class);
        expectedPotHeaderConfiguration.setId(projectId.get());

        // Clean configuration
        testProjectConfiguration.get().setCommonConfiguration(new CommonConfiguration());
        testProjectConfiguration.get().setHeaderConfiguration(new HeaderConfiguration());
        testProjectConfiguration.get().setPotHeaderConfiguration(new PotHeaderConfiguration());

        assertNotEquals(expectedCommonConfiguration, new ExportImportCommonConfiguration(testProjectConfiguration.get().getCommonConfiguration()));
        assertNotEquals(expectedHeaderConfiguration, new ExportImportHeaderConfiguration(testProjectConfiguration.get().getHeaderConfiguration()));
        assertNotEquals(expectedPotHeaderConfiguration, new ExportImportPotHeaderConfiguration(testProjectConfiguration.get().getPotHeaderConfiguration()));

        // Then call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());

        assertEquals(expectedCommonConfiguration, new ExportImportCommonConfiguration(testProjectConfiguration.get().getCommonConfiguration()));
        assertEquals(expectedHeaderConfiguration, new ExportImportHeaderConfiguration(testProjectConfiguration.get().getHeaderConfiguration()));
        assertEquals(expectedPotHeaderConfiguration, new ExportImportPotHeaderConfiguration(testProjectConfiguration.get().getPotHeaderConfiguration()));
    }

    /*
     * Disclaimer: The structure below differs from the actual debug variables
     *  and outlines the files and directories validated during testing.
     *
     * This test validates what if we swap directories and files in structure.
     *
     * Import archive with names:                * Import archive with ids:
     * /rootDirectory0/Directory1                * 00000000-0000-0001-0000-000000000008
     * |- rootDirectory0_File1.txt               * |- 00000000-0000-0001-0000-000000000010 (file)
     * |- /rootDirectory0                        * |- 00000000-0000-0001-0000-000000000007
     *    |- rootDirectory0_Directory1_File2.txt *    |- 00000000-0000-0001-0000-000000000011 (file)
     * ------------------------------------------* --------------------------------------------------
     * Existing structure with names:            * Existing structure with ids:
     * /rootDirectory0                           * 00000000-0000-0001-0000-000000000007
     * |- rootDirectory0_File1.txt               * |- 00000000-0000-0001-0000-000000000010 (file)
     * |- rootDirectory0_File6.txt               * |- 00000000-0000-0001-0000-000000000015 (file)
     * |- /rootDirectory0/Directory1             * |- 00000000-0000-0001-0000-000000000008
     *    |- rootDirectory0_Directory1_File2.txt *    |- 00000000-0000-0001-0000-000000000011 (file)
     *    |- rootDirectory0_Directory1_File3.txt *    |- 00000000-0000-0001-0000-000000000012 (file)
     * ------------------------------------------* --------------------------------------------------
     * After import with names:                  * After import with ids:
     * /rootDirectory0/Directory1                * 00000000-0000-0001-0000-000000000008              - swapped
     * |- rootDirectory0_File1.txt               * |- 00000000-0000-0001-0000-000000000010 (file)    - swapped
     * |- rootDirectory0_Directory1_File3.txt    * |- 00000000-0000-0001-0000-000000000012 (file)
     * |- /rootDirectory0                        * |- 00000000-0000-0001-0000-000000000007           - swapped
     *    |- rootDirectory0_Directory1_File2.txt *    |- 00000000-0000-0001-0000-000000000011 (file) - swapped
     *    |- rootDirectory0_File6.txt            *    |- 00000000-0000-0001-0000-000000000015 (file)
     *
     * P.S. Comment instead of javadoc is purely for keeping table formatting
     */
    @Test
    public void import_whenSameDirIdInTree_thenExistingMoved(TestInfo testInfo) {
        // Prepare
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        // Expect
        String rootDirectory0Id = "00000000-0000-0001-0000-000000000007";
        UUID rootDirectory0_File1Id = UUID.fromString("00000000-0000-0001-0000-000000000010");
        UUID rootDirectory0_File6Id = UUID.fromString("00000000-0000-0001-0000-000000000015");

        String rootDirectory0_Directory1Id = "00000000-0000-0001-0000-000000000008";
        UUID rootDirectory0_Directory1_File2Id = UUID.fromString("00000000-0000-0001-0000-000000000011");
        UUID rootDirectory0_Directory1_File3Id = UUID.fromString("00000000-0000-0001-0000-000000000012");

        int expectedDirectoryCount = testProjectConfiguration.get().getDirectories().size();
        int expectedFilesCount = testProjectConfiguration.get().getFiles().size();

        ExportImportDirectory RootDirectory0Expected = readExportDirectory(testPath.get(), rootDirectory0Id);
        ExportImportDirectory rootDirectory0_Directory1Expected = readExportDirectory(testPath.get(), rootDirectory0_Directory1Id);

        ProjectDirectory rootDirectory0BeforeImport = findDirById(rootDirectory0Id);
        ProjectDirectory rootDirectory0_Directory1BeforeImport = findDirById(rootDirectory0_Directory1Id);

        int expectedDirectorySizeForrootDirectory0 = rootDirectory0BeforeImport.getDirectories().size() - 1;
        int expectedDirectorySizeForrootDirectory0_Directory1BeforeImport = 1 + rootDirectory0_Directory1BeforeImport.getDirectories().size();

        int expectedFilesSizeForrootDirectory0 = rootDirectory0BeforeImport.getFiles().size();
        int expectedFilesSizeForrootDirectory0_Directory1 = rootDirectory0_Directory1BeforeImport.getFiles().size();

        // Then Call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());
        ProjectDirectory rootDirectory0 = findDirById(rootDirectory0Id);
        ProjectDirectory rootDirectory0_Directory1 = findDirById(rootDirectory0_Directory1Id);

        // Assert size same
        assertEquals(expectedDirectoryCount, testProjectConfiguration.get().getDirectories().size());
        assertEquals(expectedFilesCount, testProjectConfiguration.get().getFiles().size());

        // Assert new parent directory set
        assertSame(rootDirectory0_Directory1, rootDirectory0.getParentDirectory());
        assertNull(rootDirectory0_Directory1.getParentDirectory());

        // Assert child directories sizes correct
        assertEquals(expectedDirectorySizeForrootDirectory0, rootDirectory0.getDirectories().size());
        assertEquals(expectedDirectorySizeForrootDirectory0_Directory1BeforeImport, rootDirectory0_Directory1.getDirectories().size());

        assertTrue(rootDirectory0_Directory1.getDirectories().stream()
                .anyMatch(d -> d == rootDirectory0)
        );
        assertTrue(rootDirectory0.getDirectories().stream()
                .noneMatch(d -> d == rootDirectory0_Directory1)
        );

        // Assert child files size is same
        assertEquals(expectedFilesSizeForrootDirectory0, rootDirectory0.getFiles().size());
        assertEquals(expectedFilesSizeForrootDirectory0_Directory1, rootDirectory0_Directory1.getFiles().size());

        // Assert child files not lost and remain in directories
        ProjectFile rootDirectory0_File6 =
                ExportImportTestUtils.findFileById(rootDirectory0_File6Id, rootDirectory0.getFiles());
        assertEquals(rootDirectory0_File6.getDirectory(), rootDirectory0);

        ProjectFile rootDirectory0_Directory1_File3 =
                ExportImportTestUtils.findFileById(rootDirectory0_Directory1_File3Id, rootDirectory0_Directory1.getFiles());
        assertEquals(rootDirectory0_Directory1_File3.getDirectory(), rootDirectory0_Directory1);

        // Assert files swapped between directories
        ProjectFile rootDirectory0_File1 =
                ExportImportTestUtils.findFileById(rootDirectory0_File1Id, rootDirectory0_Directory1.getFiles());
        assertEquals(rootDirectory0_File1.getDirectory(), rootDirectory0_Directory1);
        assertTrue(rootDirectory0.getFiles().stream()
                .noneMatch(f -> f.getId().equals(rootDirectory0_File1Id)));

        ProjectFile rootDirectory0_Directory1_File2 =
                ExportImportTestUtils.findFileById(rootDirectory0_Directory1_File2Id, rootDirectory0.getFiles());
        assertEquals(rootDirectory0_Directory1_File2.getDirectory(), rootDirectory0);
        assertTrue(rootDirectory0_Directory1.getFiles().stream()
                .noneMatch(f -> f.getId().equals(rootDirectory0_Directory1_File2Id)));

        // Assert directory name
        assertEquals(RootDirectory0Expected.getName(), rootDirectory0.getName());
        assertEquals(rootDirectory0_Directory1Expected.getName(), rootDirectory0_Directory1.getName());

        verify(gridFsService.get(), times(2)).uploadFile(any(FileMetaData.class), any(File.class));
    }

    @Test
    public void import_whenSameFileIdInSameDirectory_thenExistingUsed(TestInfo testInfo) {
        // Prepare
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        // Expect
        int expectedFileSize = testProjectConfiguration.get().getFiles().size();
        String rootFileId = "00000000-0000-0001-0000-00000000000f";
        String childFileId = "00000000-0000-0001-0000-000000000010";
        ExportImportFile expectedRootFile = readExportFile(testPath.get(), rootFileId);
        ExportImportFile expectedChildFile = readExportFile(testPath.get(), childFileId);
        ProjectFile targetRootFileBeforeImport = findFileById(rootFileId);
        ProjectFile targetChildFileBeforeImport = findFileById(childFileId);

        // Then Call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());
        ProjectFile targetRootFileAfterImport = findFileById(rootFileId);
        ProjectFile targetChildFileAfterImport = findFileById(childFileId);

        // Assert
        assertEquals(expectedFileSize, testProjectConfiguration.get().getFiles().size());
        assertSame(targetRootFileBeforeImport, targetRootFileAfterImport,
                "File should not be recreated, only its fields should change"
        );
        assertSame(targetChildFileBeforeImport, targetChildFileAfterImport,
                "File should not be recreated, only its fields should change"
        );
        // Assert root file fields changed
        assertEquals(expectedRootFile.getName(), targetRootFileAfterImport.getName());
        assertNotEquals(expectedRootFile.getGridFsId(), targetRootFileAfterImport.getGridFsObjectId());
        // Assert child file fields changed
        assertEquals(expectedChildFile.getName(), targetChildFileAfterImport.getName());
        assertNotEquals(expectedChildFile.getGridFsId(), targetChildFileAfterImport.getGridFsObjectId());

        verify(gridFsService.get(), times(2)).uploadFile(any(FileMetaData.class), any(File.class));
    }

    @Test
    public void import_whenSameDirIdInSameDirectory_thenExistingUsed(TestInfo testInfo) {
        // Prepare
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        // Expect
        int expectedDirectoryCount = testProjectConfiguration.get().getDirectories().size();
        String rootId = "00000000-0000-0001-0000-000000000007";
        String childId = "00000000-0000-0001-0000-000000000008";
        ExportImportDirectory expectedRootDir = readExportDirectory(testPath.get(), rootId);
        ExportImportDirectory expectedChildDir = readExportDirectory(testPath.get(), childId);
        ProjectDirectory targetRootDirBeforeImport = findDirById(rootId);
        ProjectDirectory targetChildFolderBeforeImport = findDirById(childId);

        // Then Call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());
        ProjectDirectory targetRootDir = findDirById(rootId);
        ProjectDirectory targetChildFolder = findDirById(childId);

        // Assert size
        assertEquals(expectedDirectoryCount, testProjectConfiguration.get().getDirectories().size());
        // Assert dir not recreated
        assertSame(targetRootDirBeforeImport, targetRootDir,
                "Directory should not be recreated, only its fields should change"
        );
        assertSame(targetChildFolderBeforeImport, targetChildFolder,
                "Directory should not be recreated, only its fields should change"
        );
        // Assert directory name
        assertEquals(expectedRootDir.getName(), targetRootDir.getName());
        assertEquals(expectedChildDir.getName(), targetChildFolder.getName());
    }

    @Test
    public void import_whenSameFileNameInSameDirectory_thenRenameExisting(TestInfo testInfo) {
        // Prepare
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        // Expect
        String rootFileId = "00000000-1100-0001-0000-00000000000f";
        String childFileId = "00000000-1100-0001-0000-000000000010";
        // Two files would be created with the updated names
        int expectedFileSize = 2 + testProjectConfiguration.get().getFiles().size();
        ExportImportFile expectedRootFile = readExportFile(testPath.get(), rootFileId);
        ExportImportFile expectedChildFile = readExportFile(testPath.get(), childFileId);

        ProjectFile existingRootFileWithConflictingName = findFileByName(expectedRootFile.getName());
        ProjectFile existingChildFileWithConflictingName = findFileByName(expectedChildFile.getName());

        // Then Call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());

        // Assert size
        assertEquals(expectedFileSize, testProjectConfiguration.get().getFiles().size());
        // Assert root file fields changed
        validateFilenameChanged(existingRootFileWithConflictingName.getName(), expectedRootFile.getName());
        validateFilenameChanged(existingChildFileWithConflictingName.getName(), expectedChildFile.getName());
    }

    @Test
    public void import_whenSameDirNameInSameDirectory_thenRenameExisting(TestInfo testInfo) {
        // Prepare
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        // Expect
        String rootId = "00000000-1100-0001-0000-000000000007";
        String childId = "00000000-1100-0001-0000-000000000008";
        // Two Directories would be created with the updated names
        int expectedDirectoryCount = 2 + testProjectConfiguration.get().getDirectories().size();
        ExportImportDirectory expectedRootDir = readExportDirectory(testPath.get(), rootId);
        ExportImportDirectory expectedChildDir = readExportDirectory(testPath.get(), childId);

        ProjectDirectory existingRootDirWithConflictingName = findDirByName(expectedRootDir.getName());
        ProjectDirectory existingChildDirWithConflictingName = findDirByName(expectedChildDir.getName());

        // Then Call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());

        // Assert size
        assertEquals(expectedDirectoryCount, testProjectConfiguration.get().getDirectories().size());
        // Assert directory name changed
        validateFilenameChanged(existingRootDirWithConflictingName.getName(), expectedRootDir.getName());
        validateFilenameChanged(existingChildDirWithConflictingName.getName(), expectedChildDir.getName());
    }

    @Test
    public void import_whenFileParentDirectoryNotFound_thenRaiseError(TestInfo testInfo) {
        // Prepare
        copyTestDataToWorkingDirectory(testInfo);
        // Then Call import
        assertThrows(
                MiaImportParentDirectoryNotFound.class,
                () -> atpMiaImportExecutor.importData(exportImportData.get(), path.get())
        );
    }

    @Test
    public void import_whenDirectoriesParentDirectoryNotFound_thenRaiseError(TestInfo testInfo) {
        // Prepare
        copyTestDataToWorkingDirectory(testInfo);
        // Then Call import
        assertThrows(
                MiaImportParentDirectoryNotFound.class,
                () -> atpMiaImportExecutor.importData(exportImportData.get(), path.get())
        );
    }

    @Test
    public void importValidate_whenFileParentNotFound_thenError(TestInfo testInfo) {
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        String expectedMessage = new MiaImportParentDirectoryNotFound("file", "rootDirectory0_File1.txt").getMessage();

        ValidationResult validationResult = atpMiaImportExecutor.validateData(exportImportData.get(), path.get());

        assertFalse(validationResult.isValid());
        assertFalse(validationResult.getDetails().isEmpty());
        assertEquals(expectedMessage, validationResult.getDetails().get(0).getMessage());
    }

    @Test
    public void importValidate_whenDirectoryParentNotFound_thenError(TestInfo testInfo) {
        testPath.set(copyTestDataToWorkingDirectory(testInfo));
        String expectedMessage = new MiaImportParentDirectoryNotFound("directory", "/rootDirectory0/Directory1").getMessage();

        ValidationResult validationResult = atpMiaImportExecutor.validateData(exportImportData.get(), path.get());

        assertFalse(validationResult.isValid());
        assertFalse(validationResult.getDetails().isEmpty());
        assertEquals(expectedMessage, validationResult.getDetails().get(0).getMessage());
    }

    @Test
    public void importValidate_whenPositive_thenOk(TestInfo testInfo) {
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), new HashMap<>(), null, null, false));
        testPath.set(copyTestDataToWorkingDirectory(testInfo));

        ValidationResult validationResult = atpMiaImportExecutor.validateData(exportImportData.get(), path.get());
        assertTrue(validationResult.isValid());
    }

    /*
     * This test validates what if directory or file parent id changed in replacementMap due to new project flag
     * and as result new directory couldn't been found.
     *
     * Import archive with names:                * Import archive with ids:
     * /rootDirectory0                           * 00000000-0000-0001-0000-000000000007
     * |- rootDirectory0_File1.txt               * |- 00000000-0000-0001-0000-000000000010 (file)
     * |- /rootDirectory0/Directory1             * |- 00000000-0000-0001-0000-000000000008
     *    |- rootDirectory0_Directory1_File2.txt *    |- 00000000-0000-0001-0000-000000000011 (file)
     * ------------------------------------------* --------------------------------------------------
     * Existing structure with names:            * Existing structure with ids:
     * /rootDirectory0                           * 00000000-0000-0001-0000-000000000007
     * |- rootDirectory0_File1.txt               * |- 00000000-0000-0001-0000-000000000010 (file)
     * ------------------------------------------* --------------------------------------------------
     * After import with names:                  * After import with ids:
     * /rootDirectory0                           * 00000000-0000-0001-0000-000000000008    (kept)
     * |- rootDirectory0_File1.txt               * |- 00000000-0000-0001-0000-000000000010 (kept)
     * |- /rootDirectory0/Directory1             * |- Random UUID                          - despite random id parent found
     *    |- rootDirectory0_Directory1_File2.txt *    |- Random UUID                       - despite random id parent found
     *
     */
    @Test
    public void importToAnotherProject_whenIdMissing_thenReversedReplaceMapHandledIssue(TestInfo testInfo) {
        testPath.set(copyTestDataToWorkingDirectory(testInfo));

        //Prepare like in base test class
        testProjectConfiguration.get().setDirectories(new ArrayList<ProjectDirectory>() {{
            add(directoryConfigurations.get().get("/rootDirectory0"));
        }});
        testProjectConfiguration.get().setFiles(new ArrayList<ProjectFile>() {{
            add(fileConfigurations.get().get("rootDirectory0_File1.txt"));
        }});

        //Set interProjectImport to true
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), new HashMap<>(), null, null, false));

        // need to generate replacementMap
        ValidationResult validationResult = atpMiaImportExecutor.validateData(exportImportData.get(), path.get());
        assertEquals(4, validationResult.getReplacementMap().size());

        //Set interProjectImport to true again
        exportImportData.set(new ExportImportData(projectId.get(), exportScope.get(), ExportFormat.ATP, false,
                true, projectId.get(), validationResult.getReplacementMap(), null, null, false));
        // Then Call import
        atpMiaImportExecutor.importData(exportImportData.get(), path.get());

        // Assert UUID remain for existing directory
        ProjectDirectory actualDirectory = findDirById(directoryConfigurations.get().get("/rootDirectory0").getId().toString());
        ExportImportDirectory expectedDirectory = readExportDirectory(testPath.get(), actualDirectory.getSourceId().toString());
        assertEquals(expectedDirectory.getId(), actualDirectory.getId());

        // Assert UUID remain for existing file
        ProjectFile actualFile = findFileById(fileConfigurations.get().get("rootDirectory0_File1.txt").getId().toString());
        ExportImportFile expectedFile = readExportFile(testPath.get(), actualFile.getSourceId().toString());
        assertEquals(expectedFile.getId(), actualFile.getId());

        // Assert UUID regenerated
        actualDirectory = findDirByName("/rootDirectory0/Directory1");
        expectedDirectory = readExportDirectory(testPath.get(), actualDirectory.getSourceId().toString());
        assertNotEquals(expectedDirectory.getId(), actualDirectory.getId());

        // Assert UUID regenerated
        actualFile = findFileByName("rootDirectory0_Directory1_File2.txt");
        expectedFile = readExportFile(testPath.get(), actualFile.getSourceId().toString());
        assertNotEquals(expectedFile.getId(), actualFile.getId());
    }

    public Path copyTestDataToWorkingDirectory(TestInfo testInfo) {
        String testFolder = "src/test/resources/ei/import/CornerCases/";
        Path testPath = Paths.get(testFolder).resolve(testInfo.getTestMethod().get().getName());
        try {
            FileUtils.copyDirectory(testPath.toFile(), path.get().toFile());
            return testPath;
        } catch (IOException e) {
            fail("Can't copy directory " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private ProjectFile findFileByName(String name) {
        return ExportImportTestUtils.findFileByName(name, testProjectConfiguration.get().getFiles());
    }

    private ProjectFile findFileById(String fileId) {
        return ExportImportTestUtils.findFileById(UUID.fromString(fileId), testProjectConfiguration.get().getFiles());
    }

    private static ProjectDirectory findDirByName(String name) {
        return ExportImportTestUtils.findDirByName(name, testProjectConfiguration.get().getDirectories());
    }

    private ProjectDirectory findDirById(String dirId) {
        return ExportImportTestUtils.findDirById(dirId, testProjectConfiguration.get().getDirectories());
    }
}
