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

package org.qubership.atp.mia.repo.gridfs;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFile;

import com.mongodb.client.gridfs.model.GridFSFile;

public interface GridFsRepository {

    /**
     * Cleans database.
     */
    void cleanDb();

    /**
     * Check file present in GridFs.
     *
     * @return boolean
     */
    boolean exist(ProjectFile projectFile);

    /**
     * Find all project files for project in gridFs and delete obsolete.
     */
    void findAndDeleteObsoleteProjectFiles(UUID projectId, List<String> excludeIds);

    /**
     * Get file from GridFs as {@link Optional} of {@link InputStream} In case file not found,
     * return {@link Optional#empty()}.
     *
     * @return file as {@link Optional} of {@link InputStream}
     */
    Optional<InputStream> get(FileMetaData fileData);

    /**
     * Get file from GridFs as {@link Optional} of {@link InputStream} In case file not found,
     * return {@link Optional#empty()}.
     *
     * @param gridFsObjectId id of file used in gridFs
     * @return file as {@link Optional} of {@link InputStream}
     */
    Optional<GridFSFile> getById(String gridFsObjectId);

    /**
     * Returns collections size in selected bucket.
     *
     * @return size in bytes (long).
     */
    String getCollectionsSize(UUID projectId);

    /**
     * Method returns state of db: enable it or not.
     *
     * @return true of false.
     */
    boolean isEnable();

    /**
     * Removes file by type.
     */
    void removeFile(FileMetaData fileData);

    /**
     * Removes file by id.
     */
    void removeFile(ObjectId objectId);

    /**
     * Rename file in gridFs.
     */
    void rename(ProjectFile projectFile);

    /**
     * Method saves file to gridfs. Overrides file, if it exist.
     *
     * @param fileData        is {@link FileMetaData} which contains meta information for file storage.
     * @param fileInputStream file to save.
     */
    ObjectId save(FileMetaData fileData, InputStream fileInputStream);

    /**
     * Get byte array from GridFSFile object.
     */
    byte[] getFileContent(GridFSFile file);
}

