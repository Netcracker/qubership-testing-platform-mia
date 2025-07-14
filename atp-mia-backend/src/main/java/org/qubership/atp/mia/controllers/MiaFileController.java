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

package org.qubership.atp.mia.controllers;

import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.mia.controllers.api.MiaFileControllerApi;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.execution.TestDataService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;

/**
 * IMPORTANT!!! The first parameter in the request must be projectId, the second must be sessionId.
 */
@RestController
@RequiredArgsConstructor
public class MiaFileController implements MiaFileControllerApi {

    private final MiaFileService miaFileService;
    private final TestDataService testDataService;
    private final ServletContext servletContext;
    private final MiaContext miaContext;

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Download a logfile in project - {{#projectId}}")
    public ResponseEntity<Resource> downloadFile(UUID projectId, UUID sessionId, String logFile) {
        return miaFileService.downloadFile(projectId, ProjectFileType.MIA_FILE_TYPE_LOG, sessionId, logFile,
                servletContext);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "DownloadFileFromUploads called for project {{#projectId}}")
    public ResponseEntity<Resource> downloadFileFromUploads(UUID projectId, UUID sessionId, String logFile) {
        return miaFileService.downloadFile(projectId, ProjectFileType.MIA_FILE_TYPE_UPLOAD, sessionId, logFile,
                servletContext);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Download process execution output as Zip for process - \"{{#processName}}\" in "
            + "Project - {{#projectId}} ")
    public ResponseEntity<Resource> downloadOutputAsZip(UUID projectId, UUID sessionId, String processName,
                                                        List<String> filePaths) {
        if (filePaths != null) {
            filePaths.forEach(miaFileService::getFile);
        }
        String filename = miaContext.zipCommandOutputs(processName, filePaths).getName();
        return downloadFile(projectId, miaContext.getFlowData().getSessionId(), filename);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Upload a file. Project : {{#projectId}}")
    public ResponseEntity<String> handleFileUpload(UUID projectId, UUID sessionId, Boolean needDos2Unix,
                                                   MultipartFile file) {
        return ResponseEntity.ok(new Gson().toJson(miaFileService.uploadFileOnBe(file, needDos2Unix)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess(#projectId, \"EXECUTE\")")
    @AuditAction(auditAction = "Upload Test data matrix, in Project : {{#projectId}}")
    public ResponseEntity<String> handleTestDataFileUpload(UUID projectId, UUID sessionId, MultipartFile file) {
        return ResponseEntity.ok(new Gson().toJson(testDataService.uploadTestDataFileAndValidate(file)));
    }
}
