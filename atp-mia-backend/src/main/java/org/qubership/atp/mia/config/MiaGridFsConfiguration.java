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

package org.qubership.atp.mia.config;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;
import org.qubership.atp.mia.model.file.FileMetaData;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.repo.gridfs.GridFsRepository;
import org.qubership.atp.mia.repo.gridfs.GridFsRepositoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Indexes;
import com.mongodb.connection.ConnectionPoolSettings;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MiaGridFsConfiguration {

    private final MeterRegistry meterRegistry;

    @Value("${gridfs.enable:false}")
    private String enable;
    @Value("${gridfs.database:#{null}}")
    private String database;
    @Value("${gridfs.host:#{null}}")
    private String host;
    @Value("${gridfs.port:#{null}}")
    private String port;
    @Value("${gridfs.username:#{null}}")
    private String user;
    @Value("${gridfs.password:#{null}}")
    private String password;

    /**
     * Provides stub repository if gridFs properties are not provided. Used in cases when file
     * parameters are not needed.
     */
    @Bean
    public GridFsRepository provideRepo() {
        try {
            if (!enable.equals("true") || database == null || host == null) {
                log.info("Shutting down GridFS; isEnable {}, db {}, host {}", enable, database, host);
                throw new Exception();
            }
            GridFSBucket gridFsBucket = provideGridFileSystemBuckets(meterRegistry);
            log.info("Created gridFS bucket");
            return new GridFsRepositoryImpl(gridFsBucket);
        } catch (Exception e) {
            String message = "Can not initialize Grid FS module, will use mock instead";
            log.warn(message, e);
            return new GridFsRepository() {
                @Override
                public boolean isEnable() {
                    return false;
                }

                @Override
                public ObjectId save(FileMetaData fileData, InputStream fileInputStream) {
                    log.error("Can't save, it's only mock");
                    return ObjectId.get();
                }

                @Override
                public boolean exist(ProjectFile projectFile) {
                    return true;
                }

                @Override
                public void findAndDeleteObsoleteProjectFiles(UUID projectId, List<String> excludeIds) {
                }

                @Override
                public Optional<InputStream> get(FileMetaData fileData) {
                    log.error("Can't get, it's only mock");
                    return Optional.empty();
                }

                @Override
                public Optional<GridFSFile> getById(String gridFsObjectId) {
                    log.error("Can't get, it's only mock");
                    return Optional.empty();
                }

                @Override
                public void removeFile(FileMetaData fileData) {
                    log.error("Can't remove, it's only mock");
                }

                @Override
                public void removeFile(ObjectId objectId) {
                    log.error("Can't remove, it's only mock");
                }

                @Override
                public void rename(ProjectFile projectFile) {
                }

                @Override
                public String getCollectionsSize(UUID projectId) {
                    return "0";
                }

                @Override
                public void cleanDb() {
                }

                @Override
                public byte[] getFileContent(GridFSFile file) {
                    return new byte[0];
                }
            };
        }
    }

    /**
     * Provides {@link GridFSBucket} for getting files from database.
     *
     * @return GridFSBucket by specified parameters.
     */
    private GridFSBucket provideGridFileSystemBuckets(MeterRegistry meterRegistry) {
        log.info("Creating gridFS bucket");
        String mongoClientUri = String.format("mongodb://%s%s:%s/%s",
                user.isEmpty() || password.isEmpty() ? "" : user + ":" + password + "@",
                host, port, database.isEmpty() ? "" : "?authSource=" + database);
        log.info("Mongo connection string: {}", mongoClientUri);
        ConnectionString connectionString = new ConnectionString(mongoClientUri);
        MongoClient mongo = MongoClients.create(
                MongoClientSettings.builder()
                        .addCommandListener(new MongoMetricsCommandListener(meterRegistry))
                        .applyToConnectionPoolSettings((ConnectionPoolSettings.Builder builder) -> {
                            builder.addConnectionPoolListener(new MongoMetricsConnectionPoolListener(meterRegistry))
                                    .build();
                        })
                        .applyConnectionString(connectionString)
                        .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                        .build());
        CodecRegistry pojoCodecRegistry =
                CodecRegistries.fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoDatabase db = mongo.getDatabase(database).withCodecRegistry(pojoCodecRegistry);
        GridFSBucket gridFsBucket = GridFSBuckets.create(db);
        MongoCollection<Document> filesCollection = db.getCollection("fs.files");
        // composite index with two fields
        filesCollection.createIndex(Indexes.ascending("metadata.projectId", "metadata.fileType"));
        /* TODO Implement config
        MongoCollection<Document> configCollection = db.getCollection("fs.config");
        configCollection.createIndex(Indexes.ascending("metadata.uuid", "metadata.filename"), opts);
         */
        return gridFsBucket;
    }
}
