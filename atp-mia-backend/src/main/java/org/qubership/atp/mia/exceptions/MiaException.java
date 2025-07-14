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

package org.qubership.atp.mia.exceptions;

import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "MIA-0000")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class MiaException extends AtpException {

    private String reason = "MIA-0000";

    /**
     * Constructor.
     *
     * @param message message of exception.
     */
    public MiaException(String message) {
        super(message);
        log.error(message);
    }

    /**
     * Constructor.
     *
     * @param e exception.
     */
    public MiaException(Exception e) {
        super(e.getMessage());
        log.error(reason, e);
    }

    /**
     * Constructor.
     *
     * @param errorCodes ErrorCodes.
     * @param args       params for ErrorCodes
     */
    public MiaException(ErrorCodes errorCodes, Object... args) {
        super(errorCodes.getMessage(args));
        this.reason = errorCodes.getCode();
        if (args != null && args.length > 0 && args[args.length - 1] instanceof Exception) {
            log.error(errorCodes.getMessage(args), args[args.length - 1]);
        } else {
            log.error(errorCodes.getMessage(args));
        }
    }

    @Override
    public String getLocalizedMessage() {
        return null;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[]{};
    }

    /**
     * Getter of reason.
     *
     * @return reason
     */
    public String getReason() {
        return reason == null ? "MIA-0000" : reason;
    }

    /**
     * Setter of reason.
     *
     * @param reason reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }
}
