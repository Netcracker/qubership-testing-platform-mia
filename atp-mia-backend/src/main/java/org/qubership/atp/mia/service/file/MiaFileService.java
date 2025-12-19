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

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.bson.types.ObjectId;
import org.qubership.atp.mia.exceptions.fileservice.FileEmptyException;
import org.qubership.atp.mia.exceptions.fileservice.FileTransferFailException;
import org.qubership.atp.mia.exceptions.fileservice.ReadFailFileNotFoundException;
import org.qubership.atp.mia.exceptions.fileservice.WrongFilePathException;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.utils.dos2unix.Dos2UnixFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mongodb.MongoGridFSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiaFileService {

    private final GridFsService gridFsService;
    private final MiaContext miaContext;
    private final ProjectConfigurationService projectConfigurationService;
    @Value("${dos2unix.path:/usr/bin/dos2unix}")
    String dos2UnixCustomPath;

    /**
     * Download file. Firstly attempts to download it locally,
     * if it's not present then secondly it attempts to download from Mongo DB (if activated).
     *
     * @param fileName        fineName
     * @param sessionId       sessionId
     * @param projectFileType projectFileType
     * @param servletContext  servletContext
     * @return {@link ResponseEntity}
     */
    public ResponseEntity<Resource> downloadFile(UUID projectId, ProjectFileType projectFileType,
                                                 UUID sessionId, String fileName, ServletContext servletContext) {
        File fullFile = getFile(projectConfigurationService.getProjectPathWithType(projectId, projectFileType,
                sessionId).resolve(fileName).toFile());
        try {
            return ResponseEntity.ok().header(CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                    .header(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION)
                    .contentType(getMediaTypeForFileName(servletContext, fileName))
                    .body(new InputStreamResource(Files.newInputStream(fullFile.toPath())));
        } catch (IOException e) {
            throw new ReadFailFileNotFoundException(fullFile.toString());
        }
    }

    /**
     * Get file.
     *
     * @param file {@link File}
     * @return {@link File}
     */
    public File getFile(File file) {
        boolean needRestoreFile;
        try {
            needRestoreFile = !file.exists()
                    || LocalDateTime.ofInstant(
                            Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toInstant(),
                            ZoneId.systemDefault())
                    .isBefore(miaContext.getConfig().getLastLoadedWhen());
        } catch (IOException e) {
            needRestoreFile = true;
        }
        if (needRestoreFile) {
            gridFsService.restoreFile(file);
        }
        if (!file.exists()) {
            throw new ReadFailFileNotFoundException(file.toString());
        }
        return file;
    }

    /**
     * Get file.
     *
     * @param file file
     * @return {@link File}
     */
    public File getFile(String file) {
        if (file.contains("\\.\\.")) {
            throw new WrongFilePathException(file);
        }
        Path filePathToGet;
        String ethalonFilesPath = miaContext.getConfig().getCommonConfiguration().getEthalonFilesPath();
        if (file.contains(ethalonFilesPath)) {
            filePathToGet = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_PROJECT, null)
                    .resolve("." + file.split(ethalonFilesPath)[1]);
        } else if (file.contains(ProjectFileType.MIA_FILE_TYPE_PROJECT.toString())) {
            filePathToGet = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_PROJECT, null)
                    .resolve("." + file.split(ProjectFileType.MIA_FILE_TYPE_PROJECT.toString())[1]);
        } else if (file.contains(ProjectFileType.MIA_FILE_TYPE_UPLOAD.toString())) {
            filePathToGet = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_UPLOAD, null)
                    .resolve("." + file.split(ProjectFileType.MIA_FILE_TYPE_UPLOAD.toString())[1]);
        } else if (file.contains(ProjectFileType.MIA_FILE_TYPE_LOG.toString())) {
            filePathToGet = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_LOG, null)
                    .resolve("." + file.split(ProjectFileType.MIA_FILE_TYPE_LOG.toString())[1]);
        } else if (file.contains(ProjectFileType.MIA_FILE_TYPE_CONFIGURATION.toString())) {
            filePathToGet = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_CONFIGURATION, null)
                    .resolve("." + file.split(ProjectFileType.MIA_FILE_TYPE_CONFIGURATION.toString())[1]);
        } else {
            filePathToGet = miaContext.getProjectPathWithType(ProjectFileType.MIA_FILE_TYPE_PROJECT, null)
                    .resolve(file.startsWith("/") || file.startsWith("\\") ? "." + file : file);
        }
        return getFile(filePathToGet.normalize().toFile());
    }

    /**
     * Remove project file.
     *
     * @param projectFile projectFile
     */
    public void removeProjectFile(ProjectFile projectFile) {
        try {
            gridFsService.deleteProjectFile(projectFile);
        } catch (MongoGridFSException e) {
            log.warn("File not found in gridFs {}", projectFile.getName(), e);
        }
        try {
            Files.deleteIfExists(miaContext.getProjectFilePath().resolve(projectFile.getPathFile()));
        } catch (IOException e) {
            log.warn("Can't remove file ", e);
        }
    }

    /**
     * Remove project file.
     *
     * @param projectFile projectFile
     */
    public void renameProjectFile(ProjectFile projectFile, String newFileName) {
        File filePath = miaContext.getProjectFilePath().resolve(projectFile.getPathFile()).toFile();
        if (filePath.exists()) {
            filePath.delete();
        }
        if (gridFsService.existFile(projectFile)) {
            projectFile.setName(newFileName);
            gridFsService.rename(projectFile);
        }
    }

    /**
     * Save project file.
     *
     * @param file file
     * @return {@link File}
     */
    public ObjectId saveProjectFile(MultipartFile file, Path filePath) {
        if (file != null && !file.isEmpty()) {
            final Path baseDir = miaContext.getProjectFilePath().toAbsolutePath().normalize();
            final Path dest = baseDir.resolve(filePath).normalize();

            if (!dest.startsWith(baseDir)) {
                throw new SecurityException("Invalid path: Path traversal attempt detected: " + filePath);
            }
            try {
                if (!dest.toFile().getParentFile().exists() && !dest.toFile().getParentFile().mkdirs()) {
                    throw new IllegalStateException("Can't create path " + dest.toFile().getParentFile());
                }
                if (dest.toFile().exists() && !dest.toFile().delete()) {
                    throw new IllegalStateException("Can't delete old file " + dest);
                }
                file.transferTo(dest);
                return gridFsService.uploadFile(new FileMetaData(miaContext.getProjectId(), dest.toString(),
                        ProjectFileType.MIA_FILE_TYPE_PROJECT), dest.toFile());
            } catch (IOException | IllegalStateException e) {
                throw new FileTransferFailException(dest.toString(), e);
            }
        }
        throw new FileEmptyException(file);
    }

    /**
     * Save log file.
     *
     * @param fileMetaData fileMetaData
     * @param logFile      logFile
     * @return {@link ObjectId} of file in GridFs
     */
    public ObjectId saveLogFile(FileMetaData fileMetaData, File logFile) {
        try {
            return gridFsService.uploadFile(fileMetaData, logFile);
        } catch (IllegalStateException e) {
            throw new FileTransferFailException(logFile, e);
        }
    }

    /**
     * Upload file provided by FE to BE.
     *
     * @param file file
     * @return file path
     */
    public String uploadConfigurationFileOnBe(MultipartFile file) {
        return uploadFile(file, false, ProjectFileType.MIA_FILE_TYPE_CONFIGURATION);
    }

    /**
     * Upload file provided by FE to BE along with Dos2Unix conversion
     * when needDos2Unix is true and Os is Linux.
     *
     * @param file         file
     * @param needDos2Unix boolean
     * @return {@link File}
     */
    public String uploadFileOnBe(MultipartFile file, boolean needDos2Unix) {
        return uploadFile(file, needDos2Unix, ProjectFileType.MIA_FILE_TYPE_UPLOAD);
    }

    private MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
        MediaType mediaType;
        String mineType = servletContext.getMimeType(fileName);
        try {
            mediaType = MediaType.parseMediaType(mineType);
            log.debug("Got mediaType for fileName: {}, mediaType: {}.", fileName, mediaType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return mediaType;
    }

    private void runDos2Unix(Path dest) {
        String osName = System.getProperty("os.name");
        log.info("Run Dos2Unix on '{}' for file '{}'", osName, dest.toString());
        Dos2UnixFactory.getDos2Unix(osName, dos2UnixCustomPath).ifPresent(o -> o.runDos2Unix(dest));
    }

    private String uploadFile(MultipartFile file, boolean needDos2Unix, ProjectFileType fileType) {
        if (file != null && !file.isEmpty()) {
            Path fileName = Paths.get(file.getOriginalFilename());
            final File dest = miaContext.getProjectPathWithType(fileType).resolve(fileName.getFileName()).toFile();
            try (InputStream is = file.getInputStream()) {
                dest.getParentFile().mkdirs();
                if (dest.exists()) {
                    dest.delete();
                }
                Files.copy(is, dest.toPath());
                if (needDos2Unix) {
                    runDos2Unix(dest.toPath());
                }
                FileMetaData fileMetaData = new FileMetaData(miaContext.getProjectId(), dest.getPath(), fileType);
                gridFsService.uploadFile(fileMetaData, dest);
                return fileMetaData.getFileName();
            } catch (IOException e) {
                throw new FileTransferFailException(dest.getPath(), e);
            }
        }
        throw new FileEmptyException(file);
    }
}
