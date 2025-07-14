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

package org.qubership.atp.mia.model.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.qubership.atp.mia.exceptions.runtimeerrors.UnsupportedCommandTypeException;
import org.qubership.atp.mia.model.impl.executable.CommandType;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.output.HtmlPage;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.utils.Utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class CommandResponse {

    private CommandResponseType type;
    private String command = "";
    private Map<String, String> connectionInfo = new HashMap<>();
    private LinkedList<CommandOutput> commandOutputs;
    private SqlResponse sqlResponse;
    private HtmlPage htmlPage;
    private LinkedList<String> description;
    private LinkedList<Exception> errors;
    private boolean checkStatusCodeFlag;
    private String statusCode;
    private List<String> expectedCodes;
    private String postScriptExecutionReport;

    /**
     * Used only for tests.
     */
    public CommandResponse() {
    }

    public CommandResponse(SqlResponse sqlResponse) {
        this.sqlResponse = sqlResponse;
    }

    public CommandResponse(LinkedList<CommandOutput> commandOutputs) {
        this.commandOutputs = commandOutputs;
    }

    public CommandResponse(CommandOutput commandOutput) {
        addCommandOutput(commandOutput);
    }

    public CommandResponse(HtmlPage htmlPage) {
        this.htmlPage = htmlPage;
    }

    /**
     * Transform response type to connection name from environments.
     *
     * @param type response type.
     * @return connection name for environments.
     */
    public static String getConnectionNameFromResponseType(CommandResponseType type) {
        switch (type) {
            case SSH:
                return "SSH";
            case SQL:
                return "DB";
            case REST:
                return "HTTP";
            case NO_SYSTEM_REQUIRED:
                return "NO_SYSTEM_REQUIRED";
            default:
                throw new UnsupportedCommandTypeException(type);
        }
    }

    /**
     * Generates {@link CommandResponse} with Connection info which filled with current date.
     * Need for {@link CommandType#REST_FROM_TEST_DATA} command type.
     *
     * @return {@link CommandResponse} with Connection info which filled with current date.
     */
    public static CommandResponse getCommandResponseWithFilledRequestInfo() {
        CommandResponse commandResponse = new CommandResponse();
        final String notificationUser =
                "For REST_FROM_TEST_DATA please reference the excel file for a connection information";
        HashMap<String, String> connectionInfo = new HashMap<String, String>() {
            {
                put("bodyRequest", notificationUser);
                put("timestampRequest", Utils.getTimestamp());
                put("timestampResponse", Utils.getTimestamp());
            }
        };
        commandResponse.setConnectionInfo(connectionInfo);
        return commandResponse;
    }

    /**
     * Adds CommandResponse.
     *
     * @param commandResponse CommandResponse
     */
    public void addCommandResponse(CommandResponse commandResponse) {
        this.command += this.command.isEmpty() ? "" : "\n\n";
        this.command += commandResponse.command;
        connectionInfo.putAll(commandResponse.getConnectionInfo());
        addCommandOutputs(commandResponse.getCommandOutputs());
        addErrors(commandResponse.getErrors());
    }

    /**
     * Adds log (as first element of commandOutputs).
     *
     * @param commandOutput CommandOutput
     */
    public void addLog(CommandOutput commandOutput) {
        createCommandOutputs();
        this.commandOutputs.add(0, commandOutput);
    }

    /**
     * Adds CommandOutput.
     *
     * @param commandOutput CommandOutput
     */
    public void addCommandOutput(CommandOutput commandOutput) {
        createCommandOutputs();
        this.commandOutputs.add(commandOutput);
    }

    /**
     * Concat contents of CommandOutput.
     *
     * @param commandOutput command output which content need to be merged
     */
    public void concatCommandOutput(CommandOutput commandOutput) {
        createCommandOutputs();
        if (this.commandOutputs.isEmpty()) {
            commandOutputs.add(commandOutput);
        } else {
            commandOutputs.getFirst().concatContent(commandOutput);
        }
    }

    /**
     * Adds CommandOutputs.
     *
     * @param commandOutputs CommandOutputs
     */
    public void addCommandOutputs(LinkedList<CommandOutput> commandOutputs) {
        if (commandOutputs != null) {
            createCommandOutputs();
            this.commandOutputs.addAll(commandOutputs);
        }
    }

    /**
     * Add description.
     *
     * @param description description
     */
    public void addDescription(String description) {
        createDescription();
        this.description.add(description);
    }

    /**
     * Add error.
     *
     * @param e error
     */
    public void addError(Exception e) {
        createErrors();
        errors.add(e);
    }

    /**
     * Add errors.
     *
     * @param e errors
     */
    public void addErrors(LinkedList<Exception> e) {
        if (e != null) {
            createErrors();
            errors.addAll(e);
        }
    }

    /**
     * Create description list if null.
     */
    private void createDescription() {
        if (this.description == null) {
            this.description = new LinkedList<>();
        }
    }

    /**
     * Create errors list if null.
     */
    private void createErrors() {
        if (this.errors == null) {
            this.errors = new LinkedList<>();
        }
    }

    /**
     * Create commandOutputs list if null.
     */
    private void createCommandOutputs() {
        if (this.commandOutputs == null) {
            this.commandOutputs = new LinkedList<>();
        }
    }

    public enum CommandResponseType {
        REST, SSH, SQL, NO_SYSTEM_REQUIRED
    }
}

