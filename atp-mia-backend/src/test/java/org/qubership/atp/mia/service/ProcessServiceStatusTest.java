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

package org.qubership.atp.mia.service;

import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;

public class ProcessServiceStatusTest extends ProcessServiceBaseTest {

    private final ThreadLocal<String> processName = new ThreadLocal<>();
    private final ThreadLocal<CommandResponse> positiveResponse = new ThreadLocal<>();
    private final ThreadLocal<CommandResponse> negativeResponse = new ThreadLocal<>();
    private final ThreadLocal<Command> positiveCommand = new ThreadLocal<>();
    private final ThreadLocal<Command> negativeCommand = new ThreadLocal<>();

    @BeforeEach
    public void beforeExecutionHelperServiceStatusTest() {
        processName.set("processName");
        positiveResponse.set(new CommandResponse());
        negativeResponse.set(new CommandResponse());
        positiveCommand.set(new Command("PositiveCommand", "VALIDATE_TEST_DATA", testSystem.get().getName(),
                listToSet("positiveCommandValue")));
        negativeCommand.set(new Command("NegativeCommand", "VALIDATE_TEST_DATA", testSystem.get().getName(),
                listToSet("negativeCommandValue")));
        TableMarkerResult tmr = new TableMarkerResult();
        tmr.setTableRowCount("1", "1", Statuses.SUCCESS);
        SqlResponse sqlResponse = new SqlResponse();
        sqlResponse.setTableMarkerResult(tmr);
        positiveResponse.get().setSqlResponse(sqlResponse);
        when(testDataHelperService.get().validate(positiveCommand.get())).thenReturn(positiveResponse.get());
        TableMarkerResult negTmr = new TableMarkerResult();
        negTmr.setTableRowCount("2", "1", Statuses.FAIL);
        SqlResponse response = new SqlResponse();
        response.setTableMarkerResult(negTmr);
        negativeResponse.get().setSqlResponse(response);
        when(testDataHelperService.get().validate(negativeCommand.get())).thenReturn(negativeResponse.get());
    }

    // ### Tests for marking test data validation ###
    @Test
    public void validateTestDataShouldBeFailed() {
        //setup
        final List<Validation> validations = new ArrayList<>();
        final ProcessSettings process = ProcessSettings.builder().name(processName.get()).command(negativeCommand.get())
                .validations(validations).build();
        //do
        ExecutionResponse response = executeProcess(process);
        Assert.assertEquals(Statuses.FAIL, response.getProcessStatus().getStatus());
    }

    @Test
    public void validateTestDataShouldBeSuccess() {
        //expect
        ProcessStatus expectedStatus = new ProcessStatus();
        expectedStatus.setStatus(Statuses.SUCCESS);
        //setup
        final List<Validation> validations = new ArrayList<>();
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName.get()).command(positiveCommand.get())
                .validations(validations).build();
        //do
        ExecutionResponse response = executeProcess(process);
        Assert.assertEquals(expectedStatus.getStatus(), response.getProcessStatus().getStatus());
    }
}
