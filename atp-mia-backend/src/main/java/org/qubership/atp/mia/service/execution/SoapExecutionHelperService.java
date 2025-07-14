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

package org.qubership.atp.mia.service.execution;

import java.nio.file.Files;
import java.nio.file.Path;

import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.mia.exceptions.soap.SoapNotFoundException;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.repo.impl.SoapRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SoapExecutionHelperService {

    private final SoapRepository soapRepository;
    private final MiaContext miaContext;
    private final MiaFileService miaFileService;

    /**
     * Sends soap request.
     */
    @AtpJaegerLog()
    public CommandResponse sendSoapRequest(Command command) {
        if (command.getSoap() == null) {
            throw new SoapNotFoundException();
        }
        if (!Strings.isNullOrEmpty(command.getSoap().getRequestFile())) {
            String requestFile = miaContext.evaluate(command.getSoap().getRequestFile());

            Path uploadsPath = miaContext.getUploadsPath().resolve(requestFile).normalize();
            Path projectPath = miaContext.getProjectFilePath().resolve(requestFile).normalize();
            Path resolvedPath = null;
            //String internalPathFile = miaContext.getUploadsPath().resolve(requestFile).toString();
            /*if (Files.notExists(Paths.get(internalPathFile))) {
                internalPathFile = miaContext.getProjectFilePath().resolve(requestFile).toString();
                internalPathFile = internalPathFile.replace("//", "/").replace("\\\\", "\\");
                if (Files.notExists(Paths.get(internalPathFile))) {
                    miaFileService.getFile(internalPathFile);
                }
            }
            command.getSoap().setRequest(miaContext.evaluate(FileUtils.readFile(Paths.get(internalPathFile))));
             */

            if (Files.exists(uploadsPath)) {
                resolvedPath = uploadsPath;
            } else if (Files.exists(projectPath)) {
                resolvedPath = projectPath;
            } else {
                //Try to download or fetch the file
                resolvedPath = projectPath;
                miaFileService.getFile(resolvedPath.toString());

                // Re-check if the file was created successfully
                if (!Files.exists(resolvedPath)) {
                    throw new RuntimeException("File not found even after attempting to fetch: " + resolvedPath);
                }
            }
            try {
                String fileContent = FileUtils.readFile(resolvedPath);
                command.getSoap().setRequest(miaContext.evaluate(fileContent));
            } catch (SecurityException e) {
                throw new RuntimeException("Failed to read SOAP request file securely: " + resolvedPath, e);
            }
        } else {
            command.getSoap().setRequest(miaContext.evaluate(command.getSoap().getRequest()));
        }
        return soapRepository.sendSoapRequest(command);
    }
}
