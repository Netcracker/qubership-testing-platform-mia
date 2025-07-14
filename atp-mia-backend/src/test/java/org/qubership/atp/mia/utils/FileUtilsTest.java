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

package org.qubership.atp.mia.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.model.pot.Link;

import com.google.common.base.Strings;

@ExtendWith(SkipTestInJenkins.class)
public class FileUtilsTest extends ConfigTestBean {

    private static final Pattern projectIdPattern = Pattern.compile("projectId=(.+)&env=.+");
    String query = "projectId=313241243&env=127.0.0.1";
    String pathToFile = "src/test/resources/CommandOutput/outputWithAllMarkers.log";

    private static String nowDate() {
        return new SimpleDateFormat("yyyy-MM-dd'_'HH-mm-ss_z").format(new Date());
    }

    public ExecutionResponse getExecutionResponse(String processName) {
        CommandOutput resp = new CommandOutput(pathToFile, "/home/ext", false, miaContext.get());
        ExecutionResponse body = new ExecutionResponse();
        body.setProcessName(processName);
        body.setExecutedCommand("command");
        body.setCommandResponse(new CommandResponse(resp));
        return body;
    }

    @Test
    public void logProcessIntoFile_expectSuccess() {
        ExecutionResponse body = getExecutionResponse("process");
        String response = logProcessResponseIntoFile(body, query);
        File file = new File(response);
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.isFile());
    }

    @Test
    public void logCompoundIntoFile_expectSuccess() {
        LinkedList<ExecutionResponse> body = new LinkedList<>();
        IntStream.range(1, 4).forEach(x -> body.add(getExecutionResponse("compoundProc1" + x)));
        String response = logProcessResponseIntoFile(body, query);
        File file = new File(response);
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.isDirectory());
    }

    public String logProcessResponseIntoFile(Object body, String queryUrl) {
        String fileName = "unknown";
        Matcher containProjectId = projectIdPattern.matcher(queryUrl);
        String projectId = containProjectId.matches() ? containProjectId.group(1) : "unknownId";
        if (body instanceof ExecutionResponse) {
            ExecutionResponse response = (ExecutionResponse) body;
            File file = getProcessFileName(response.getProcessName(), Optional.empty());
            logProcessResponseIntoFile(response, file);
            fileName = file.getPath();
        } else if (body instanceof LinkedList) {
            LinkedList<ExecutionResponse> responses = (LinkedList<ExecutionResponse>) body;
            File file = getProcessFileName("", Optional.of("compound_" + nowDate()));
            responses.forEach(r -> logProcessResponseIntoFile(r,
                    getProcessFileName(r.getProcessName(), Optional.of("compound" + nowDate()))));
            fileName = file.getParentFile().getPath();
        }
        return new Link(FilenameUtils.separatorsToUnix(fileName), "").getPath();
    }

    private void logProcessResponseIntoFile(ExecutionResponse response, File file) {
        FileUtils.logIntoFile(response.toString(), file);
    }

    private File getProcessFileName(String processName, Optional<String> prefix) {
        String process = Strings.isNullOrEmpty(processName) ? "unknown" : processName;
        String fileName = String.format("logs/%s/responseLog_process-%s_date-%s.txt",
                prefix.orElse(process), process, nowDate());
        File file = miaContext.get().getLogPath().resolve(fileName.replaceAll("[ :]", "_")).toFile();
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
        return file;
    }
}
