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

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.qubership.atp.mia.exceptions.macrosandevaluations.IncorrectEnvironmentVariableFormatException;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.repo.ContextRepository;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class EnvironmentVariableUtils {

    private static ContextRepository contextRepositoryStatic;
    private final ContextRepository contextRepository;

    /**
     * Evaluates the environment variable value.
     * Example of Environment variable is "${ENV.Billing System.DB.db_type}"
     * Whereas its syntax is "{ENV.SYSTEM_NAME.CONNECTION_NAME.ParameterName}"
     *
     * @param text String comes in above format (Pre-Check for format happens at calling method)
     * @return evaluated value if found something otherwise returns null
     */
    public static String evaluateEnvironmentVariable(String text) {
        text = text.substring(2, text.length() - 1);
        String[] textArray = text.split("\\.");
        if (textArray.length != 4) {
            throw new IncorrectEnvironmentVariableFormatException("${" + text + "}");
        }
        Optional<System> system = contextRepositoryStatic.getContext().getEnvironment()
                .getSystems().stream().filter(sys -> sys.getName().equalsIgnoreCase(textArray[1])).findFirst();
        if (system.isPresent()) {
            Optional<Connection> connection = system.get().getConnections().stream().filter(c ->
                    c.getName().equalsIgnoreCase(textArray[2])).findFirst();
            if (connection.isPresent()) {
                return connection.get().getParameters().get(textArray[3]);
            }
        }
        return null;
    }

    /**
     * Initialize static variables.
     */
    @PostConstruct
    public void init() {
        contextRepositoryStatic = contextRepository;
    }
}
