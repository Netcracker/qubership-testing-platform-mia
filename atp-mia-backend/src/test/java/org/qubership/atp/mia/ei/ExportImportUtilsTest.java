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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.qubership.atp.mia.ei.component.ExportImportUtils.createMapIdToExistingDirectory;
import static org.qubership.atp.mia.ei.component.ExportImportUtils.createMapIdToExistingFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.ei.component.ExportImportUtils;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;

public class ExportImportUtilsTest {

    @Test
    public void testCreateMapIdToExistingDirectory_whenDuplicateId_allDirectoryStored() {
        List<ProjectDirectory> directories = new ArrayList<ProjectDirectory>() {{
            add(new ProjectDirectory().toBuilder()
                    .name("directory1")
                    .id(new UUID(0, 1))
                    .sourceId(new UUID(0, 2))
                    .build());
            add(new ProjectDirectory().toBuilder()
                    .name("directory2")
                    .id(new UUID(0, 2))
                    .sourceId(new UUID(0, 5))
                    .build());
            add(new ProjectDirectory().toBuilder()
                    .name("directory5")
                    .id(new UUID(0, 5))
                    .sourceId(new UUID(0, 5))
                    .build());
        }};

        ProjectConfiguration projectConfiguration = new ProjectConfiguration();
        projectConfiguration.setDirectories(directories);
        Map<UUID, ProjectDirectory> map = createMapIdToExistingDirectory(projectConfiguration);
        for (ProjectDirectory directory : directories) {
            assertSame(directory, map.get(directory.getId()));
        }
    }

    @Test
    public void testCreateMapIdToExistingFile_whenDuplicateId_allFilesStored() {
        List<ProjectFile> files = new ArrayList<ProjectFile>() {{
            add(new ProjectFile().toBuilder()
                    .name("file1")
                    .id(new UUID(0, 1))
                    .sourceId(new UUID(0, 2))
                    .build());
            add(new ProjectFile().toBuilder()
                    .name("file2")
                    .id(new UUID(0, 2))
                    .sourceId(new UUID(0, 5))
                    .build());
            add(new ProjectFile().toBuilder()
                    .name("file5")
                    .id(new UUID(0, 5))
                    .sourceId(new UUID(0, 5))
                    .build());
        }};

        ProjectConfiguration projectConfiguration = new ProjectConfiguration();
        projectConfiguration.setFiles(files);
        Map<UUID, ProjectFile> map = createMapIdToExistingFile(projectConfiguration);
        for (ProjectFile file : files) {
            assertSame(file, map.get(file.getId()));
        }
    }

    @Test
    public void testReverseMap() {
        assertTrue(ExportImportUtils.reverseMap(new HashMap<>()).isEmpty());
    }
}
