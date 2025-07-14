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

import static java.util.Objects.nonNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.springframework.beans.factory.annotation.Value;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GridFsRepositoryImpl implements GridFsRepository {

    private final GridFSBucket gridFsBucket;
    @Value("${gridfs.clean.period.days:14}")
    private long daysToClean;

    @Override
    public void cleanDb() {
        long dayInMs = 1000 * 60 * 60 * 24;
        Date dateDaysAgo = new Date(System.currentTimeMillis() - (daysToClean * dayInMs));
        gridFsBucket.find()
                .filter(Filters.and(
                        Filters.or(
                                Filters.in("metadata.fileType",
                                        ProjectFileType.MIA_FILE_TYPE_LOG.toString(),
                                        ProjectFileType.MIA_FILE_TYPE_UPLOAD.toString()),
                                Filters.in("metadata.type", "log")),
                        Filters.lt("uploadDate", dateDaysAgo)))
                .map(GridFSFile::getId).forEach(gridFsBucket::delete);
    }

    /**
     * Check if file exist in gridFs.
     *
     * @return true if present.
     */
    @Override
    public boolean exist(ProjectFile projectFile) {
        log.debug("Check file '{}' exist", projectFile);
        return gridFsBucket.find(Filters.eq("_id", new ObjectId(projectFile.getGridFsObjectId()))).first() != null;
    }

    /**
     * Find all project files for project in gridFs.
     */
    @Override
    public void findAndDeleteObsoleteProjectFiles(UUID projectId, List<String> excludeIds) {
        log.debug("Find all project files for project '{}'", projectId);
        gridFsBucket.find().filter(Filters.and(
                        Filters.eq("metadata.fileType", ProjectFileType.MIA_FILE_TYPE_PROJECT.toString()),
                        Filters.eq("metadata.projectId", projectId.toString()),
                        Filters.nin("_id", excludeIds)))
                .forEach(f -> {
                    if (!excludeIds.contains(f.getObjectId().toString())) {
                        gridFsBucket.delete(f.getObjectId());
                    }
                });
    }

    /**
     * Get file from GridFs as {@link Optional} of {@link InputStream} In case file not found,
     * return {@link Optional#empty()}.
     *
     * @return file as {@link Optional} of {@link InputStream}
     */
    @Override
    public Optional<InputStream> get(FileMetaData fileData) {
        log.debug("Getting file {}", fileData.getFileName());
        GridFSFile file = getGridFsFile(fileData);
        if (file == null) {
            return Optional.empty();
        }
        return Optional.of(gridFsBucket.openDownloadStream(file.getId()));
    }

    @Override
    public Optional<GridFSFile> getById(String gridFsObjectId) {
        if (nonNull(gridFsObjectId)) {
            log.debug("Getting file byId {}", gridFsObjectId);
            GridFSFile file = gridFsBucket.find(Filters.eq("_id", new ObjectId(gridFsObjectId))).first();
            if (nonNull(file)) {
                try (GridFSDownloadStream ds = gridFsBucket.openDownloadStream(file.getId())) {
                    return Optional.of(ds.getGridFSFile());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns collections size in selected bucket.
     *
     * @return size in bytes (long).
     */
    public String getCollectionsSize(UUID projectId) {
        List<Long> lengths = new ArrayList<>();
        if (projectId == null) {
            gridFsBucket.find().forEach(file -> lengths.add(file.getLength()));
        } else {
            gridFsBucket.find(new Document().append("projectId", projectId))
                    .forEach(file -> lengths.add(file.getLength()));
        }
        return String.format("Amount of documents %s, Size: %s byte",
                lengths.size(), lengths.stream().reduce(0L, Long::sum));
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    /**
     * Remove attachment.
     */
    @Override
    public void removeFile(FileMetaData fileData) {
        log.warn("Removing file {}", fileData.getFileName());
        GridFSFile res = getGridFsFile(fileData);
        if (res != null) {
            gridFsBucket.delete(res.getObjectId());
        }
    }

    @Override
    public void removeFile(ObjectId objectId) {
        gridFsBucket.delete(objectId);
    }

    /**
     * Rename file in gridFs.
     */
    @Override
    public void rename(ProjectFile projectFile) {
        log.debug("Rename file '{}'", projectFile);
        gridFsBucket.rename(new ObjectId(projectFile.getGridFsObjectId()),
                Paths.get(ProjectFileType.MIA_FILE_TYPE_PROJECT.name()).resolve(projectFile.getPathFile()).toString());
    }

    /**
     * Method saves file to gridfs. It not overrides file, if it exist, so files versioned by the upload time.
     *
     * @param fileData is {@link FileMetaData} which contains meta information for file storage.
     * @param fis      file to save.
     */
    @Override
    public ObjectId save(FileMetaData fileData, InputStream fis) {
        log.debug("Saving file {} ", fileData);
        Optional.ofNullable(gridFsBucket.find(queryToFind(fileData)).first())
                .ifPresent(f -> gridFsBucket.delete(f.getId()));
        GridFSUploadOptions uploadOptions = buildOptions(getDocument(fileData));
        return gridFsBucket.uploadFromStream(fileData.getFileName(), fis, uploadOptions);
    }

    private GridFSUploadOptions buildOptions(Document document) {
        return new GridFSUploadOptions().chunkSizeBytes(1024).metadata(document);
    }

    private Document getDocument(FileMetaData fileData) {
        Document document = new Document()
                .append("projectId", fileData.getProjectId().toString())
                .append("fileType", fileData.getProjectFileType().name())
                .append("uploadDate", LocalDateTime.now().toString())
                .append("extension", fileData.getFileExtension())
                .append("contentType", fileData.getContentType());
        return document;
    }

    private GridFSFile getGridFsFile(FileMetaData fileData) {
        log.debug("Getting fs file {}", fileData.getFileName());
        return gridFsBucket.find(queryToFind(fileData)).first();
    }

    private Bson queryToFind(FileMetaData fileData) {
        return Filters.and(Filters.eq("metadata.projectId", fileData.getProjectId().toString()),
                Filters.eq("metadata.fileType", fileData.getProjectFileType().name()),
                Filters.eq("filename", fileData.getFileName())
        );
    }

    /**
     * Get byte array from GridFSFile object.
     */
    public byte[] getFileContent(GridFSFile file) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gridFsBucket.downloadToStream(file.getObjectId(), bos);
        return bos.toByteArray();
    }
}
