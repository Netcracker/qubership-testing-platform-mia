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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.request.CompoundRequest;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.repo.impl.ProcessStatusRepository;
import org.qubership.atp.mia.service.execution.CompoundService;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.qubership.atp.mia.service.execution.RecordingSessionsService;
import org.qubership.atp.mia.service.execution.RestExecutionHelperService;
import org.qubership.atp.mia.service.execution.SoapExecutionHelperService;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.qubership.atp.mia.service.execution.SshExecutionHelperService;
import org.qubership.atp.mia.service.execution.TestDataService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;

@ExtendWith(SkipTestInJenkins.class)
public abstract class ProcessServiceBaseTest extends ConfigTestBean {

    protected static final LinkedHashSet<String> nullSet = null;
    protected final ThreadLocal<ExecutionRequest> request = new ThreadLocal<>();
    protected final ThreadLocal<MiaFileService> fileService = new ThreadLocal<>();
    protected final ThreadLocal<SqlExecutionHelperService> sqlService = new ThreadLocal<>();
    protected final ThreadLocal<SshExecutionHelperService> sshService = new ThreadLocal<>();
    protected final ThreadLocal<ProcessService> executionHelperService = new ThreadLocal<>();
    protected final ThreadLocal<CompoundService> compoundService = new ThreadLocal<>();
    protected final ThreadLocal<TestDataService> testDataHelperService = new ThreadLocal<>();

    @BeforeEach
    public void beforeExecutionHelperServiceBaseTest() {
        request.set(ExecutionRequest.builder().build());
        sshService.set(mock(SshExecutionHelperService.class));
        sqlService.set(mock(SqlExecutionHelperService.class));
        fileService.set(mock(MiaFileService.class));
        testDataHelperService.set(mock(TestDataService.class));
        executionHelperService.set(spy(
                new ProcessService(
                        miaContext.get(),
                        fileService.get(),
                        sshService.get(),
                        testDataHelperService.get(),
                        sqlService.get(),
                        mock(RestExecutionHelperService.class),
                        mock(SoapExecutionHelperService.class),
                        mock(MetricsAggregateService.class),
                        mock(RecordingSessionsService.class),
                        gridFsService.get(),
                        spy(new ProcessStatusRepository(miaContext.get())),
                        "", "http://atp-mia.com/project/%s/mia/execution?entityId=%s",
                        sseEmitterService.get()
                )));
        compoundService.set(spy(
                new CompoundService(
                        miaContext.get(),
                        executionHelperService.get(),
                        mock(MetricsAggregateService.class),
                        "http://atp-mia.com/project/%s/mia/execution?entityId=%s",
                        sseEmitterService.get()
                )));
        CommandResponse commandResponse = new CommandResponse();
        doReturn(commandResponse).when(sshService.get()).executeSingleCommand(any());
        doReturn(commandResponse).when(sshService.get()).executeCommandAndTransferFileOnServer(any());
        doReturn(commandResponse).when(sshService.get()).executeCommandAndDownloadFilesFromServer(any());
        doReturn(commandResponse).when(sshService.get()).executeCommandAndCheckFileOnServer(any());
        doReturn(Arrays.asList(commandResponse)).when(sqlService.get()).executeCommand(anyString(), anyString());
    }

    protected LinkedList<ExecutionResponse> executeCompound(CompoundConfiguration compound) {
        request.get().setFlowData(miaContext.get().getFlowData());
        request.get().setCompound(CompoundRequest.fromCompoundConfiguration(compound));
        miaContext.get().getConfig().setCompounds(Arrays.asList(compound));
        miaContext.get().getConfig().setProcesses(compound.getProcesses());
        return compoundService.get().executeCompound(request.get(), null);
    }

    protected ExecutionResponse executeProcess(ProcessSettings processSettings) {
        ProcessConfiguration processConfiguration = ProcessConfiguration.builder()
                .id(UUID.randomUUID())
                .name(processSettings.getName())
                .processSettings(processSettings)
                .build();
        miaContext.get().getConfig().setProcesses(Arrays.asList(processConfiguration));
        request.get().setFlowData(miaContext.get().getFlowData());
        request.get().setProcess(processSettings.getName());
        return executionHelperService.get().executeProcess(request.get(), null);
    }

    protected ExecutionResponse executeProcessforSwitcher(ProcessSettings process) {
        final LinkedList<ProcessConfiguration> processes = new LinkedList();
        ProcessConfiguration processConfig = new ProcessConfiguration();
        processConfig.setId(UUID.randomUUID());
        processConfig.setName(process.getName());
        Switcher sw1 = new Switcher();
        sw1.setValue(true);
        sw1.setActionType("SQL");
        sw1.setActionTrue("commentsysdate");
        sw1.setName(SwitcherSQL1);
        Switcher sw2 = new Switcher();
        sw2.setValue(false);
        sw2.setActionType("SQL");
        sw2.setActionFalse("Uncomment sysdate");
        sw2.setName(SwitcherSQL2);
        Switcher sw3 = new Switcher();
        sw3.setValue(true);
        sw3.setActionType("SSH");
        sw3.setActionTrue("export TRACE_LEVEL=FULL\nexport TRACE_ALL=ON");
        sw3.setName("fullTraceMode");
        request.get().setSwitchers(Arrays.asList(sw1, sw2, sw3));
        request.get().setFlowData(miaContext.get().getFlowData());
        processConfig.setProcessSettings(process);
        processes.add(processConfig);
        miaContext.get().getConfig().setProcesses(processes);
        request.get().setProcess(process.getName());
        return executionHelperService.get().executeProcess(request.get(), null);
    }
}
