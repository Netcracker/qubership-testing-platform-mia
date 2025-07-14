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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@NoArgsConstructor
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ExecutionResponse implements Serializable {

    private static final long serialVersionUID = 24941995508251952L;

    @Nullable
    private UUID sseId;
    private int order;
    private boolean finalMessage;
    private String processName;
    private String executedCommand;
    private CommandResponse commandResponse;
    private List<SqlResponse> validations;
    @Nullable
    private List<CommandResponse> prerequisites;
    @Nullable
    private ProcessStatus processStatus;
    @Nullable
    private MiaException error;
    @Nullable
    private HashMap<String, String> globalVariables;
    private String entityUrl;
    private UUID entityId;
    private long duration;

    /**
     * Sets warning of whole process.
     *
     * @param warning warning text
     */
    public void setWarn(String warning) {
        if (processStatus == null) {
            log.debug("process status is null, initiating it with empty value");
            processStatus = new ProcessStatus();
        }
        if (!processStatus.getStatus().equals(Statuses.FAIL)) {
            log.debug("process status not fail, but [{}], so setting it to WARNING", processStatus.getStatus());
            processStatus.setStatus(Statuses.WARNING);
        }
        if (commandResponse == null) {
            commandResponse = new CommandResponse();
        }
        commandResponse.addDescription(warning);
    }
}
