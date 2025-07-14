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

package org.qubership.atp.mia.model.environment;

import static org.qubership.atp.mia.integration.utils.TestUtils.getSshTestParams;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ServerTest {

    public static Stream<Arguments> serverAddresses() {
        return Stream.of(
                Arguments.of("127.0.0.1:73", 73),
                Arguments.of("127.0.0.1", 22),
                Arguments.of("localhost", 22),
                Arguments.of("someDnsName.domainName.com:22", 22),
                Arguments.of("someDnsName.domainName.com", 22),
                Arguments.of("127.0.0.1:32577", 32577)
        );
    }

    @ParameterizedTest(name = "{index}:  host {0}, ip {1}))")
    @MethodSource("serverAddresses")
    public void testSshPortExtraction(String sshHost, int sshPort) {
        Server server = new Server(new Connection(), "ssh");
        server.getConnection().setParameters(getSshTestParams(sshHost));
        Assert.assertEquals(sshPort, server.getPort());
    }
}
