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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

public class ConnectionPropsTest {

    private String hostname = "qaapp1234cn";
    private int port = 22;
    private String username = "username";
    private String password = "password";
    private String key;
    private String passphrase;
    private boolean pty;
    private int timeoutConnect = 30;
    private int timeoutExecute;
    private long timeOutFileDownload;
    private int channelsPerSession;
    private String sshServerKexAlgorithms;

    private ConnectionProps connectionProps;

    @BeforeEach
    public void initialize() {
        connectionProps = new ConnectionProps(hostname, port, username, password, key, passphrase, pty,
                timeoutConnect, timeoutExecute, timeOutFileDownload, channelsPerSession, sshServerKexAlgorithms);
    }

    @Test
    public void TestConnectionFullInfo() {
        Assert.assertTrue(connectionProps.fullInfo().contains(hostname));
        Assert.assertTrue(connectionProps.fullInfo().contains(username));
        Assert.assertTrue(connectionProps.fullInfo().contains(""+timeoutConnect));
        Assert.assertFalse(connectionProps.fullInfo().contains("Password"));
    }

}
