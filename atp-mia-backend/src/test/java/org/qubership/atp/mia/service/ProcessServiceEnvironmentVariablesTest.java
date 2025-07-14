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

import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.pot.ProcessStatus;
import org.qubership.atp.mia.model.pot.Statuses;


public class ProcessServiceEnvironmentVariablesTest extends ProcessServiceBaseTest {

    private final String commandName = "REST";
    private final String connectionType = "HTTP";
    private final String expUrl = "http://localhost:8080";
    private final String expUrlParam = "url";
    private final String expLogin = "user1";
    private final String expLoginParam = "login";

    @Test
    public void environmentVariableShouldBeEvaluated() {
        //expect
        ProcessStatus expectedStatus = new ProcessStatus();
        expectedStatus.setStatus(Statuses.SUCCESS);
        String evalSystemKey = "evalSystemKey";
        String evalConnectionKey = "evalConnectionKey";
        //setup
        miaContext.get().getFlowData().addParameter(evalSystemKey, expUrlParam.toUpperCase());
        miaContext.get().getFlowData().addParameter(evalConnectionKey, expLoginParam);
        final Command command = new Command("Command", commandName, testSystem.get().getName(), listToSet("commandValue"));
        final List<Validation> validations = new ArrayList<>();
        final ProcessSettings process = new ProcessSettings().toBuilder().name("pr").command(command).validations(validations).build();
        //do
        executeProcess(process);
        Assert.assertEquals(expUrl, miaContext.get().getFlowData().getParameters().get(expUrlParam.toUpperCase()));
    }

    @Test
    public void shouldAddEnvironmentParametersToFlowData() {
        //expect
        ProcessStatus expectedStatus = new ProcessStatus();
        expectedStatus.setStatus(Statuses.SUCCESS);
        //setup
        final Command command = new Command("Command", commandName, testSystem.get().getName(), listToSet("commandValue"));
        final List<Validation> validations = new ArrayList<>();
        final ProcessSettings process = new ProcessSettings().toBuilder().name("pr").command(command).validations(validations).build();
        //do
        ExecutionResponse response = executeProcess(process);
        Assert.assertEquals(expectedStatus.getStatus(), response.getProcessStatus().getStatus());
        Assert.assertTrue("No key in flow data: " + expUrlParam.toUpperCase(),
                miaContext.get().getFlowData().getParameters().containsKey(expUrlParam.toUpperCase()));
        Assert.assertEquals(expUrl, miaContext.get().getFlowData().getParameters().get(expUrlParam.toUpperCase()));
    }
}
