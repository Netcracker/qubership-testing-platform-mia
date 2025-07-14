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

package org.qubership.atp.mia.exceptions.history;

import org.qubership.atp.mia.exceptions.MiaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "MIA-2063")
public class MiaCompoundHistoryRevisionRestoreException extends MiaException {

    public static final String DEFAULT_MESSAGE =
            "Revision was not restored. %s version of Compound has removed processes.";

    public MiaCompoundHistoryRevisionRestoreException() {
        super(DEFAULT_MESSAGE);

    }

    public MiaCompoundHistoryRevisionRestoreException(long version) {
        super(String.format(DEFAULT_MESSAGE, version));
    }
}
