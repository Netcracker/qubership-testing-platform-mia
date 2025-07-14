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

package org.qubership.atp.mia.exceptions.dos2unix;

import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "MIA-2001")
public class Dos2unixLinuxFailException extends MiaException {

    public Dos2unixLinuxFailException(String dest, String dosPath) {
        super(ErrorCodes.MIA_2001_DOS2UNIX_LINUX_FAIL, dest, dosPath);
    }
}
