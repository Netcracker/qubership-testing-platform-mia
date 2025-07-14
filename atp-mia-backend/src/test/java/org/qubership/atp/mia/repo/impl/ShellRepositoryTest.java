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

package org.qubership.atp.mia.repo.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.mia.model.Constants.CustomParameters.ACCOUNT_NUMBER;
import static org.qubership.atp.mia.model.Constants.CustomParameters.FULL_TRACE_MODE;
import static org.qubership.atp.mia.model.Constants.CustomParameters.GENEVA_DATE;
import static org.qubership.atp.mia.model.Constants.CustomParameters.WORKING_DIRECTORY;
import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.repo.impl.pool.ssh.SshSessionPool;

@ExtendWith(SkipTestInJenkins.class)
public class ShellRepositoryTest extends ConfigTestBean {

    private static final String fileToUse = "src/test/config/SqlExecutionHelperServiceTest.sql";
    private static final String systemName = "Billing System";
    private static final String accountNumber = "1234567890";
    private static final String EXPORT_GENEVA_DATE = "exportGenevaDate";
    private Command command;
    private final ThreadLocal<ShellRepository> repository = new ThreadLocal<>();
    private final ThreadLocal<SshConnectionManager> sshConnectionManager = new ThreadLocal<>();
    private final ThreadLocal<SshSessionPool> sshSessionPool = new ThreadLocal<>();
    private final ThreadLocal<Map<String, String>> prefixes = new ThreadLocal<>();

    @BeforeEach
    public void beforeShellRepositoryTest() {
        sshSessionPool.set(spy(new SshSessionPool("300", "30000", miaContext.get())));
        sshConnectionManager.set(mock(SshConnectionManager.class));
        command = new Command("name", "SSH", systemName,
                listToSet("echo :param1", "ls -l"));
        prefixes.set(miaContext.get().getConfig()
                .getCommonConfiguration()
                .getCommandShellPrefixes()
                .stream().filter(p -> p.getSystem().equals(systemName)).findFirst().get().getPrefixes());
        doReturn(sshConnectionManager.get()).when(sshSessionPool.get()).getConnection(any(), any());
        when(sshConnectionManager.get().runCommand(anyString())).thenReturn("output of commands");
        doReturn(sshConnectionManager.get()).when(sshSessionPool.get()).getConnection(Mockito.any(), Mockito.any());
        repository.set(new ShellRepository(miaContext.get(), sshSessionPool.get(), metricsService));
    }

    @Test
    public void executeAndGetLog_whenCommandOk() throws IOException {
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter(ACCOUNT_NUMBER, accountNumber);
        flowData.addParameter(WORKING_DIRECTORY, "");
        flowData.addParameter("param1", "value1");
        flowData.addParameter("processName", "Process Name");
        repository.get().executeAndGetLog(command);
        final String startMatcher = "^" + String.format(prefixes.get().get(ACCOUNT_NUMBER.toString()), accountNumber) + "\n";
        verify(sshConnectionManager.get()).getFileFromServer(matches("^/" + "ProcessName_" + accountNumber + "_\\w+.log"),
                eq(""));
        verify(sshConnectionManager.get(), times(1)).runCommand(
                matches(startMatcher + "echo value1 > /ProcessName_" + accountNumber + "_\\w+.log 2>&1"));
        flowData.addParameter(WORKING_DIRECTORY, "/");
        flowData.addParameter(GENEVA_DATE, "010101");
        repository.get().executeAndGetLog(command);
        verify(sshConnectionManager.get(), times(1)).runCommand(
                matches(startMatcher + String.format(prefixes.get().get(GENEVA_DATE.toString()), "010101")
                        + "\n" + String.format(prefixes.get().get(WORKING_DIRECTORY.toString()), "/")
                        + "\necho value1 > /ProcessName_" + accountNumber + "_\\w+.log 2>&1"));
        flowData.addParameter(GENEVA_DATE, null);
        flowData.addParameter(WORKING_DIRECTORY, "/TEST");
        repository.get().executeAndGetLog(command);
        verify(sshConnectionManager.get(), times(1)).runCommand(
                matches(startMatcher + String.format(prefixes.get().get(WORKING_DIRECTORY.toString()), "/TEST")
                        + "\necho value1 > /TEST/ProcessName_" + accountNumber + "_\\w+.log 2>&1"));
        flowData.addParameter(FULL_TRACE_MODE, "true");
        flowData.addParameter(WORKING_DIRECTORY, "/TEST/");
        repository.get().executeAndGetLog(command);
        verify(sshConnectionManager.get(), times(1)).runCommand(
                matches(startMatcher
                        + String.format(prefixes.get().get(WORKING_DIRECTORY.toString()), "/TEST/")
                        + "\n" + String.format(prefixes.get().get(FULL_TRACE_MODE.toString()), "true")
                        + "\necho value1 > /TEST/ProcessName_" + accountNumber + "_\\w+.log 2>&1"));
        final File file = new File(fileToUse);
        String workingDir = flowData.getCustom(WORKING_DIRECTORY, miaContext.get());
        Mockito.when(sshConnectionManager.get().getFileFromServer(anyString(),
                eq(workingDir))).thenReturn(file);
/*        doReturn(file).when(sshConnectionManager.get()).getFileFromServer(anyString(),
                eq(workingDir));*/
        Assert.assertEquals(Files.readAllLines(Paths.get(file.getPath()), Charset.forName("UTF-8")),
                repository.get().executeAndGetLog(command).getCommandOutputs().get(0).contentFromFile());
        flowData.removeParameter(FULL_TRACE_MODE.toString());
        flowData.removeParameter(GENEVA_DATE.toString());
        flowData.removeParameter(ACCOUNT_NUMBER.toString());
        flowData.removeParameter(WORKING_DIRECTORY.toString());
    }

    @Test
    public void transferFileOnServer() {
        final Command command = new Command("name", "SSH", systemName, listToSet("echo :param1", "ls -l"));
        repository.get().transferFileOnServer(command, fileToUse, "pathToUpload");
        verify(sshConnectionManager.get(), times(1)).transferFileOnServer(eq(fileToUse), eq("pathToUpload"));
    }

    @Test
    public void createFileName() {
        final Command testFileNameCommand = new Command("name", "SSH", systemName,
                listToSet("echo :param1", "ls -l"));
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter("processName", "Process Name");
        flowData.addParameter("param1", "value1");
        repository.get().executeAndGetLog(testFileNameCommand);
        verify(sshConnectionManager.get(), times(1)).runCommand(
                matches("^echo value1 > /ProcessName_\\w+.log 2>&1"));
        testFileNameCommand.setLogFileNameFormat("Custom_logFileName_format_:processName.log");
        repository.get().executeAndGetLog(testFileNameCommand);
        verify(sshConnectionManager.get(), times(1)).runCommand(
                matches("^echo value1 > /Custom_logFileName_format_ProcessName.log 2>&1"));
    }

    @Test
    public void genevaDateSetTrueTest() {
        String exportGenevaDate = "true";
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter("processName", "Process Name");
        flowData.addParameter(WORKING_DIRECTORY, "/");
        flowData.addParameter(EXPORT_GENEVA_DATE, exportGenevaDate);
        repository.get().executeAndGetLog(command);
        verify(sshConnectionManager.get(), times(1)).runCommand(
                startsWith(String.format(prefixes.get().get(WORKING_DIRECTORY.toString()), "/"))
                        + "\n" + String.format(prefixes.get().get(EXPORT_GENEVA_DATE), exportGenevaDate));
    }

    @Test
    public void genevaDateIncorrectValues() {
        String[] exportGenevaDateArr = new String[]{"false", "", "null", "somevalue", "124142", null};
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter("processName", "Process Name");
        flowData.addParameter(WORKING_DIRECTORY, "/");
        int execCount = 1;
        for (String exportGenDate : exportGenevaDateArr) {
            flowData.addParameter(EXPORT_GENEVA_DATE, exportGenDate);
            repository.get().executeAndGetLog(command);
            verify(sshConnectionManager.get(), times(execCount++)).runCommand(
                    startsWith(String.format(prefixes.get().get(WORKING_DIRECTORY.toString()), "/")));
        }
    }
}
