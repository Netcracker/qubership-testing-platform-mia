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

package org.qubership.atp.mia.service.file;

import static org.qubership.atp.mia.model.Constants.ERROR_SSH_DOWNLOAD_FAILED;
import static org.qubership.atp.mia.model.Constants.MIA_ROOT_DIRECTORY;
import static org.qubership.atp.mia.model.file.FileMetaData.PROJECT_FOLDER;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.util.Strings;
import org.bson.types.ObjectId;
import org.qubership.atp.mia.exceptions.gridfs.GridFsFileNotFoundException;
import org.qubership.atp.mia.exceptions.gridfs.GridFsGetFileFromDbException;
import org.qubership.atp.mia.exceptions.gridfs.GridfsSaveLogToDbException;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.repo.gridfs.GridFsRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.stereotype.Service;

import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridFsService {

    private final GridFsRepository fsRepository;
    private final MiaContext miaContext;

    /**
     * Clean GridFs from old files.
     */
    public void cleanDb() {
        fsRepository.cleanDb();
    }

    /**
     * Clean mongo ProjectFiles.
     *
     * @param projectId  projectId
     * @param excludeIds excludeIds
     */
    public void cleanMongoProjectFiles(UUID projectId, List<String> excludeIds) {
        log.info("Cleaning project files in GridFs for {} with exclusion {}", projectId, excludeIds);
        fsRepository.findAndDeleteObsoleteProjectFiles(projectId, excludeIds);
    }

    /**
     * Clean folder with logs.
     *
     * @param cleanInterval clean interval
     */
    public void cleanLogs(Integer cleanInterval) {
        Date oldestAllowedFileDate = DateUtils.addHours(new Date(), -cleanInterval); //minus hours from current dateTime
        File targetDir = PROJECT_FOLDER.toFile();
        if (targetDir.exists()) {
            Iterator<File> filesToDelete = FileUtils.iterateFiles(targetDir, new AgeFileFilter(oldestAllowedFileDate),
                    TrueFileFilter.INSTANCE);
            while (filesToDelete.hasNext()) {
                FileUtils.deleteQuietly(filesToDelete.next());
            }
            deleteEmptyDir(targetDir);
        }
    }

    /**
     * Upload file to GridFs.
     *
     * @param projectFile project file
     */
    public void deleteProjectFile(ProjectFile projectFile) {
        log.info("Delete file {} from GridFs", projectFile.getName());
        fsRepository.removeFile(new ObjectId(projectFile.getGridFsObjectId()));
    }

    /**
     * Check if file exist.
     *
     * @param projectFile {@link ProjectFile}.
     */
    public boolean existFile(ProjectFile projectFile) {
        return fsRepository.exist(projectFile);
    }

    /**
     * Returns collections size in selected bucket.
     *
     * @return size in megabytes.
     */
    public String getCollectionsSize() {
        return "Overall collection size: " + fsRepository.getCollectionsSize(null) + ";\n" + "Project collection size: "
                + fsRepository.getCollectionsSize(miaContext.getProjectId());
    }

    /**
     * Rename file.
     */
    public void rename(ProjectFile projectFile) {
        log.info("Update path in GridFs for file '{}' with ID '{}' in directory '{}'",
                projectFile.getName(), projectFile.getId(),
                projectFile.getDirectory() == null ? MIA_ROOT_DIRECTORY : projectFile.getDirectory().getName());
        fsRepository.rename(projectFile);
    }

    /**
     * Restore file from GridFs.
     *
     * @param file file.
     */
    public void restoreFile(File file) {
        FileMetaData fileMetaData = FileMetaData.define(miaContext.getProjectId(), file.toString());
        log.info("Restore file: {}.", fileMetaData.getFileName());
        try (InputStream is = fsRepository.get(fileMetaData)
                .orElseThrow(() -> new FileNotFoundException(file.toString()))) {
            file.getParentFile().mkdirs();
            FileUtils.copyInputStreamToFile(is, file);
        } catch (IOException e) {
            throw new GridFsGetFileFromDbException(file.toString(), e);
        }
    }

    public Optional<GridFSFile> getFile(String gridFsObjectId) {
        return fsRepository.getById(gridFsObjectId);
    }

    /**
     * Save all file of response in GridFs.
     *
     * @param response execution response.
     */
    public void saveLogResponseAfterExecution(ExecutionResponse response) {
        log.info("Save log response after execution to GridFs");
        if (response.getPrerequisites() != null) {
            log.info("Save prerequisites");
            response.getPrerequisites().forEach(p -> saveCommandOutput(p.getCommandOutputs()));
        }
        if (response.getCommandResponse() != null) {
            log.info("Save command");
            if (response.getCommandResponse().getCommandOutputs() != null) {
                saveCommandOutput(response.getCommandResponse().getCommandOutputs());
            } else if (response.getCommandResponse().getSqlResponse() != null) {
                String fileName = response.getCommandResponse().getSqlResponse().getInternalPathToFile();
                if (Strings.isNotEmpty(fileName)) {
                    uploadFile(FileMetaData.log(miaContext.getProjectId(), fileName), fileName);
                }
            } else {
                log.warn("No Command output file is found");
            }
        }
        if (response.getValidations() != null) {
            log.info("Save validations");
            response.getValidations().forEach(v -> {
                String fileName = v.getInternalPathToFile();
                if (Strings.isNotEmpty(fileName)) {
                    uploadFile(FileMetaData.log(miaContext.getProjectId(), fileName), fileName);
                } else {
                    log.error("Can't upload to GridFs: internal path not defined for validation {}", v);
                }
            });
        }
    }

    /**
     * Upload file to GridFs.
     *
     * @param fileMetaData fileMetaData
     * @return ObjectId of file in GridFs.
     */
    public ObjectId uploadFile(FileMetaData fileMetaData, String fullPathToFile) {
        //return uploadFile(fileMetaData, new File(fullPathToFile));
        return uploadFile(fileMetaData, Paths.get(fullPathToFile).toAbsolutePath().normalize().toFile());
    }

    /**
     * Upload file to GridFs.
     *
     * @param fileMetaData fileMetaData
     * @return ObjectId of file in GridFs.
     */
    public ObjectId uploadFile(FileMetaData fileMetaData, File fullPathToFile) {
        log.info("Upload file {} to GridFs", fileMetaData);
        /*Path baseDir = miaContext.getProjectFilePath().toAbsolutePath().normalize();
        Path targetPath = fullPathToFile.toPath().toAbsolutePath().normalize();
        if (!targetPath.startsWith(baseDir)) {
            throw new SecurityException("Invalid file path: path traversal attempt detected - " + fullPathToFile);
        }*/
        try (FileInputStream is = new FileInputStream(fullPathToFile)) {
            return fsRepository.save(fileMetaData, is);
        } catch (FileNotFoundException e) {
            throw new GridFsFileNotFoundException(fullPathToFile.toString(), e);
        } catch (IOException e) {
            throw new GridfsSaveLogToDbException(e);
        }
    }

    private void deleteEmptyDir(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteEmptyDir(file);
                }
            }
            directory.delete();
        }
    }

    private void saveCommandOutput(List<CommandOutput> outputs) {
        if (outputs != null) {
            for (CommandOutput output : outputs) {
                String fileName = output.getInternalPathToFile();
                if (output.getMarkedContent().stream()
                        .anyMatch(markedContent -> markedContent.getText().contains(ERROR_SSH_DOWNLOAD_FAILED))) {
                    log.error("Problem Occurred in process execution. Could not get file from server and Can't "
                            + "upload to GridFs. File path: {}", fileName);
                } else if (Strings.isNotEmpty(fileName)) {
                    if (fileName.contains(ProjectFileType.MIA_FILE_TYPE_LOG.toString())) {
                        uploadFile(FileMetaData.log(miaContext.getProjectId(), fileName), fileName);
                    } else if (fileName.contains(ProjectFileType.MIA_FILE_TYPE_UPLOAD.toString())) {
                        uploadFile(FileMetaData.upload(miaContext.getProjectId(), fileName), fileName);
                    }
                } else {
                    log.error("Can't upload to GridFs: internal path not defined for output {}", output);
                }
            }
        }
    }

    public byte[] getByteArrayFromGridFsFile(GridFSFile file) {
        return fsRepository.getFileContent(file);
    }
}
