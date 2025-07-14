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

package org.qubership.atp.mia.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.repo.configuration.ProjectFileRepository;
import org.qubership.atp.mia.service.execution.RecordingSessionsService;

import com.mongodb.Function;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Collation;

@ExtendWith(SkipTestInJenkins.class)
public class ScheduleServiceTest extends ConfigTestBean {

    static final ThreadLocal<LockManager> lockManager = new ThreadLocal<>();
    static final ThreadLocal<ProjectFileRepository> projectFileRepository = new ThreadLocal<>();
    static final ThreadLocal<ScheduleService> scheduleService = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        lockManager.set(mock(LockManager.class));
        projectFileRepository.set(mock(ProjectFileRepository.class));
        scheduleService.set(
                new ScheduleService(
                        gridFsService.get(),
                        lockManager.get(),
                        mock(RecordingSessionsService.class),
                        environmentsService.get(),
                        projectFileRepository.get()));
    }

    /*@Test
    void cleanMongoProjectFiles() {
        //mock
        ObjectId objectId1 = new ObjectId("123456789012".getBytes());
        ObjectId objectId2 = new ObjectId("012345678912".getBytes());
        BsonObjectId fileId1 = new BsonObjectId(objectId1);
        BsonObjectId fileId2 = new BsonObjectId(objectId2);
        GridFSFile gridFSFile1 = new GridFSFile(fileId1, "file1.txt", 1L, 1, Date.from(Instant.now()), new Document());
        GridFSFile gridFSFile2 = new GridFSFile(fileId2, "file2.txt", 1L, 1, Date.from(Instant.now()), new Document());
        List<ProjectFile> projectFiles = new ArrayList<>();
        projectFiles.add(new ProjectFile().toBuilder()
                .id(UUID.randomUUID())
                .name(gridFSFile1.getFilename())
                .gridFsObjectId(gridFSFile1.getObjectId().toString())
                .build());
        GridFSBucket gridFSBucket = mock(GridFSBucket.class);
        when(gridFSBucket.find())
                .thenReturn(gridFsFindIterable(Arrays.asList(gridFSFile1, gridFSFile2)));
        gridFsRepository.set(spy(new GridFsRepositoryImpl(gridFSBucket)));
        gridFsService.set(spy(new GridFsService(gridFsRepository.get(), miaContext.get())));
        when(projectFileRepository.get().findAllByProjectId(eq(projectId.get())))
                .thenReturn(Collections.singletonList(gridFSFile1.getObjectId().toString()));
        scheduleService.set(
                new ScheduleService(
                        gridFsService.get(),
                        lockManager.get(),
                        mock(RecordingSessionsService.class),
                        environmentsService.get(),
                        projectFileRepository.get()));
        //execute
        scheduleService.get().cleanMongoProjectFiles();
        startLockManager();
        //checks
        verify(gridFSBucket, times(0)).delete(objectId1);
        verify(gridFSBucket, times(1)).delete(objectId2);
    }*/

    @Test
    public void cleanDb() {
        scheduleService.get().cleanDb();
        startLockManager();
        Mockito.verify(gridFsService.get(), Mockito.times(1)).cleanDb();
        Mockito.verify(gridFsRepository.get(), Mockito.times(1)).cleanDb();
    }

    void startLockManager() {
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verify(lockManager.get()).executeWithLock(any(), runnable.capture());
        runnable.getValue().run();
    }

    GridFSFindIterable gridFsFindIterable(List<GridFSFile> files) {
        return new GridFSFindIterable() {
            @Override
            public GridFSFindIterable filter(Bson bson) {
                return this;
            }

            @Override
            public GridFSFindIterable limit(int i) {
                return null;
            }

            @Override
            public GridFSFindIterable skip(int i) {
                return null;
            }

            @Override
            public GridFSFindIterable sort(Bson bson) {
                return null;
            }

            @Override
            public GridFSFindIterable noCursorTimeout(boolean b) {
                return null;
            }

            @Override
            public GridFSFindIterable maxTime(long l, TimeUnit timeUnit) {
                return null;
            }

            @Override
            public GridFSFindIterable batchSize(int i) {
                return null;
            }

            @Override
            public GridFSFindIterable collation(Collation collation) {
                return null;
            }

            @Override
            public MongoCursor<GridFSFile> iterator() {
                return null;
            }

            @Override
            public MongoCursor<GridFSFile> cursor() {
                return null;
            }

            @Override
            public GridFSFile first() {
                return null;
            }

            @Override
            public <U> MongoIterable<U> map(Function<GridFSFile, U> function) {
                return null;
            }

            @Override
            public <A extends Collection<? super GridFSFile>> A into(A objects) {
                return null;
            }

            @Override
            public void forEach(Consumer<? super GridFSFile> block) {
                files.forEach(block);
            }
        };
    }
}
