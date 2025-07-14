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

package org.qubership.atp.mia.model.exception;

import org.springframework.http.HttpStatus;

public class MiaException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * Constructor.
     */
    public MiaException(ErrorCodes errorCode, Object... params) {
        super(errorCode.getMessage(params));
        this.errorCode = errorCode.getCode();
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Constructor.
     */
    public MiaException(HttpStatus httpStatus, ErrorCodes errorCode, Object... params) {
        super(errorCode.getMessage(params));
        this.errorCode = errorCode.getCode();
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
