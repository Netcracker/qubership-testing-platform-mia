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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Constants {

    public static final String EVENT_GENERATION_FILE = "EventGenerationFile";
    public static final String EVENT_GENERATION_SHEET = "EventGenerationSheet";
    public static final String EVENT_GENERATION_SCENARIO = "EventGenerationScenario";
    public static final String EVENT_GENERATION_TC = "EventGenerationTestCase";
    public static final String DEFAULT_SYSTEM_NAME = "Billing System";
    public static final UUID DEFAULT_PROJECT_ID = new UUID(0, 1);
    public static final UUID DEFAULT_SESSION_ID = new UUID(0, 2);
    public static final String DEFAULT_PROJECT_NAME = "default";
    public static final String REDIRECT_URL = "/redirect-uri/";
    public static final String SERVICE_PATH_DEFAULT = "/api/atp-mia/v1";
    public static final String DEFAULT_WORKING_DIRECTORY = "/tmp/TA/";
    public static final String DEFAULT_PROJECT_FLOW_FOLDER =
            "." + File.separator + "src" + File.separator + "main" + File.separator + "config" + File.separator
                    + "project" + File.separator + "default";
    public static final String ERROR = "Error";

    public static final String ERROR_SSH_DOWNLOAD_FAILED = "SSH download failed.";
    public static final String MIA_ROOT_DIRECTORY = "MIA_ROOT_DIRECTORY";


    public static final List<String> sqlOperators = Arrays.asList("select", "declare", "update", "insert", "delete");

    /**
     * Custom parameters.
     */
    public enum CustomParameters {
        ACCOUNT_NUMBER("accountNumber"),
        CUSTOMER_REF("customerRef"),
        ENVIRONMENT_ID("environmentId"),
        FULL_TRACE_MODE("fullTraceMode"),
        GENEVA_DATE("genevaDate"),
        INFINYS_ROOT("infinys_root"),
        WORKING_DIRECTORY("workingDirectory"),
        PROCESS_NAME("processName"),
        PROJECT_ID("projectId");
        private final String paramName;

        CustomParameters(String paramName) {
            this.paramName = paramName;
        }

        @Override
        public String toString() {
            return paramName;
        }
    }

    public enum SystemSwitcherNames {
        fullTraceMode,
        needDos2Unix,
        replaceFileOnUploadAttachment,
        stopOnFail
    }
}
