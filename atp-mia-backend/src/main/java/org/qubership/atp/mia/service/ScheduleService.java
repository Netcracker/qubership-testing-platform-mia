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

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.mia.repo.configuration.ProjectFileRepository;
import org.qubership.atp.mia.service.configuration.EnvironmentsService;
import org.qubership.atp.mia.service.execution.RecordingSessionsService;
import org.qubership.atp.mia.service.file.GridFsService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final GridFsService gridFsService;
    private final LockManager lockManager;
    private final RecordingSessionsService recordingSessionsService;
    private final EnvironmentsService environmentsService;
    private final ProjectFileRepository projectFileRepository;

    @Value("${gridfs.enable}")
    private Boolean gridFsEnable;

    @Value("${atp.clean.logs.interval.hours}")
    private String cleanInterval;

    /**
     * Run clean files from gridFs according to cron job.
     */
    @Scheduled(cron = "${atp.mia.cron.clean.gridfs}")
    public void cleanDb() {
        lockManager.executeWithLock("mia_cleanMongoDb", () -> {
            log.info("Scheduled clean GridFs job started");
            gridFsService.cleanDb();
            log.info("Scheduled clean GridFs job finished");
        });
    }

    /**
     * Run clean files from gridFs according to cron job.
     */
    /*@Scheduled(cron = "${atp.mia.cron.clean.mongoProjectFiles}")
    public void cleanMongoProjectFiles() {
        lockManager.executeWithLock("mia_cleanMongoProjectFiles", () -> {
            log.info("Scheduled clean GridFs ProjectFiles job started");
            environmentsService.getProjects().forEach(p ->
                    gridFsService.cleanMongoProjectFiles(p.getId(),
                            projectFileRepository.findAllByProjectId(p.getId())));
            log.info("Scheduled clean GridFs ProjectFiles job finished");
        });
    }*/

    /**
     * Run clean files from gridFs according to cron job.
     */
    @Scheduled(cron = "${atp.mia.cron.clean.logs}")
    public void cleanLogs() {
        if (gridFsEnable) {
            lockManager.executeWithLock("mia_cleanDb", () -> {
                log.info("Scheduled clean logs job started");
                gridFsService.cleanLogs((int) Utils.parseLongValueOrDefault(cleanInterval, 1, "CleanLogInterval"));
                log.info("Scheduled clean logs job finished");
            });
        }
    }

    /**
     * Clean Postgres table with old POT sessions.
     */
    @Scheduled(cron = "${atp.mia.cron.clean.postgresql}")
    public void cleanPostgresqlPot() {
        lockManager.executeWithLock("mia_cleanPostgresql", () -> {
            log.info("Scheduled clean postgresql POT tables job started");
            recordingSessionsService.deleteOldSession();
            log.info("Scheduled clean postgresql POT tables finished");
        });
    }
}
