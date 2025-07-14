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

package org.qubership.atp.mia.model.pot;

import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.exception.MiaException;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PotSessionException {

    private String errorCode;
    private HttpStatus httpStatus;
    private String message;

    /**
     * Creates from Exception a new RecordingException which can be stored in DB.
     *
     * @param e exception.
     */
    public PotSessionException(Exception e) {
        this.message = e.getMessage();
        if (e instanceof MiaException) {
            this.errorCode = ((MiaException) e).getErrorCode();
            this.httpStatus = ((MiaException) e).getHttpStatus();
        } else {
            this.errorCode = ErrorCodes.MIA_8000_UNEXPECTED_ERROR.toString();
            this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
