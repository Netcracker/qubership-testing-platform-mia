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

package org.qubership.atp.mia.integration.sql;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.integration.configuration.BaseIntegrationTestConfiguration;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;

//@Disabled("Temporarily disabled for refactoring")
public class MongoConnectionTest extends BaseIntegrationTestConfiguration {

    private static File dest;

    @AfterEach
    public void after() {
        gridFsRepository.removeFile(new FileMetaData(miaContext.getProjectId(), dest.getName(), ProjectFileType.MIA_FILE_TYPE_LOG));
    }

    @BeforeEach
    public void before() throws IOException {
        File file = new File(mongoTest_path + mongoTest_filename);
        dest = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_LOG,
                miaContext.getFlowData().getSessionId()).resolve(mongoTest_filename).toFile();
        FileUtils.copyFile(file, dest);
        ExecutionResponse executionResponse = new ExecutionResponse();
        CommandResponse commandResponse =
                new CommandResponse(
                        new CommandOutput(dest.toString(), "/download/" + mongoTest_path, true, miaContext));
        executionResponse.setCommandResponse(commandResponse);
        gridFsService.saveLogResponseAfterExecution(executionResponse);
    }

    @Test
    public void checkCollectionSaveAndGet() throws IOException {
        UUID id = UUID.randomUUID();
        String expectedText = "predefine";
        FileMetaData meta = new FileMetaData(id, mongoTest_filename, ProjectFileType.MIA_FILE_TYPE_LOG);
        try (FileInputStream fs = new FileInputStream(mongoTest_path + mongoTest_filename)) {
            gridFsRepository.save(meta, fs);
            String actual = new BufferedReader(new InputStreamReader(gridFsRepository.get(meta).get())).readLine();
            Assert.assertEquals("expected text not saved in a file", expectedText, actual);
            gridFsRepository.removeFile(new FileMetaData(miaContext.getProjectId(), dest.getName(), ProjectFileType.MIA_FILE_TYPE_LOG));
        }
    }

    @Test
    public void restoreLog_whenNotInDB() {
        File file = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_LOG,
                miaContext.getFlowData().getSessionId()).resolve("not_exist_file_" + mongoTest_filename).toFile();
        try {
            miaFileService.getFile(file).exists();
            assertTrue(false);
        } catch (MiaException e) {
            assertTrue(e.getMessage().contains("MIA-2101"));
        }
    }

    @Test
    public void restoreLog_whenRemove() throws IOException {
        FileUtils.forceDelete(dest);
        boolean result = miaFileService.getFile(dest).exists();
        assertTrue(result);
        assertTrue(dest.exists());
        FileUtils.forceDelete(dest);
    }

    @Test
    public void smokeMongoAvailable() {
        assertTrue("Error, mock used instead of working repository!", gridFsRepository.isEnable());
    }
}
