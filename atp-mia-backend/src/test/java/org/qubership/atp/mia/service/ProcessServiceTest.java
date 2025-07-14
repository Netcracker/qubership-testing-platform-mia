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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.qubership.atp.mia.exceptions.prerequisite.PrerequisiteTypeUnsupportedException;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Input;
import org.qubership.atp.mia.model.impl.executable.Prerequisite;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.TableMarker;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.impl.request.CompoundRequest;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.model.pot.db.table.TableMarkerResult;

import com.google.common.collect.ImmutableList;

public class ProcessServiceTest extends ProcessServiceBaseTest {

    private static final String testDataFilePath = "src/test/resources/testData/";
    private final String sqlPrerequisiteValue = "select * from table;";
    private final String sqlFileValue = "test1.sql";
    private final String sshPrerequisiteValue = "sshPrerequisiteValue;";
    private final String commandValue = "commandValue";
    private final String processName = "processName";
    private final String compoundName = "Compound Name";
    private final String input_name = "input_name";
    private final String input_value = "input_value";

    /**
     * Checks the same failed marker overwrite passed.
     */
    @Test
    public void addLogStatus_testFailedMarkersAfterPassed() {
        ProcessConfiguration processWithMarkers = DeserializerConfigBaseTest.getGenerationPp();
        List<String> passedMarkersList = new ArrayList<>();
        passedMarkersList.add("Process finishing with status \\d+");
        processWithMarkers.getProcessSettings().getCommand().getMarker().setPassedMarkerForLog(passedMarkersList);
        List<String> failedMarkersList = new ArrayList<>();
        failedMarkersList.add("Process \\w+ with status ");
        processWithMarkers.getProcessSettings().getCommand().getMarker().setFailedMarkersForLog(failedMarkersList);
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.addLog(new CommandOutput("src/test/resources/bill/log.txt", null, true, miaContext.get()));
        when(sshService.get().executeCommandAndGenerateFile(eq(processWithMarkers.getProcessSettings().getCommand()))).thenReturn(commandResponse);
        ExecutionResponse executionResponse = executeProcess(processWithMarkers.getProcessSettings());
        Assert.assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
    }

    @Test
    public void addLogStatus_testFailedMarkersWithRegExp() {
        ProcessConfiguration processWithMarkers = DeserializerConfigBaseTest.getGenerationPp();
        String failedMarker = "Process finishing with status \\d";
        List<String> failedMarkersList = new ArrayList<>();
        failedMarkersList.add(failedMarker);
        processWithMarkers.getProcessSettings().getCommand().getMarker().setFailedMarkersForLog(failedMarkersList);
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.addCommandOutput(new CommandOutput("src/test/resources/bill/log.txt", null, true, miaContext.get()));
        when(sshService.get().executeCommandAndGenerateFile(eq(processWithMarkers.getProcessSettings().getCommand()))).thenReturn(commandResponse);
        ExecutionResponse executionResponse = super.executeProcess(processWithMarkers.getProcessSettings()); //save and assert
        Assert.assertEquals(Statuses.FAIL, executionResponse.getProcessStatus().getStatus());
    }

    @Test
    public void addLogStatus_testPositiveMarkersWithRegExp() {
        ProcessConfiguration processWithMarkers = DeserializerConfigBaseTest.getGenerationPp();
        String passedMarker = "Process finishing with status \\d";
        List<String> passedMarkersList = new ArrayList<>();
        passedMarkersList.add(passedMarker);
        processWithMarkers.getProcessSettings().getCommand().getMarker().setPassedMarkerForLog(passedMarkersList);
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.addCommandOutput(new CommandOutput("src/test/resources/bill/log.txt", null, true, miaContext.get()));
        when(sshService.get().executeCommandAndGenerateFile(eq(processWithMarkers.getProcessSettings().getCommand()))).thenReturn(commandResponse);
        ExecutionResponse executionResponse = super.executeProcess(processWithMarkers.getProcessSettings());
        Assert.assertEquals(executionResponse.getProcessStatus().getStatus(), Statuses.SUCCESS);
    }

    @Test
    public void executeCompound_whenIncorrectName_thenEmptyResponse() {
        try {
            final CompoundConfiguration compound = CompoundConfiguration.builder().name("not exist").id(UUID.randomUUID()).build();
            executeCompound(compound);
            Assert.fail("Exception should be on 'executeCompound'");
        } catch (RuntimeException e) {
            Assert.assertEquals("MIA-0053: Compound with name 'not exist' has no processes!", e.getMessage());
        }
    }

    @Test
    public void executeCompound_whenOnlyCompoundName_thenExecuteProcessByDefaultAndNotStopOnFail() {
        final CompoundConfiguration compound = DeserializerConfigBaseTest.getDefaultCompound();
        Switcher sw = new Switcher();
        sw.setName("stopOnFail");
        sw.setDisplay("Stop compound if one of processes is fail");
        sw.setValue(false);
        request.get().setSystemSwitchers(Arrays.asList(sw));
        final LinkedList<ExecutionResponse> responses = executeCompound(compound);
        Assert.assertEquals(12, responses.size());
    }

    @Test
    public void executeCompound_whenOnlyCompoundName_thenExecuteProcessByDefaultAndStopOnPostalPayment() {
        CompoundConfiguration compound = DeserializerConfigBaseTest.getDefaultCompound();
        final LinkedList<ExecutionResponse> responses = executeCompound(compound);
        Assert.assertEquals(8, responses.size());
        Assert.assertEquals(compound.getId(), responses.getFirst().getEntityId());
        Assert.assertEquals("http://atp-mia.com/project/"
                + miaContext.get().getProjectId()
                + "/mia/execution?entityId="
                + compound.getId(), responses.getFirst().getEntityUrl());
    }

    @Test
    public void executeCompound_toSkipCompound_whenReferToInput_NotAvailable() {
        CompoundConfiguration compoundConfiguration = DeserializerConfigBaseTest.getDefaultCompound();
        compoundConfiguration.setReferToInput("toSkip");
        final LinkedList<ExecutionResponse> responses =
                executeCompound(compoundConfiguration);
        Assert.assertEquals(1, responses.size());
        Assert.assertTrue(responses.get(0).getCommandResponse().getDescription().get(0).contains("SKIPPED Compound"));
        Assert.assertEquals("WARNING", responses.get(0).getProcessStatus().getStatus().name());
    }

    @Test
    public void executeCompound_toSkipOneProcess_withProcessReferToInput_WhenInputNotAvailable() {
        CompoundConfiguration compoundConfiguration = DeserializerConfigBaseTest.getDefaultCompound();
        compoundConfiguration.getProcesses().get(0).getProcessSettings().setReferToInput("toSkip");
        final LinkedList<ExecutionResponse> responses =
                executeCompound(compoundConfiguration);
        Assert.assertEquals(8, responses.size());
        Assert.assertTrue(responses.get(0).getCommandResponse().getDescription().get(0).contains("SKIPPED process"));
        Assert.assertNull(responses.get(1).getCommandResponse().getDescription());
        Assert.assertEquals("WARNING", responses.get(0).getProcessStatus().getStatus().name());
        Assert.assertNotEquals("WARNING", responses.get(1).getProcessStatus().getStatus().name());
        compoundConfiguration.getProcesses().get(0).getProcessSettings().setReferToInput(null);
    }

    @Test
    public void executeCompound_toSkipOneProcess_withProcessReferToInput_WhenInputIsAvailableButEmpty() {
        CompoundConfiguration compoundConfiguration = DeserializerConfigBaseTest.getDefaultCompound();
        compoundConfiguration.getProcesses().get(1).getProcessSettings().setReferToInput("SYSdateValue");
        final LinkedList<ExecutionResponse> responses =
                executeCompound(compoundConfiguration);
        Assert.assertEquals(8, responses.size());
        Assert.assertTrue(responses.get(1).getCommandResponse().getDescription().get(0).contains("SKIPPED process"));
        Assert.assertNull(responses.get(0).getCommandResponse().getDescription());
        Assert.assertEquals("WARNING", responses.get(1).getProcessStatus().getStatus().name());
        Assert.assertNotEquals("WARNING", responses.get(0).getProcessStatus().getStatus().name());
        compoundConfiguration.getProcesses().get(1).getProcessSettings().setReferToInput(null);
    }

    @Test
    public void executeCompound_toSkipOneProcess_withProcessReferToInput_WhenInputIsAvailableWithValueFalse() {
        CompoundConfiguration compoundConfiguration = DeserializerConfigBaseTest.getDefaultCompound();
        miaContext.get().getFlowData().addParameter("bill_period", "false");
        compoundConfiguration.getProcesses().get(1).getProcessSettings().setReferToInput("SYSdateValue");
        final LinkedList<ExecutionResponse> responses =
                executeCompound(compoundConfiguration);
        Assert.assertEquals(8, responses.size());
        Assert.assertTrue(responses.get(1).getCommandResponse().getDescription().get(0).contains("SKIPPED process"));
        Assert.assertNull(responses.get(0).getCommandResponse().getDescription());
        Assert.assertEquals("WARNING", responses.get(1).getProcessStatus().getStatus().name());
        Assert.assertNotEquals("WARNING", responses.get(0).getProcessStatus().getStatus().name());
        //Cleanup
        compoundConfiguration.getProcesses().get(1).getProcessSettings().setReferToInput(null);
        miaContext.get().getFlowData().removeParameter("bill_period");
    }

    @Test
    public void executeCompound_toSkipOneProcess_withProcessReferToInput_WhenInputIsAvailableWithValueOtherThanFalse() {
        CompoundConfiguration compoundConfiguration = DeserializerConfigBaseTest.getDefaultCompound();
        miaContext.get().getFlowData().addParameter("bill_period", "notFalse");
        compoundConfiguration.getProcesses().get(1).getProcessSettings().setReferToInput("bill_period");
        final LinkedList<ExecutionResponse> responses =
                executeCompound(compoundConfiguration);
        Assert.assertEquals(8, responses.size());
        Assert.assertNull(responses.get(1).getCommandResponse().getDescription());
        Assert.assertEquals("SUCCESS", responses.get(1).getProcessStatus().getStatus().name());
        Assert.assertNotEquals("WARNING", responses.get(0).getProcessStatus().getStatus().name());
        //Cleanup
        compoundConfiguration.getProcesses().get(1).getProcessSettings().setReferToInput(null);
        miaContext.get().getFlowData().removeParameter("bill_period");
    }

    @Test
    public void executeCompound_whenProcessNameWithCommand_thenToExecuteChanged() {
        final CompoundConfiguration compoundInConfig = DeserializerConfigBaseTest.getDefaultCompound();
        final CompoundConfiguration compoundInRequest = DeserializerConfigBaseTest.getDefaultCompound();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("testValue");
        compoundInRequest.getProcesses().get(1).getProcessSettings().setCommand(Command.builder().values(values).build());
        request.get().setFlowData(miaContext.get().getFlowData());
        request.get().setCompound(CompoundRequest.fromCompoundConfiguration(compoundInRequest));
        miaContext.get().getConfig().setCompounds(Arrays.asList(compoundInConfig));
        miaContext.get().getConfig().setProcesses(compoundInConfig.getProcesses());
        final List<ExecutionResponse> response = compoundService.get().executeCompound(request.get(), null);
        verify(sseEmitterService.get(), times(0)).sendEventWithExecutionResult(any());
        Assert.assertNotNull(response.get(1));
        Assert.assertEquals("echo 1;\ntestValue", response.get(1).getExecutedCommand());
        Assert.assertEquals(compoundInConfig.getId(), response.get(1).getEntityId());
    }

    @Test
    public void executeCompound_whenProcessNameWithSseId_sseEmitterShouldCall() {
        final UUID sseId = UUID.randomUUID();
        String token = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ4MHlzOEJ4N2lncm1BSnRvMkdzWFpnaUlMTnJTQV9aMk12TENTX2RWZjZ3In0.eyJleHAiOjE3MTI4NjQ4MjksImlhdCI6MTcxMjg2MTIyOSwiYXV0aF90aW1lIjoxNzEyODYxMjI5LCJqdGkiOiI2ODQxNzc4Ni0xYzYwLTRmNjQtYWU2Zi1hNGM2MTMyY2I2YjIiLCJpc3MiOiJodHRwczovL2F0cC1rZXljbG9hay1kZXYwNC5hdHAyazgubWFuYWdlZC5uZXRjcmFja2VyLmNsb3VkL2F1dGgvcmVhbG1zL2F0cDIiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiMGJhMmIzYTUtMzU0Ni00N2NjLWE0Y2YtMmZmZmU2ZTU4MzQ4IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZnJvbnRlbmQiLCJub25jZSI6IjMiLCJzZXNzaW9uX3N0YXRlIjoiZGYyOGRjMGQtYWFjMy00Y2QxLWI5OGUtNzJiNWU5Mjk5NGZkIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIkFUUF9TVVBQT1JUIiwib2ZmbGluZV9hY2Nlc3MiLCJBVFBfQURNSU4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6ImRmMjhkYzBkLWFhYzMtNGNkMS1iOThlLTcyYjVlOTI5OTRmZCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6IkFkbWluIEFkbWlub3ZpY2giLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJhZG1pbiIsImdpdmVuX25hbWUiOiJBZG1pbiIsImZhbWlseV9uYW1lIjoiQWRtaW5vdmljaCIsImVtYWlsIjoidGVzdEB0ZXN0In0.B1ZxZ9XR0J2vEnosXriX9-k74K0k-XkVQyLUzTt8LFfZlqVQVDBdRDOxvJw8VbUxLW630oBa-A53jOSFCklWCafbEI9C16WfpXa4zgKkjmKoRdM5O3h18JYcOCwj6Ey1aoMDTAGJb4lSeLEbuGgz3Y_O3nrjUX_0JcXhtUpcqvQm1LUkllz_HFiPiw4sMcx3BPBj0-Ht6ecVjUjQDej0yGH4OrXTxmuWcx-NSQ-1S-hZY_JYGwc0qYXVyH_u4YdTN4Jn6l-IEzGI_GY89ZxyP3RSMrzK9B2-T85NWYK3briZacqJmnYdFPvdz5eCF-o5M5PWUTdPaSPFMOEAWbCrPg";
        sseEmitterService.get().generateAndConfigureEmitter(sseId, token);
        final CompoundConfiguration compound = DeserializerConfigBaseTest.getDefaultCompound();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("testValue");
        compound.getProcesses().get(1).getProcessSettings().setCommand(Command.builder().values(values).build());
        request.get().setFlowData(miaContext.get().getFlowData());
        request.get().setCompound(CompoundRequest.fromCompoundConfiguration(compound));
        miaContext.get().getConfig().setCompounds(Arrays.asList(compound));
        miaContext.get().getConfig().setProcesses(compound.getProcesses());
        LinkedList<ExecutionResponse> responses = compoundService.get().executeCompound(request.get(), sseId);
        for (ExecutionResponse r:responses) {
            verify(sseEmitterService.get(), times(1)).sendEventWithExecutionResult(eq(r));
            verify(sseEmitterService.get(), times(1)).sendEventWithExecutionResult(any(), eq(r));
            assertEquals(sseId, r.getSseId());
        }
    }

    @Test
    public void executeCompound_whenProcessNameWithSseId_sseEmitterShouldCallKafka() {
        final UUID sseId = UUID.randomUUID();
        when(kafkaExecutionFinishProducer.get().isMock()).thenReturn(false);
        when(kafkaExecutionFinishProducer.get().executionFinishEventSend(any())).thenReturn(true);
        final CompoundConfiguration compound = DeserializerConfigBaseTest.getDefaultCompound();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("testValue");
        compound.getProcesses().get(1).getProcessSettings().setCommand(Command.builder().values(values).build());
        request.get().setFlowData(miaContext.get().getFlowData());
        request.get().setCompound(CompoundRequest.fromCompoundConfiguration(compound));
        miaContext.get().getConfig().setCompounds(Arrays.asList(compound));
        miaContext.get().getConfig().setProcesses(compound.getProcesses());
        LinkedList<ExecutionResponse> responses = compoundService.get().executeCompound(request.get(), sseId);
        for (ExecutionResponse r:responses) {
            verify(sseEmitterService.get(), times(1)).sendEventWithExecutionResult(eq(r));
            verify(kafkaExecutionFinishProducer.get(), times(1)).executionFinishEventSend(eq(r));
            assertEquals(sseId, r.getSseId());
        }
    }

    @Test
    public void executeCompound_whenProcessNameWithoutCommand_thenExecuteProcessByDefault() {
        final CompoundConfiguration compound = CompoundConfiguration.builder()
                .id(UUID.randomUUID())
                .name(compoundName)
                .processes(Arrays.asList(
                        ProcessConfiguration.builder().name("SQL_GPARAMS")
                                .processSettings(ProcessSettings.builder()
                                        .command(Command.builder().values(new LinkedHashSet<String>() {{add("some command");}})
                                                .build())
                                        .build())
                                .build()))
                .build();
        executeCompound(compound);
        final CompoundConfiguration expectedCompound = miaContext.get().getConfig().getCompoundByName(compoundName);
        expectedCompound.getProcesses().forEach(p -> Assert.assertEquals(p.getProcessSettings().getCommand().getToExecute(), p.getProcessSettings().getCommand().getValue()));
    }
    //PREREQUSITE TESTS

    @Test
    public void executePrerequisites_OnLocalHost() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final Prerequisite prerequisites = new Prerequisite("COMMAND_ON_LOCALHOST", TEST_SYSTEM_NAME, "echo 123");
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(ImmutableList.of(prerequisites)).command(command).build();
        executeProcess(process);
        verify(executionHelperService.get(), Mockito.times(1)).executeCommandOnLocalHost(eq("echo 123"));
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_OnLocalHost_whenReferToInputName() {
        Map<String, String> params = new HashMap<>();
        params.put(input_name, input_value);
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ArrayList<Prerequisite> prerequisites = new ArrayList<Prerequisite>();
        Prerequisite prerequisite = new Prerequisite("COMMAND_ON_LOCALHOST", TEST_SYSTEM_NAME, "echo 123");
        prerequisite.setReferToInputName(Collections.singletonList(input_name));
        prerequisites.add(prerequisite);
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
        miaContext.get().getFlowData().addParameters(params);
        executeProcess(process);
        verify(executionHelperService.get(), Mockito.times(1)).executeCommandOnLocalHost(eq("echo 123"));
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenNameDefined_thenSaveResultOfSelectToFlowData() {
        final String param = "param";
        Map<String, String> params = new HashMap<>();
        params.put("Do update", "true");
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue :" + param));
        ArrayList<Prerequisite> prerequisites = new ArrayList<>();
        final Prerequisite prerequisite1 = new Prerequisite("SQL", TEST_SYSTEM_NAME, sqlPrerequisiteValue + 1).setReferToInputName(Arrays.asList("Do update"));
        prerequisite1.setName(param + 1);
        final Prerequisite prerequisite2 = new Prerequisite("SQL", TEST_SYSTEM_NAME, sqlPrerequisiteValue + 2).setReferToInputName(Arrays.asList("Do update"));
        prerequisite2.setName(param + 2);
        prerequisites.add(prerequisite1);
        prerequisites.add(prerequisite2);
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
        final DbTable dbTable1 = new DbTable(ImmutableList.of("col1", "col2"), ImmutableList.of(ImmutableList.of("1", "2"), (Arrays.asList("3", "4"))));
        final DbTable dbTable2 = new DbTable(ImmutableList.of("col1", "col2"), ImmutableList.of(ImmutableList.of("3", "4"), (Arrays.asList("1", "2"))));
        SqlResponse sqlResponse1 = new SqlResponse();
        sqlResponse1.setQuery(sqlPrerequisiteValue + 1);
        sqlResponse1.setData(dbTable1);
        sqlResponse1.setRecords(dbTable1.getData().size());
        SqlResponse sqlResponse2 = new SqlResponse();
        sqlResponse2.setQuery(sqlPrerequisiteValue + 2);
        sqlResponse2.setData(dbTable2);
        sqlResponse2.setRecords(dbTable2.getData().size());
        when(sqlService.get().executeCommand(eq(sqlPrerequisiteValue + 1), anyString())).thenReturn(Arrays.asList(new CommandResponse(sqlResponse1)));
        when(sqlService.get().executeCommand(eq(sqlPrerequisiteValue + 2), anyString())).thenReturn(Arrays.asList(new CommandResponse(sqlResponse2)));
        executeProcess(process);
        Assert.assertEquals("1", miaContext.get().getFlowData().getParameters().get(param + 1));
        Assert.assertEquals("3", miaContext.get().getFlowData().getParameters().get(param + 2));
    }

    @Test
    public void executePrerequisites_whenNoConditions() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(false, false)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenNoParamsAndReferPresent_thenNotExecutePrerequisite() {
        miaContext.get().getFlowData().setParameters(null);
        final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue"));
        final ArrayList<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("SQL", TEST_SYSTEM_NAME, sqlPrerequisiteValue).setReferToInputName(Arrays.asList("param")));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(0)).executeCommand(eq(sqlPrerequisiteValue), any());
    }

    @Test
    public void executePrerequisites_whenOneParamAndSeveralRefers_thenExecuteAccordingToParamValue() {
        final String param1 = "param1";
        final String param2 = "param2";
        Map<String, String> params = new HashMap<>();
        params.put(param1, "");
        params.put(param2, param2);
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue"));
        final Prerequisite prerequisite = new Prerequisite("SQL", TEST_SYSTEM_NAME, sqlPrerequisiteValue).setReferToInputName(Arrays.asList(param1, param2));
        final List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(prerequisite);
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(sqlPrerequisiteValue), any());
    }

    @Test
    public void executePrerequisites_whenReferToCommandValue_AndCommandValueIsEqual() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(false, true)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToCommandValue_AndCommandValueNotEqual() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet("other_command_value"));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(false, true)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(0)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals("other_command_value", command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToInputName_AndInputIsFilled() {
        miaContext.get().getFlowData().setParameters(new HashMap<String, String>() {{
            put(input_name, input_value);
        }});
        Input input = new Input(input_name, "type", "value");
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).inputs(Arrays.asList(input)).prerequisites(createPrerequisites(true, false)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToInputName_AndInputNotFilled() {
        Map<String, String> params = new HashMap<>();
        params.put(input_name, "");
        params.put("other_input_name", "input_value");
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(true, false)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(0)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToInputName_AndReferToComValue_AndComValueIsEqual_InputNotFilled() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        miaContext.get().getFlowData().setParameters(new HashMap<String, String>() {{
            put(input_name, "");
            put("other_input_name", "input_value");
        }});
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(true, true)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(0)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToInputName_AndReferToComValue_AndComValueNotEqual_InputNotFilled() {
        Map<String, String> params = new HashMap<>();
        params.put("other_input_name", "input_value");
        params.put(input_name, "");
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet("other_command_value"));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(true, true)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(0)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals("other_command_value", command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToInputName_AndReferToCommandValue_AndCommandValueIsEqual_InputFilled() {
        Map<String, String> params = new HashMap<>();
        params.put(input_name, input_value);
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(true, true)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals(commandValue, command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenReferToInputName_AndReferToCommandValue_AndCommandValueNotEqual_InputFilled() {
        Map<String, String> params = new HashMap<>();
        params.put(input_name, input_value);
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet("other_command_value"));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(createPrerequisites(true, true)).command(command).build();
        executeProcess(process);
        verify(sqlService.get(), Mockito.times(0)).executeCommand(eq(sqlPrerequisiteValue), any());
        Assert.assertEquals("other_command_value", command.getToExecute());
    }

    @Test
    public void executePrerequisites_whenTypeSQL_thenExecutePrerequisitesFirstAndCommandSecond() {
        final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue"));
        ArrayList<Prerequisite> prerequisites = new ArrayList<Prerequisite>();
        prerequisites.add(new Prerequisite("SQL", TEST_SYSTEM_NAME, "update "));
        prerequisites.add(new Prerequisite("SQL", TEST_SYSTEM_NAME, sqlPrerequisiteValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
        executeProcess(process);
        InOrder inOrder = Mockito.inOrder(sqlService.get());
        inOrder.verify(sqlService.get(), Mockito.times(1)).executeCommand(eq("update "), eq(TEST_SYSTEM_NAME));
        inOrder.verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(sqlPrerequisiteValue), eq(TEST_SYSTEM_NAME));
        inOrder.verify(sqlService.get(), Mockito.times(1)).executeCommand(eq(commandValue), eq(TEST_SYSTEM_NAME));
    }

    @Test
    public void executePrerequisites_whenTypeNotSupported_thenThrowException() {
        Assert.assertThrows(PrerequisiteTypeUnsupportedException.class, () -> {
            final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue"));
            ArrayList<Prerequisite> prerequisites = new ArrayList<Prerequisite>();
            prerequisites.add(new Prerequisite("SOAP", TEST_SYSTEM_NAME, sqlPrerequisiteValue));
            final ProcessSettings process =
                    new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
            executeProcess(process);
        });
    }

    @Test
    public void executePrerequisites_whenTypeSSHAndUseValue_commandValueChanged() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final ArrayList<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(new Prerequisite("SSH", TEST_SYSTEM_NAME, sqlPrerequisiteValue));
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(prerequisites).command(command).build();
        executeProcess(process);
        Assert.assertEquals(commandValue, process.getCommand().getToExecute());
    }

    @Test
    public void executePrerequisites_whenTypeSSHAndUseValues_commandValuesChanged() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet(commandValue));
        final Prerequisite prerequisite1 = new Prerequisite("SSH", TEST_SYSTEM_NAME, sqlPrerequisiteValue);
        final Prerequisite prerequisite2 = new Prerequisite("SSH", TEST_SYSTEM_NAME, sqlPrerequisiteValue);
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).prerequisites(ImmutableList.of(prerequisite1, prerequisite2)).command(command).build();
        executeProcess(process);
        Assert.assertEquals(commandValue, process.getCommand().getToExecute());
    }

    @Test
    public void executeProcess_verify_replaceProcessSystems_method_called() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet("commandValue"));
        final List<Validation> validations = new ArrayList<>();
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).command(command).validations(validations).build();
        executeProcess(process);
        verify(executionHelperService.get(), Mockito.times(1)).replaceProcessSystems(eq(process));
    }

    @Test
    public void executeProcess_whenAtpValuesInCommand_thenGetCommandByLabel() {
        final ProcessSettings process = DeserializerConfigBaseTest.getSql().getProcessSettings();
        Assert.assertEquals("select * from gparams where name like '%SYSdate%'", process.getCommand().getToExecute());
        request.get().setCommand("UPDATE");
        executeProcess(process);
        Assert.assertEquals("select * from gparams where name like '%SYSdate%'", process.getCommand().getToExecute());
    }

    @Test
    public void executeProcess_whenCalled_thenSshAndSqlServicesAreCalled() {
        final Command command = new Command("Command", "SSH", TEST_SYSTEM_NAME, listToSet("commandValue"));
        final List<Validation> validations = new ArrayList<>();
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).command(command).validations(validations).build();
        miaContext.get().getFlowData().setParameters(new HashMap<>());
        executeProcess(process);
        verify(sshService.get(), Mockito.times(1)).executeSingleCommand(eq(command));
        verify(sqlService.get(), Mockito.times(1)).executeValidations(eq(validations), any(Command.class));
    }

    @Test
    public void executeProcess_whenSwitcherisPresent_andProcessCommandtypeisSQL() {
        final ProcessSettings process = DeserializerConfigBaseTest.getSqlforswitcher().getProcessSettings();
        Assert.assertNull(process.getPrerequisites());
        ExecutionResponse executionResponse = executeProcessforSwitcher(process);
        Assert.assertEquals(2, executionResponse.getPrerequisites().size());
        Assert.assertEquals(CommandResponse.CommandResponseType.SQL,
                executionResponse.getPrerequisites().get(0).getType());
        Assert.assertEquals(CommandResponse.CommandResponseType.SQL,
                executionResponse.getPrerequisites().get(1).getType());
    }

    @Test
    public void executeProcess_whenSwitcherisPresent_andProcessCommandtypeisSSH() {
        when(sshService.get().executeSingleCommand(any())).thenReturn(new CommandResponse());
        final ProcessSettings process = DeserializerConfigBaseTest.getBgforswitcher().getProcessSettings();
        ExecutionResponse executionResponse = executeProcessforSwitcher(process);
        Assert.assertEquals("export TRACE_LEVEL=FULL\nexport TRACE_ALL=ON\nBG -a \"-a :accountNumber\"",
                executionResponse.getExecutedCommand());
    }

    @Test
    public void executeValidations_addValidationStatus() {
        final String param = "param";
        Map<String, String> params = new HashMap<>();
        params.put("Do update", "true");
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue :" + param));
        LinkedHashMap<String, String> expectedResultForQuery = new LinkedHashMap<>();
        expectedResultForQuery.put("account_num", "123456");
        expectedResultForQuery.put("max_bill_seq", "<10");
        expectedResultForQuery.put("action_dat", "2019-04-19 00:00:00.0");
        LinkedHashMap<String, String> actualResultForQuery = new LinkedHashMap<>();
        actualResultForQuery.put("account_num", "123456");
        actualResultForQuery.put("bill_seq", "1");
        actualResultForQuery.put("max_bill_seq", "8");
        actualResultForQuery.put("action_dat", "2019-04-19 00:00:00.0");
        final Validation validation = new Validation("SQL", TEST_SYSTEM_NAME, sqlPrerequisiteValue).setTableName("ACCOUNTPAYATTRIBUTES").setTableMarker(new TableMarker().setTableRowCount(">0").setExpectedResultForQuery(expectedResultForQuery));
        List<Validation> listValidation = Arrays.asList(validation);
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).command(command).validations(ImmutableList.of(validation)).build();
        final DbTable dbTable = new DbTable(ImmutableList.of("account_num", "bill_seq", "max_bill_seq", "action_dat"), ImmutableList.of(ImmutableList.of("123456", "1", "8", "2019-04-19 00:00:00.0")));
        SqlResponse sqlResponse = new SqlResponse();
        sqlResponse.setQuery(sqlPrerequisiteValue);
        sqlResponse.setData(dbTable);
        sqlResponse.setRecords(dbTable.getData().size());
        List<SqlResponse> validationResponses = Arrays.asList(sqlResponse);
        when(sqlService.get().executeValidations(eq(listValidation), eq(command))).thenReturn(validationResponses);
        ExecutionResponse executionResponse = executeProcess(process);
        Assert.assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        for (SqlResponse validationResponse : executionResponse.getValidations()) {
            for (TableMarkerResult.TableMarkerColumnStatus columnStatus : validationResponse.getTableMarkerResult().getColumnStatuses()) {
                Assert.assertEquals(Statuses.SUCCESS, columnStatus.getStatus());
            }
            List<String> expectedResultTableRow = new ArrayList<>();
            expectedResultTableRow.add("ER");
            expectedResultTableRow.add("123456");
            expectedResultTableRow.add("---");
            expectedResultTableRow.add("<10");
            expectedResultTableRow.add("2019-04-19 00:00:00.0");
            Assert.assertEquals(expectedResultTableRow, validationResponse.getData().getData().get(0));
            List<String> actualResultTableRow = new ArrayList<>();
            actualResultTableRow.add("AR");
            actualResultTableRow.addAll(actualResultForQuery.values());
            Assert.assertEquals(actualResultTableRow, validationResponse.getData().getData().get(1));
        }
    }

    @Test
    public void executeValidations_addValidationStatusforSqlfile() {
        final String param = "param";
        Map<String, String> params = new HashMap<>();
        params.put("Do update", "true");
        miaContext.get().getFlowData().setParameters(params);
        final Command command = new Command("Command", "SQL", TEST_SYSTEM_NAME, listToSet("commandValue :" + param));
        LinkedHashMap<String, String> expectedResultForQuery = new LinkedHashMap<>();
        expectedResultForQuery.put("account_num", "123456");
        expectedResultForQuery.put("max_bill_seq", "<10");
        expectedResultForQuery.put("action_dat", "2019-04-19 00:00:00.0");
        LinkedHashMap<String, String> actualResultForQuery = new LinkedHashMap<>();
        actualResultForQuery.put("account_num", "123456");
        actualResultForQuery.put("bill_seq", "1");
        actualResultForQuery.put("max_bill_seq", "8");
        actualResultForQuery.put("action_dat", "2019-04-19 00:00:00.0");
        File file = new File("src/main/config/project/default/flow/test1.sql");
        Mockito.doReturn(file).when(fileService.get()).getFile(Mockito.anyString());
        final Validation validation = new Validation("SQL", TEST_SYSTEM_NAME, sqlFileValue).setTableName("ACCOUNT").setTableMarker(new TableMarker().setTableRowCount(">0").setExpectedResultForQuery(expectedResultForQuery));
        List<Validation> listValidation = Arrays.asList(validation);
        final ProcessSettings process = new ProcessSettings().toBuilder().name(processName).command(command).validations(ImmutableList.of(validation)).build();
        final DbTable dbTable = new DbTable(ImmutableList.of("account_num", "bill_seq", "max_bill_seq", "action_dat"), ImmutableList.of(ImmutableList.of("123456", "1", "8", "2019-04-19 00:00:00.0")));
        SqlResponse sqlResponse = new SqlResponse();
        sqlResponse.setQuery(sqlPrerequisiteValue);
        sqlResponse.setData(dbTable);
        sqlResponse.setRecords(dbTable.getData().size());
        List<SqlResponse> validationResponses = Arrays.asList(sqlResponse);
        when(sqlService.get().executeValidations(eq(listValidation), eq(command))).thenReturn(validationResponses);
        ExecutionResponse executionResponse = executeProcess(process);
        Assert.assertEquals(Statuses.SUCCESS, executionResponse.getProcessStatus().getStatus());
        for (SqlResponse validationResponse : executionResponse.getValidations()) {
            for (TableMarkerResult.TableMarkerColumnStatus columnStatus : validationResponse.getTableMarkerResult().getColumnStatuses()) {
                Assert.assertEquals(Statuses.SUCCESS, columnStatus.getStatus());
            }
            List<String> expectedResultTableRow = new ArrayList<>();
            expectedResultTableRow.add("ER");
            expectedResultTableRow.add("123456");
            expectedResultTableRow.add("---");
            expectedResultTableRow.add("<10");
            expectedResultTableRow.add("2019-04-19 00:00:00.0");
            Assert.assertEquals(expectedResultTableRow, validationResponse.getData().getData().get(0));
            System.out.println(validationResponse.getData().getData().get(0));
            List<String> actualResultTableRow = new ArrayList<>();
            actualResultTableRow.add("AR");
            actualResultTableRow.addAll(actualResultForQuery.values());
            Assert.assertEquals(actualResultTableRow, validationResponse.getData().getData().get(1));
            System.out.println(validationResponse.getData().getData().get(1));
        }
    }

    @Test
    public void getNextBillDate_whenCalled_delegatesCallToSqlService() {
        String expectedBillDate = "2205";
        String accountNumber = "123";
        when(sqlService.get().getNextBillDate()).thenReturn(expectedBillDate);
        String actualBillDate = executionHelperService.get().getNextBillDate().toString();
        Assert.assertEquals(expectedBillDate, actualBillDate);
    }

    @Test
    public void verify_replaceProcessSystemsMethod_replacesSystems() {
        final Command command = new Command("Command", ":customSystem", TEST_SYSTEM_NAME, listToSet("commandValue"));
        final Validation validation = new Validation("SQL", ":customSystem", sqlPrerequisiteValue);
        final Prerequisite prerequisites = new Prerequisite("SSH", ":customSystem", sqlPrerequisiteValue);
        Input input = new Input(input_name, "type", "Billing System");
        ProcessSettings process = new ProcessSettings().toBuilder().name(processName).command(command).prerequisites(Arrays.asList(prerequisites)).inputs(Arrays.asList(input)).validations(Arrays.asList(validation)).build();
        HashMap<String, String> params = new HashMap<>();
        params.put("customSystem", "Billing System");
        miaContext.get().getFlowData().setParameters(params);
        process = executionHelperService.get().replaceProcessSystems(process);
        Assert.assertEquals(process.getCommand().getSystem(), "Billing System");
        Assert.assertEquals(process.getPrerequisites().get(0).getSystem(), "Billing System");
        Assert.assertEquals(process.getValidations().get(0).getSystem(), "Billing System");
    }

    @Test
    public void zipCommandOutputs_checkLink() {
        final String projId = "default";
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testDataFilePath + "Create_Customer1.sql");
        File file = new File(testDataFilePath + "Create_Customer1.sql");
        Mockito.doReturn(file).when(fileService.get()).getFile(Mockito.anyString());
        Link actualLink = miaContext.get().zipCommandOutputs(projId, filePaths);
        final String name = "Test_Process_\\d+.zip";
        Assert.assertEquals(actualLink.getPath().substring(0, 55), "/rest/downloadFile/" + projectId.get());
    }

    private List<Prerequisite> createPrerequisites(boolean setInputReference, boolean setCommandReference) {
        final List<Prerequisite> prerequisites = new ArrayList<>();
        final Prerequisite sshPrerequisite = new Prerequisite("SSH", "Billing System", sshPrerequisiteValue);
        final Prerequisite sqlPrerequisite = new Prerequisite("SQL", "Billing System", sqlPrerequisiteValue);
        if (setCommandReference) {
            sshPrerequisite.setReferToCommandValue(Arrays.asList(commandValue));
            sqlPrerequisite.setReferToCommandValue(Arrays.asList(commandValue));
        }
        if (setInputReference) {
            sshPrerequisite.setReferToInputName(Arrays.asList(input_name));
            sqlPrerequisite.setReferToInputName(Arrays.asList(input_name));
        }
        prerequisites.add(sshPrerequisite);
        prerequisites.add(sqlPrerequisite);
        return prerequisites;
    }
}
