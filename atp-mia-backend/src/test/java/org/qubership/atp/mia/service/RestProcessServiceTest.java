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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.exceptions.rest.RestFormatNotCorrectException;
import org.qubership.atp.mia.exceptions.rest.RestHeadersIncorrectFormatException;
import org.qubership.atp.mia.exceptions.rest.RestIncorrectEndpointException;
import org.qubership.atp.mia.exceptions.rest.RestNotFoundException;
import org.qubership.atp.mia.exceptions.rest.UnsupportedRestMethodException;
import org.qubership.atp.mia.model.ContentType;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.repo.impl.RestRepository;
import org.qubership.atp.mia.service.execution.RestExecutionHelperService;

@ExtendWith(SkipTestInJenkins.class)
public class RestProcessServiceTest extends ConfigTestBean {

    private Command command;
    private File file;
    private String fileName;
    private String filePath;
    private Rest rest;
    public static final String PROJECT_FOLDER = "." + File.separator + "PROJECT_FOLDER" + File.separator;
    private ThreadLocal<RestRepository> restRepository = new ThreadLocal<>();
    private ThreadLocal<RestExecutionHelperService> restService = new ThreadLocal<>();

    private static void createFile(File file, String fileInput) throws IOException {
        file.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(fileInput);
            bw.flush();
        }
    }

    @AfterEach
    public void cleanLogFile() {
        final File logFile = new File(filePath);
        if (logFile.exists()) {
            logFile.deleteOnExit();
        }
    }

    @BeforeEach
    public void prepareMocks() {
        // construct
        restRepository.set(mock(RestRepository.class));
        restService.set(new RestExecutionHelperService(restRepository.get(), miaContext.get(), miaFileService.get()));
        miaContext.get().getFlowData().addParameter("processName", "test");
        // rest
        rest = Rest.builder().build();
        //command
        command = new Command();
        command.setSystem(testSystem.get().getName());
        command.setRest(rest);
        Path path = Paths.get(PROJECT_FOLDER, projectId.get().toString(),
                ProjectFileType.MIA_FILE_TYPE_UPLOAD.toString(),
                miaContext.get().getFlowData().getSessionId().toString());
        doReturn(path).when(miaContext.get()).getUploadsPath();
        file = miaContext.get().getUploadsPath().resolve(miaContext.get().createFileName(ContentType.json)).toFile();
        doReturn(file).when(miaFileService.get()).getFile(Mockito.anyString());
        fileName = file.getName();
        filePath = file.getPath();
    }

    @Test
    public void when_EndPointIsNotPresentInRestFile_thenError() throws IOException {
        //construct
        String fileInput = "GET\n"
                + "\n"
                + "\n"
                + "\n"
                + "Content-Type: application/json;charset=UTF-8\n"
                + "\n"
                + "bdoy1\n";
        rest.setRestFile(fileName);
        createFile(file, fileInput);
        RestIncorrectEndpointException ex = assertThrows(RestIncorrectEndpointException.class,
                () -> restService.get().sendRestRequest(command));
        assertTrue("MIA-1400", ex.getMessage().contains("MIA-1400"));
    }

    @Test
    public void when_HeaderIsNotPresentInRestFile_thenError() throws IOException {
        //construct
        String fileInput = "GET\n"
                + "\n"
                + "endPoint\n"
                + "\n"
                + "\n"
                + "\n"
                + "bdoy1\n";
        rest.setRestFile(fileName);
        createFile(file, fileInput);
        RestHeadersIncorrectFormatException ex = assertThrows(RestHeadersIncorrectFormatException.class,
                () -> restService.get().sendRestRequest(command));
        assertTrue("MIA-1406", ex.getMessage().contains("MIA-1406"));
    }

    @Test
    public void when_MethodIsNotPresentInRestFile_thenError() throws IOException {
        //construct
        String fileInput = "\n"
                + "\n"
                + "/rest/endpoint/check\n"
                + "\n"
                + "Content-Type: application/json;charset=UTF-8\n"
                + "\n"
                + "bdoy1\n";
        rest.setRestFile(fileName);
        createFile(file, fileInput);
        UnsupportedRestMethodException ex = assertThrows(UnsupportedRestMethodException.class,
                () -> restService.get().sendRestRequest(command));
        assertTrue("MIA-1402", ex.getMessage().contains("MIA-1402"));
    }

    @Test
    public void when_RestFile_thenCommandResponse() throws IOException {
        String fileInput = "GET\n"
                + "\n"
                + "/rest/endpoint/check\n"
                + "\n"
                + "Content-Type: application/json;charset=UTF-8\n"
                + "\n"
                + "body1\n"
                + "body2\n";
        //construct
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setStatusCode("200");
        rest.setRestFile(fileName);
        createFile(file, fileInput);
        // stub
        when(restRepository.get().sendRestRequest(any())).thenReturn(commandResponse);
        CommandResponse result = restService.get().sendRestRequest(command);
        assertNotNull(result);
        assertEquals("200", result.getStatusCode());
    }

    @Test
    public void when_RestFormatNotCorrect_thenError() throws IOException {
        //construct
        String fileInput = "testFile";
        rest.setRestFile(fileName);
        createFile(file, fileInput);
        RestFormatNotCorrectException ex = assertThrows(RestFormatNotCorrectException.class,
                () -> restService.get().sendRestRequest(command));
        assertTrue("MIA-1412", ex.getMessage().contains("MIA-1412"));
    }

    @Test
    public void when_bodyFile_thenCommandResponse() throws IOException {
        String fileInput = "GET\n"
                + "\n"
                + "/rest/endpoint/check\n"
                + "\n"
                + "Content-Type: application/json;charset=UTF-8\n"
                + "\n"
                + "bdoy1\n"
                + "body2\n";
        //construct
        CommandResponse commandResponse = new CommandResponse();
        commandResponse.setStatusCode("200");
        rest.setBodyFile(fileName);
        createFile(file, fileInput);
        // stub
        when(restRepository.get().sendRestRequest(any())).thenReturn(commandResponse);
        CommandResponse result = restService.get().sendRestRequest(command);
        assertNotNull(result);
        assertEquals("200", result.getStatusCode());
    }

    @Test
    public void when_NotRest_thenError() throws IOException {
        //construct
        command.setRest(null);
        RestNotFoundException ex = assertThrows(RestNotFoundException.class,
                () -> restService.get().sendRestRequest(command));
        assertTrue("MIA-1409", ex.getMessage().contains("MIA-1409"));
    }
}
