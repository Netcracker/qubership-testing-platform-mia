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

package org.qubership.atp.mia.model;

import java.util.concurrent.TimeUnit;

import lombok.Getter;

public enum CacheKeys {
    AUTH_PROJECTS_KEY(Constants.AUTH_PROJECTS_KEY, 2, TimeUnit.MINUTES, CacheGroups.PROJECTS, true),

    PROJECTNAME_KEY(Constants.PROJECTNAME_KEY, 24, TimeUnit.DAYS, CacheGroups.PROJECTS, true),
    CONFIGURATION_KEY(Constants.CONFIGURATION_KEY, 5, TimeUnit.MINUTES, CacheGroups.PROJECTS, true),
    GENERAL_CONFIGURATION_KEY(Constants.GENERAL_CONFIGURATION_KEY, 5, TimeUnit.MINUTES, CacheGroups.CONFIGURATION,
            true),
    
    // Lazy loading cache keys for processes and compounds
    PROCESSES_LIST_KEY(Constants.PROCESSES_LIST_KEY, 1, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    COMPOUNDS_LIST_KEY(Constants.COMPOUNDS_LIST_KEY, 1, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    PROCESS_BY_NAME_KEY(Constants.PROCESS_BY_NAME_KEY, 1, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    COMPOUND_BY_NAME_KEY(Constants.COMPOUND_BY_NAME_KEY, 1, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    PROCESS_BY_ID_KEY(Constants.PROCESS_BY_ID_KEY, 1, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    COMPOUND_BY_ID_KEY(Constants.COMPOUND_BY_ID_KEY, 1, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    PROCESS_IDS_KEY(Constants.PROCESS_IDS_KEY, 2, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    COMPOUND_IDS_KEY(Constants.COMPOUND_IDS_KEY, 2, TimeUnit.HOURS, CacheGroups.CONFIGURATION, true),
    
    ENVIRONMENTS_KEY(Constants.ENVIRONMENTS_KEY, 1, TimeUnit.HOURS, CacheGroups.ENVIRONMENT, true),
    ENVIRONMENTS_BY_NAME_KEY(Constants.ENVIRONMENTS_BY_NAME_KEY, 1, TimeUnit.HOURS, CacheGroups.ENVIRONMENT, true),
    ENVIRONMENTSFULL_KEY(Constants.ENVIRONMENTSFULL_KEY, 10, TimeUnit.MINUTES, CacheGroups.ENVIRONMENT, true),
    SYSTEM_NAMES(Constants.SYSTEM_NAMES, 20, TimeUnit.MINUTES, CacheGroups.SYSTEMS, false),
    MIA_PROJECTS_KEY(Constants.MIA_PROJECTS_KEY, 2, TimeUnit.HOURS, CacheGroups.ENVIRONMENT, false),
    ATP_MACROS_KEY(Constants.ATP_MACROS_KEY, 5, TimeUnit.MINUTES, CacheGroups.MACROS, true);

    @Getter
    private final String key;
    @Getter
    private final int timeToLive;
    @Getter
    private final TimeUnit timeUnit;
    @Getter
    private final CacheGroups cacheGroup;
    @Getter
    private final boolean isKeyContainProjectId;

    CacheKeys(String key, int timeToLive, TimeUnit timeUnit, CacheGroups cacheGroup, boolean isCacheContainsProjectId) {
        this.key = key;
        this.timeToLive = timeToLive;
        this.timeUnit = timeUnit;
        this.cacheGroup = cacheGroup;
        this.isKeyContainProjectId = isCacheContainsProjectId;
    }

    /**
     * Get Time To Leave value in seconds.
     *
     * @return Time To Leave value in seconds
     */
    public int getTtlInSeconds() {
        switch (timeUnit) {
            case SECONDS:
                return timeToLive;
            case MINUTES:
                return 60 * timeToLive;
            case HOURS:
                return 60 * 60 * timeToLive;
            case DAYS:
                return 24 * 60 * 60 * timeToLive;
            default:
                return 0;
        }
    }

    public enum CacheGroups {
        PROJECTS, ENVIRONMENT, SYSTEMS, MACROS, CONFIGURATION
    }

    public static class Constants {

        public static final String AUTH_PROJECTS_KEY = "auth_projects";

        public static final String PROJECTNAME_KEY = "ATP_MIA_PROJECT_NAME_OS";
        public static final String CONFIGURATION_KEY = "ATP_MIA_CONFIGURATION_OS";
        public static final String GENERAL_CONFIGURATION_KEY = "ATP_MIA_GENERAL_CONFIGURATION_OS";
        
        // Lazy loading cache keys
        public static final String PROCESSES_LIST_KEY = "ATP_MIA_PROCESSES_LIST_OS";
        public static final String COMPOUNDS_LIST_KEY = "ATP_MIA_COMPOUNDS_LIST_OS";
        public static final String PROCESS_BY_NAME_KEY = "ATP_MIA_PROCESS_BY_NAME_OS";
        public static final String COMPOUND_BY_NAME_KEY = "ATP_MIA_COMPOUND_BY_NAME_OS";
        public static final String PROCESS_BY_ID_KEY = "ATP_MIA_PROCESS_BY_ID_OS";
        public static final String COMPOUND_BY_ID_KEY = "ATP_MIA_COMPOUND_BY_ID_OS";
        public static final String PROCESS_IDS_KEY = "ATP_MIA_PROCESS_IDS_OS";
        public static final String COMPOUND_IDS_KEY = "ATP_MIA_COMPOUND_IDS_OS";
        
        public static final String ENVIRONMENTS_KEY = "ATP_MIA_ENVIRONMENTS_OS";
        public static final String ENVIRONMENTS_BY_NAME_KEY = "ATP_MIA_ENVIRONMENTS_BY_NAME_OS";
        public static final String ENVIRONMENTSFULL_KEY = "ATP_MIA_ENVIRONMENTSFULL_OS";
        public static final String SYSTEM_NAMES = "ATP_MIA_SYSTEM_NAMES_OS";
        public static final String MIA_PROJECTS_KEY = "ATP_MIA_PROJECTS_OS";
        public static final String ATP_MACROS_KEY = "ATP_MIA_MACROS_OS";

    }
}
