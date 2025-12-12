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

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static final int MILLISECONDS = 1000;
    private static final int CHANNELS_PER_SESSION = 10;

    private final Connection connection;
    private final String name;

    public Server(Connection connection, String name) {
        this.name = name;
        this.connection = connection;
    }

    public String getName() {
        return name;
    }

    public String getUser() {
        return getProperty(name + "_login");
    }

    public String getPass() {
        return getProperty(name + "_password");
    }

    public String getKey() {
        return getProperty(name + "_key");
    }

    public String getPassPhrase() {
        return getProperty("passphrase");
    }

    /**
     * Get pty from environment. If not defined or incorrect value then 'true'.
     *
     * @return pty from environment. {@code true} if not defined or incorrect value
     */
    public boolean getPty() {
        final String pty = getProperty(name + "_pty");
        if (pty != null) {
            try {
                return Boolean.parseBoolean(pty);
            } catch (Exception e) {
                //nothing
            }
        }
        return true;
    }

    /**
     * Get timeout connect from environment. If not defined or incorrect value then 1 minute.
     *
     * @return timeout connect from environment. 1 minute if not defined or incorrect value
     */
    public int getTimeoutConnect() {
        return getTimeout("connect", 60 * MILLISECONDS, 60000);
    }

    /**
     * Get timeout connect from environment. If not defined or incorrect value then 1 minute.
     *
     * @return timeout connect from environment. defaultValue in milliseconds if not defined or incorrect value
     */
    public int getTimeoutConnect(int defaultValue, int maxValue) {
        return getTimeout("connect", defaultValue * MILLISECONDS, maxValue * MILLISECONDS);
    }

    /**
     * Get timeout execute from environment. If not defined or incorrect value then 60 minute.
     *
     * @return timeout execute from environment. 60 minute if not defined or incorrect value
     */
    public int getTimeoutExecute() {
        return getTimeout("execute", 60 * 60 * MILLISECONDS, 3600000);
    }

    /**
     * Get timeout execute from environment. If not defined or incorrect value then 60 minute.
     *
     * @return timeout execute from environment. defaultValue in milliseconds if not defined or incorrect value
     */
    public int getTimeoutExecute(int defaultValue, int maxValue) {
        return getTimeout("execute", defaultValue * MILLISECONDS, maxValue * MILLISECONDS);
    }

    /**
     * Get Wait timeout for next trial in case of fileDownload from Server failed.
     *
     * @return timeout in millSeconds to wait for next retrial
     */
    public long getTimeOutFileDownload() {
        String timeOut = getProperty(name + "_timeOutFileDownload");
        long duration = 0;
        if (timeOut != null) {
            try {
                return (long) Float.parseFloat(timeOut);
            } catch (Exception e) {
                return duration;
            }
        } else {
            return duration;
        }
    }

    public String getSshServerKexAlgorithms() {
        return getProperty(name + "_kex");
    }

    /**
     * Get property from connection.
     *
     * @param key parameter key
     * @return property value, NULL in case connection or parameter doesn't exist
     */
    public String getProperty(String key) {
        if (connection != null && connection.getParameters() != null) {
            return connection.getParameters().get(key);
        } else {
            return null;
        }
    }

    public Map<String, String> getProperties() {
        return connection.getParameters();
    }

    public String getHostFull() {
        return getProperty(name + "_host");
    }

    /**
     * Gets host from property.
     *
     * @return host, NullPointerException otherwise
     */
    public String getHost() {
        Pattern pattern = Pattern.compile("([^:^]*)(:\\d*)?(.*)?");
        Matcher matcher = pattern.matcher(getHostFull());
        matcher.find();
        return matcher.group(1);
    }

    /**
     * Gets port from host property,
     * for e.g. 127.0.0.1:24, then port will be 24.
     * If port not present in host property then default 22 will be returned.
     *
     * @return int port value.
     */
    public int getPort() {
        String host = getHostFull();
        int port = 22;
        if (host.contains(":")) {
            String[] arr = host.split(":");
            try {
                port = Integer.parseInt(arr[arr.length - 1]);
            } catch (NumberFormatException ignore) {
                LOGGER.debug("use default port 22 for host [{}]", host);
            }
        }
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Server server = (Server) o;
        return Objects.equals(connection, server.connection)
                && Objects.equals(name, server.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection, name);
    }

    /**
     * Get timeout from environment. If not defined or incorrect value then {@code defaultValue}.
     *
     * @param timeoutPostfix postfix of timeout
     * @param defaultValue   default value to return
     * @return timeout from environment. {@code defaultValue} if not defined or incorrect value
     */
    private int getTimeout(String timeoutPostfix, int defaultValue, int maxValue) {
        final String timeout = getProperty(name + "_timeout_" + timeoutPostfix);
        if (timeout != null) {
            try {
                int t = Integer.parseInt(timeout);
                return t > maxValue ? maxValue : Math.max(t, 0);
            } catch (Exception e) {
                //nothing
            }
        }
        return defaultValue;
    }

    /**
     * Returns number of channels per session for one Ssh server.
     * When amount of channels exceeds this value the next channel will be queued.
     *
     * @return int value.
     */
    public int getChannelsPerSession() {
        String valName = "_channelsPerSession";
        return parseIntOrDefault(getProperty(name + valName), CHANNELS_PER_SESSION, valName);
    }

    private static int parseIntOrDefault(String valueToParse, int defaultValue, String valueName) {
        int tempValue;
        try {
            tempValue = Integer.parseInt(valueToParse);
        } catch (NumberFormatException e) {
            tempValue = defaultValue;
            LOGGER.trace("Error can't parse {} value [{}], use the standard [{}]",
                    valueName, valueToParse, defaultValue);
        }
        return tempValue;
    }

    public Connection getConnection() {
        return connection;
    }

    @Override
    public String toString() {
        return "Server{name='" + name + ", host=" + getHostFull() + '}';
    }

    /**
     * Type of Connection.
     */
    @Getter
    public enum ConnectionType {
        SSH("ssh"), DB("db"), HTTP("http");

        private final String type;

        ConnectionType(String type) {
            this.type = type;
        }

    }
}
