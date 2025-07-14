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

package org.qubership.atp.mia.model.impl.executable;

import java.io.Serializable;

import org.qubership.atp.mia.exceptions.runtimeerrors.UnsupportedCommandTypeException;
import org.qubership.atp.mia.model.impl.CommandResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum CommandType implements Serializable {
    SSH,
    SSH_CheckFileOnServer,
    SSH_TransferFile,
    SSH_GenerationFile,
    SSH_DownloadFiles,
    SSH_UploadFile,
    SSH_UploadFileAndDownloadResult,
    SSH_GenerateEvent,
    SQL,
    REST,
    SOAP,
    SOAP_FROM_TEST_DATA,
    REST_FROM_TEST_DATA,
    VALIDATE_TEST_DATA,
    EVENT_TEST_DATA,
    SSH_FROM_TEST_DATA,
    SQL_FROM_TEST_DATA;

    /**
     * Prerequisite type.
     */
    @Deprecated
    public static final String COMMAND_ON_LOCALHOST = "COMMAND_ON_LOCALHOST";

    /**
     * Checks whether commandType is TEST_DATA process.
     *
     * @param commandType commandType of process.
     * @return true or false.
     */
    public static boolean isTestData(CommandType commandType) {
        switch (commandType) {
            case EVENT_TEST_DATA:
            case SQL_FROM_TEST_DATA:
            case VALIDATE_TEST_DATA:
            case SSH_FROM_TEST_DATA:
            case REST_FROM_TEST_DATA:
            case SOAP_FROM_TEST_DATA:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns response type according command type.
     *
     * @param commandType type of command.
     * @return response type according command type.
     */
    public static CommandResponse.CommandResponseType getResponseType(CommandType commandType) {
        CommandResponse.CommandResponseType result;
        switch (commandType) {
            case SSH:
            case SSH_CheckFileOnServer:
            case SSH_TransferFile:
            case SSH_GenerationFile:
            case SSH_DownloadFiles:
            case SSH_UploadFile:
            case SSH_UploadFileAndDownloadResult:
            case SSH_GenerateEvent:
            case SSH_FROM_TEST_DATA:
            case EVENT_TEST_DATA:
                result = CommandResponse.CommandResponseType.SSH;
                break;
            case SQL:
            case SQL_FROM_TEST_DATA:
            case VALIDATE_TEST_DATA:
            case SOAP_FROM_TEST_DATA:
                result = CommandResponse.CommandResponseType.SQL;
                break;
            case SOAP:
            case REST:
            case REST_FROM_TEST_DATA:
                result = CommandResponse.CommandResponseType.REST;
                break;
            default:
                throw new UnsupportedCommandTypeException(commandType);
        }
        return result;
    }
}
