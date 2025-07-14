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

import java.util.ArrayList;
import java.util.Arrays;

import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.mia.exceptions.rest.RestFormatNotCorrectException;
import org.qubership.atp.mia.exceptions.rest.RestHeadersIncorrectFormatException;
import org.qubership.atp.mia.exceptions.rest.RestIncorrectEndpointException;
import org.qubership.atp.mia.exceptions.rest.RestNotFoundException;
import org.qubership.atp.mia.exceptions.rest.UnsupportedRestMethodException;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.repo.impl.RestRepository;
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
public class RestExecutionHelperService {

    private final RestRepository restRepository;
    private final MiaContext miaContext;
    private final MiaFileService miaFileService;

    /**
     * Sends rest request.
     */
    @AtpJaegerLog()
    public CommandResponse sendRestRequest(Command command) {
        if (command.getRest() == null) {
            throw new RestNotFoundException();
        }
        if (!Strings.isNullOrEmpty(command.getRest().getRestFile())) {
            String restFileName = miaContext.evaluate(command.getRest().getRestFile());
            final String[] restQuery = readRestFile(restFileName)
                    .split("\\r?\\n\\r?\\n");
            log.trace("Read restQuery size [{}]\n req: [{}]", restQuery.length, restQuery);
            if (restQuery.length < 3) {
                throw new RestFormatNotCorrectException();
            }
            if (!Strings.isNullOrEmpty(restQuery[0])) {
                command.getRest().setMethod(miaContext.evaluate(restQuery[0]));
            } else {
                throw new UnsupportedRestMethodException("Null or Empty REST method in Rest File");
            }
            if (!Strings.isNullOrEmpty(restQuery[1])) {
                command.getRest().setEndpoint(miaContext.evaluate(restQuery[1]));
            } else {
                throw new RestIncorrectEndpointException("Null or Empty Endpoint in RestFile");
            }
            if (!Strings.isNullOrEmpty(restQuery[2])) {
                command.getRest().setHeaders(miaContext.evaluate(restQuery[2]));
            } else {
                throw new RestHeadersIncorrectFormatException("Empty Header");
            }
            if (restQuery.length > 3) {
                final ArrayList<String> body = new ArrayList<>(Arrays.asList(restQuery).subList(3, restQuery.length));
                command.getRest().setBody(
                        miaContext.evaluate(body.toString().substring(1, body.toString().length() - 1)));
            }
        } else if (!Strings.isNullOrEmpty(command.getRest().getBodyFile())) {
            String bodyFileName = miaContext.evaluate(command.getRest().getBodyFile());
            command.getRest().setBody(miaContext.evaluate(readRestFile(bodyFileName)));
        }
        return restRepository.sendRestRequest(command);
    }

    private String readRestFile(String fileName) {
        return FileUtils.readFile(miaFileService.getFile(fileName).toPath());
    }
}
