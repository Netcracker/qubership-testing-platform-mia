/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.exceptions.fileservice.ReadFailFileNotFoundException;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class MiaFileServiceTest extends BaseUnitTestConfiguration {

    @Test
    public void fileDownload() {
        MiaException thrown = Assertions.assertThrows(MiaException.class, () -> {
            miaFileService.get().downloadFile(projectId.get(), ProjectFileType.MIA_FILE_TYPE_LOG, UUID.randomUUID(), "console.log",
                    servletContext.get());
        });
        Assertions.assertInstanceOf(ReadFailFileNotFoundException.class, thrown);
        verify(gridFsService.get(), times(1)).restoreFile(any());
    }

    @Test
    public void downloadFileFromUploads() {
        MiaException thrown = Assertions.assertThrows(MiaException.class, () -> {
            miaFileService.get().downloadFile(projectId.get(), ProjectFileType.MIA_FILE_TYPE_UPLOAD, UUID.randomUUID(),
                    "RateMatrix.xls",
                    servletContext.get());
        });
        Assertions.assertInstanceOf(ReadFailFileNotFoundException.class, thrown);
        verify(gridFsService.get(), times(1)).restoreFile(any());
    }

    @Test
    public void handleFileUpload() throws IOException {
        File file = new File("src/test/resources/testData/fileToUpload.txt");
        FileInputStream input = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                file.getName(),
                "text/plain",
                input
        );
        Path fileName = Path.of(multipartFile.getOriginalFilename());
        final File dest = miaContext.get().getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_UPLOAD).resolve(fileName.getFileName()).toFile();
        miaFileService.get().uploadFileOnBe(multipartFile, false);
        Assertions.assertTrue(dest.exists());
        verify(gridFsService.get(), times(1)).uploadFile(any(), eq(dest));
    }

    @Test
    public void handleTestDataFileUpload() throws IOException {
        File file = new File("src/test/resources/testData/Rating_Matrix.xlsx");
        FileInputStream input = new FileInputStream(file);
        MultipartFile multipartFile = new MockMultipartFile(
                "file",
                file.getName(),
                "text/plain",
                input
        );
        Path fileName = Path.of(multipartFile.getOriginalFilename());
        final File dest = miaContext.get().getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_UPLOAD).resolve(fileName.getFileName()).toFile();
        Assertions.assertThrows(Exception.class, () -> {
            testDataService.get().uploadTestDataFileAndValidate(multipartFile);
        });
        Assertions.assertTrue(dest.exists());
        verify(gridFsService.get(), times(1)).uploadFile(any(), eq(dest));
    }
}
