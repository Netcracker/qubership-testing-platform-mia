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

import java.util.Objects;
import java.util.StringJoiner;

import lombok.Data;

@Data
public class ConnectionProps {

    private final String hostname;
    private final int port;
    private final String username;
    private final String password;
    private final String key;
    private final String passphrase;
    private final boolean pty;
    private final int timeoutConnect;
    private final int timeoutExecute;
    private final long timeOutFileDownload;
    private final int channelsPerSession;
    private final String sshServerKexAlgorithms;

    /**
     * Constructor with parameters.
     */
    public ConnectionProps(String hostname, Integer port, String username, String password, String key,
                           String passphrase, boolean pty, int timeoutConnect, int timeoutExecute,
                           long timeOutFileDownload, int channelsPerSession, String sshServerKexAlgorithms) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.key = key;
        this.passphrase = passphrase;
        this.pty = pty;
        this.timeoutConnect = timeoutConnect;
        this.timeoutExecute = timeoutExecute;
        this.timeOutFileDownload = timeOutFileDownload;
        this.channelsPerSession = channelsPerSession;
        this.sshServerKexAlgorithms = sshServerKexAlgorithms;
    }

    /**
     * Create ConnectionProps for ssh.
     */
    public static ConnectionProps forSsh(Server server) {
        return new ConnectionProps(server.getHost(), server.getPort(), server.getUser(),
                server.getPass(), server.getKey(), server.getPassPhrase(),
                server.getPty(), server.getTimeoutConnect(), server.getTimeoutExecute(),
                server.getTimeOutFileDownload(), server.getChannelsPerSession(), server.getSshServerKexAlgorithms());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionProps that = (ConnectionProps) o;
        return Objects.equals(hostname, that.hostname)
                && Objects.equals(port, that.port)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(passphrase, that.passphrase)
                && Objects.equals(key, that.key)
                && Objects.equals(pty, that.pty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port, username, password, passphrase, key, pty);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "[", "]")
                .add("hostname='" + hostname + "'")
                .add("port='" + port + "'")
                .add("username='" + username + "'")
                .toString();
    }

    /**
     * Gets full information about connection.
     *
     * @return full information about connection
     */
    public String fullInfo() {
        return new StringJoiner(", ", "[", "]")
                .add("hostname='" + hostname + "'")
                .add("port='" + port + "'")
                .add("username='" + username + "'")
                .add("pty=" + pty)
                .add("timeoutConnect=" + timeoutConnect)
                .add("timeoutExecute=" + timeoutExecute)
                .toString();
    }
}
