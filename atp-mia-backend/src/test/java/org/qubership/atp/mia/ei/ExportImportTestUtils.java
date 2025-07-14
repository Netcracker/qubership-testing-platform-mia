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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qubership.atp.mia.ei.component.ExportImportUtils.getFolderId;
import static org.qubership.atp.mia.ei.component.ImportLoader.EI_CONFLICT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.mia.model.ei.ExportImportDirectory;
import org.qubership.atp.mia.model.ei.ExportImportEntities;
import org.qubership.atp.mia.model.ei.ExportImportFile;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class ExportImportTestUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ExportImportFile readExportFile(Path path, String id) {
        return readEntity(path, ExportImportEntities.MIA_FILES, ExportImportFile.class, id);
    }

    public static ExportImportDirectory readExportDirectory(Path path, String id) {
        return readEntity(path, ExportImportEntities.MIA_FILES, ExportImportDirectory.class, id);
    }

    public static <T> T readEntity(Path path, ExportImportEntities entity, Class<T> clazz) {
        return readEntity(getFirstFileFromPath(path, entity, clazz), clazz);
    }

    public static <T> T readEntity(Path path, ExportImportEntities entity, Class<T> clazz, String id) {
        return readEntity(resolveFilePath(path, entity, clazz, UUID.fromString(id)), clazz);
    }

    public static <T> T readEntity(File file, Class<T> clazz) {
        try {
            return mapper.readValue(file, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File[] getFiles(File file) {
        if (nonNull(file) && nonNull(file.listFiles())) {
            assertTrue(file.listFiles().length > 0);
            return file.listFiles();
        }
        throw new RuntimeException("File not found " + file.getPath());
    }

    public static File resolveFilePath(Path path, ExportImportEntities entity, Class<?> clazz, UUID fileId) {
        return resolvePath(path, entity.getValue(), clazz.getSimpleName(), fileId + ".json").toFile();
    }

    public static File getFirstFileFromPath(Path path, ExportImportEntities entity, Class<?> clazz) {
        return getFiles(resolvePath(path, entity.getValue(), clazz.getSimpleName()).toFile())[0];
    }

    public static Path resolvePath(Path path, String... paths) {
        for (String resolvePath : paths) {
            path = path.resolve(resolvePath);
        }
        return path;
    }

    public static ProjectFile findFileByName(String name, List<ProjectFile> files) {
        Optional<ProjectFile> projectFile = files.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
        assertTrue(projectFile.isPresent(), "File not found: " + name);
        return projectFile.get();
    }

    public static ProjectFile findFileById(UUID fileId, List<ProjectFile> files) {
        Optional<ProjectFile> projectFile = files.stream()
                .filter(f -> f.getId().equals(fileId)
                        || (f.getSourceId() != null && f.getSourceId().equals(fileId)))
                .findFirst();
        assertTrue(projectFile.isPresent(), "File not found: " + fileId);
        return projectFile.get();
    }

    public static ProjectDirectory findDirByName(String name, List<ProjectDirectory> directories) {
        Optional<ProjectDirectory> projectDir = directories.stream()
                .filter(f -> f.getName().equals(name))
                .findFirst();
        assertTrue(projectDir.isPresent(), "Directory not found: " + name);
        return projectDir.get();
    }

    public static ProjectDirectory findDirById(String dirId, List<ProjectDirectory> directories) {
        Optional<ProjectDirectory> projectDir = directories.stream()
                .filter(f -> f.getId().equals(UUID.fromString(dirId))
                        || (f.getSourceId() != null && f.getSourceId().equals(UUID.fromString(dirId))))
                .findFirst();
        assertTrue(projectDir.isPresent(), "Directory not found: " + dirId);
        return projectDir.get();
    }

    public static void validateFilenameChanged(String generatedName, String expectedFilename) {
        assertTrue(generatedName.startsWith(expectedFilename + EI_CONFLICT),
                "new filename should start with the former name"
        );
        // Check that the part after "_ei_" is a valid UUID (match the UUID pattern)
        String timestamp = generatedName.substring(expectedFilename.length() + EI_CONFLICT.length());
        assertDoesNotThrow(() -> Instant.ofEpochMilli(Long.parseLong(timestamp)));
    }

    public static void compareProjectAndEiDirectories(UUID id, Path pathToExpected, List<ProjectDirectory> directories) {
        ProjectDirectory actualDirectory = findDirById(id.toString(), directories);
        ExportImportDirectory expectedDirectory = readExportDirectory(pathToExpected, id.toString());
        compareProjectAndEiDirectories(expectedDirectory, actualDirectory);
    }

    public static void compareProjectAndEiFiles(UUID id, Path pathToExpected, List<ProjectFile> files) {
        ProjectFile actualFile = findFileById(id, files);
        ExportImportFile expectedFile = readExportFile(pathToExpected, id.toString());
        compareProjectAndEiFiles(expectedFile, actualFile);
    }

    public static void compareProjectAndEiFiles(ExportImportFile expectedFile, ProjectFile actualFile) {
        //assertEquals(expectedFile.getId(), actualFile.getId());
        assertEquals(expectedFile.getName(), actualFile.getName());
        assertEquals(expectedFile.getDirectory(), getFolderId(actualFile.getDirectory()));
    }

    public static void compareProjectAndEiDirectories(ExportImportDirectory expectedDirectory, ProjectDirectory actualDirectory) {
        //assertEquals(expectedDirectory.getId(), actualDirectory.getId());
        assertEquals(expectedDirectory.getName(), actualDirectory.getName());
        assertEquals(expectedDirectory.getParentDirectory(), getFolderId(actualDirectory.getParentDirectory()));
    }

    public static void compareProjectAndEiDirectoriesIdDiffers(String name, Path pathToExpected, List<ProjectDirectory> directories) {
        ProjectDirectory actualDirectory = findDirByName(name, directories);
        ExportImportDirectory expectedDirectory = readExportDirectory(pathToExpected, actualDirectory.getSourceId().toString());
        assertNotEquals(expectedDirectory.getId(), actualDirectory.getId());
        if (actualDirectory.getParentDirectory() != null) {
            assertEquals(expectedDirectory.getParentDirectory(), actualDirectory.getParentDirectory().getSourceId());
        } else {
            assertNull(expectedDirectory.getParentDirectory());
        }
    }

    public static void compareProjectAndEiFilesIdDiffers(String name, Path pathToExpected, List<ProjectFile> files) {
        ProjectFile actualFile = findFileByName(name, files);
        ExportImportFile expectedFile = readExportFile(pathToExpected, actualFile.getSourceId().toString());
        assertNotEquals(expectedFile.getId(), actualFile.getId());
        if (actualFile.getDirectory() != null) {
            assertEquals(expectedFile.getDirectory(), actualFile.getDirectory().getSourceId());
        } else {
            assertNull(expectedFile.getDirectory());
        }
    }
}
