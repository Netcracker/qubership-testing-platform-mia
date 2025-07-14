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

package org.qubership.atp.mia.utils;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class PodNameUtils {

    private static final UUID RANDOM_ID = UUID.randomUUID();

    /**
     * Returns SERVICE_POD_NAME from system environment or randomly generates name.
     *
     * @return pod name
     */
    public static String getServicePodName() {
        String podName = System.getenv("SERVICE_POD_NAME");
        return StringUtils.isEmpty(podName) ? "atp-mia-" + RANDOM_ID : podName;
    }
}
