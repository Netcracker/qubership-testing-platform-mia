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

import java.nio.file.Path;

public interface Dos2Unix {

    /**
     * Run command {@code dest2unix} (utility from GNU, also present in Windows) at {@code dest} file.
     * Command run only if {@code needDos2Unix} parameter is {@code true} and
     * operation system where MIA ran is Linux.
     *
     * @param destSource path to file at which bash dos2unix command will be executed.
     */
    void runDos2Unix(Path destSource);
}
