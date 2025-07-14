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

package org.qubership.atp.mia.integration.utils;

import static org.qubership.atp.mia.TestConstants.JDBC_URL;
import static org.qubership.atp.mia.TestConstants.SQL_HOST;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN;
import static org.qubership.atp.mia.TestConstants.SQL_LOGIN_VALUE;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD;
import static org.qubership.atp.mia.TestConstants.SQL_PASSWORD_VALUE;
import static org.qubership.atp.mia.TestConstants.SSH_HOST;
import static org.qubership.atp.mia.TestConstants.SSH_LOGIN;
import static org.qubership.atp.mia.TestConstants.SSH_LOGIN_VALUE;
import static org.qubership.atp.mia.TestConstants.SSH_PASSWORD;
import static org.qubership.atp.mia.TestConstants.SSH_PASSWORD_VALUE;
import static org.qubership.atp.mia.TestConstants.SSH_TIMEOUT_CONNECT;
import static org.qubership.atp.mia.TestConstants.SSH_TIMEOUT_EXECUTE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestUtils {

    public static Map<String, String> getSshTestParams(String ip) {
        HashMap<String, String> params = new HashMap<>();
        params.put(SSH_HOST, ip);
        params.put(SSH_LOGIN, SSH_LOGIN_VALUE);
        params.put(SSH_PASSWORD, SSH_PASSWORD_VALUE);
        return params;
    }

    public static Map<String, String> getSshTestParams(String ip, int connectionTimeout, int executionTimeout) {
        Map<String, String> params = getSshTestParams(ip);
        params.put(SSH_TIMEOUT_CONNECT, String.valueOf(connectionTimeout));
        params.put(SSH_TIMEOUT_EXECUTE, String.valueOf(executionTimeout));
        return params;
    }

    public static Map<String, String> getSqlTestParams(String ip) {
        HashMap<String, String> params = new HashMap<>();
        params.put(JDBC_URL, ip);
        params.put(SQL_LOGIN, SQL_LOGIN_VALUE);
        params.put(SQL_PASSWORD, SQL_PASSWORD_VALUE);
        return params;
    }

    public static Server preparePostgresServer(String jdbcUrl) {
        Server sqlServer = new Server(Connection.builder().build(), "db");
        sqlServer.getConnection().setParameters(
                new HashMap<String, String>() {{
                    put(SQL_HOST, getHost(jdbcUrl));
                    put(JDBC_URL, jdbcUrl);
                    put(SQL_LOGIN, SQL_LOGIN_VALUE);
                    put(SQL_PASSWORD, SQL_PASSWORD_VALUE);
                }}
        );
        return sqlServer;
    }

    public static List<String> readFile(String pathStr) {
        Path path = Paths.get(pathStr);
        List<String> data = null;
        try (Stream<String> lines = Files.lines(path)) {
            data = lines.collect(Collectors.toList());
        } catch (IOException e) {
            log.error("can't read file [{}]", e.getMessage());
        }
        return data;
    }

    private static String getHost(String jdbcUrl) {
        return jdbcUrl.split(":")[2].replaceAll("//", "");
    }
}
