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

    PROJECTNAME_KEY(Constants.PROJECTNAME_KEY_OS, 24, TimeUnit.DAYS, CacheGroups.PROJECTS, true),
    CONFIGURATION_KEY(Constants.CONFIGURATION_KEY_OS, 5, TimeUnit.MINUTES, CacheGroups.PROJECTS, true),
    GENERAL_CONFIGURATION_KEY(Constants.GENERAL_CONFIGURATION_KEY_OS, 5, TimeUnit.MINUTES, CacheGroups.CONFIGURATION,
            true),
    ENVIRONMENTS_KEY(Constants.ENVIRONMENTS_KEY_OS, 1, TimeUnit.HOURS, CacheGroups.ENVIRONMENT, true),
    ENVIRONMENTS_BY_NAME_KEY(Constants.ENVIRONMENTS_BY_NAME_KEY_OS, 1, TimeUnit.HOURS, CacheGroups.ENVIRONMENT, true),
    ENVIRONMENTSFULL_KEY(Constants.ENVIRONMENTSFULL_KEY_OS, 10, TimeUnit.MINUTES, CacheGroups.ENVIRONMENT, true),
    SYSTEM_NAMES(Constants.SYSTEM_NAMES_OS, 20, TimeUnit.MINUTES, CacheGroups.SYSTEMS, false),
    MIA_PROJECTS_KEY(Constants.MIA_PROJECTS_KEY_OS, 2, TimeUnit.HOURS, CacheGroups.ENVIRONMENT, false),
    ATP_MACROS_KEY(Constants.ATP_MACROS_KEY_OS, 5, TimeUnit.MINUTES, CacheGroups.MACROS, true);

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

        public static final String PROJECTNAME_KEY_OS = "ATP_MIA_PROJECT_NAME";
        public static final String CONFIGURATION_KEY_OS = "ATP_MIA_CONFIGURATION";
        public static final String GENERAL_CONFIGURATION_KEY_OS = "ATP_MIA_GENERAL_CONFIGURATION";
        public static final String ENVIRONMENTS_KEY_OS = "ATP_MIA_ENVIRONMENTS";
        public static final String ENVIRONMENTS_BY_NAME_KEY_OS = "ATP_MIA_ENVIRONMENTS_BY_NAME";
        public static final String ENVIRONMENTSFULL_KEY_OS = "ATP_MIA_ENVIRONMENTSFULL";
        public static final String SYSTEM_NAMES_OS = "ATP_MIA_SYSTEM_NAMES";
        public static final String MIA_PROJECTS_KEY_OS = "ATP_MIA_PROJECTS";
        public static final String ATP_MACROS_KEY_OS = "ATP_MIA_MACROS";

    }
}
