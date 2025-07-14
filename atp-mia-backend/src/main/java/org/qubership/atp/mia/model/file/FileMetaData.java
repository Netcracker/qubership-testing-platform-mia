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

package org.qubership.atp.mia.model.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.qubership.atp.mia.exceptions.configuration.FileMetaDataNotFoundException;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@AllArgsConstructor
public class FileMetaData {

    public static final Path PROJECT_FOLDER = Paths.get("PROJECT_FOLDER");
    private final UUID projectId;
    private final String fileName;
    private final ProjectFileType projectFileType;
    private String contentType;

    /**
     * Require arg consructor.
     *
     * @param projectId       Project ID
     * @param fileName        file name
     * @param projectFileType {@link ProjectFileType}
     */
    public FileMetaData(UUID projectId, String fileName, ProjectFileType projectFileType) {
        this.projectId = projectId;
        this.projectFileType = projectFileType;
        //To avoid access of one project to files of other project
        this.fileName = fileName.replaceFirst(".*" + projectFileType.name(), projectFileType.name());
    }

    /**
     * Create FileMetaData LOG.
     *
     * @param projectId project ID
     * @param fileName  file name
     * @return FileMetaData instance
     */
    public static FileMetaData log(UUID projectId, String fileName) {
        return new FileMetaData(projectId, fileName, ProjectFileType.MIA_FILE_TYPE_LOG);
    }

    /**
     * Create FileMetaData UPDATE.
     *
     * @param projectId project ID
     * @param fileName  file name
     * @return FileMetaData instance
     */
    public static FileMetaData upload(UUID projectId, String fileName) {
        return new FileMetaData(projectId, fileName, ProjectFileType.MIA_FILE_TYPE_UPLOAD);
    }

    /**
     * Create FileMetaData project.
     *
     * @param projectId project ID
     * @param fileName  file name
     * @return FileMetaData instance
     */
    public static FileMetaData project(UUID projectId, String fileName) {
        return new FileMetaData(projectId, fileName, ProjectFileType.MIA_FILE_TYPE_PROJECT);
    }

    /**
     * Create FileMetaData project.
     *
     * @param projectId project ID
     * @param fileName  file name
     * @return FileMetaData instance
     */
    public static FileMetaData configuration(UUID projectId, String fileName) {
        return new FileMetaData(projectId, fileName, ProjectFileType.MIA_FILE_TYPE_CONFIGURATION);
    }

    /**
     * Create FileMetaData project.
     *
     * @param projectId project ID
     * @param fileName  file name
     * @return FileMetaData instance
     */
    public static FileMetaData define(UUID projectId, String fileName) throws IllegalArgumentException {
        Path projFolder = PROJECT_FOLDER.resolve(projectId.toString());
        Path pathCompare = projFolder.resolve(ProjectFileType.MIA_FILE_TYPE_LOG.name());
        if (fileName.startsWith(pathCompare.toString())) {
            return FileMetaData.log(projectId, fileName.replace(projFolder.toString(), ""));
        }
        pathCompare = projFolder.resolve(ProjectFileType.MIA_FILE_TYPE_UPLOAD.name());
        if (fileName.startsWith(pathCompare.toString())) {
            return FileMetaData.upload(projectId, fileName.replace(projFolder.toString(), ""));
        }
        pathCompare = projFolder.resolve(ProjectFileType.MIA_FILE_TYPE_PROJECT.name());
        if (fileName.startsWith(pathCompare.toString())) {
            return FileMetaData.project(projectId, fileName.replace(projFolder.toString(), ""));
        }
        pathCompare = projFolder.resolve(ProjectFileType.MIA_FILE_TYPE_CONFIGURATION.name());
        if (fileName.startsWith(pathCompare.toString())) {
            return FileMetaData.configuration(projectId, fileName.replace(projFolder.toString(), ""));
        }
        throw new FileMetaDataNotFoundException(fileName);
    }

    /**
     * ContentType of the file.
     *
     * @return content type of the file
     */
    public String getContentType() {
        if (Strings.isNullOrEmpty(contentType)) {
            this.contentType = "plain/text";
        }
        return contentType;
    }

    /**
     * Extension of the file.
     *
     * @return Extension of the file.
     */
    public String getFileExtension() {
        int lastIndex = fileName.lastIndexOf('.');
        String contentType = "unknown";
        if (lastIndex > -1) {
            contentType = fileName.substring(lastIndex + 1);
        }
        return contentType;
    }
}
