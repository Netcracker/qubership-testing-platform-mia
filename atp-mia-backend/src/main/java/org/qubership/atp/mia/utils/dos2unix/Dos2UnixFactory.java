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

package org.qubership.atp.mia.utils.dos2unix;

import java.util.Optional;

import org.qubership.atp.mia.utils.dos2unix.impl.LinuxDos2Unix;
import org.qubership.atp.mia.utils.dos2unix.impl.WindowsDos2Unix;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Dos2UnixFactory {

    /**
     * Depending on operation system name where MIA run
     * returns dos2unix implementation.
     *
     * @param osName operation system name
     * @return dos2unix implementation or empty.
     */
    public static Optional<Dos2Unix> getDos2Unix(String osName, String dos2UnixPath) {
        Optional<Dos2Unix> res = Optional.empty();
        String name = osName.toLowerCase();
        if (name.startsWith("linux")) {
            log.debug("Returning linux dos2unix");
            res = Optional.of(new LinuxDos2Unix(dos2UnixPath));
        } else if (name.startsWith("windows")) {
            // for local testing purpose
            log.debug("Returning windows dos2unix");
            res = Optional.of(new WindowsDos2Unix(dos2UnixPath));
        } else {
            log.debug("Returning empty instead of dos2unix");
        }
        return res;
    }
}
