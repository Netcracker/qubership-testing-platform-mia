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

package org.qubership.atp.mia;

public class TestConstants {
    public static final String SYS_DATE_VALUE = "20200723";

    public static final String SSH_HOST = "ssh_host";
    public static final String SSH_PORT = "ssh_port";
    public static final String SSH_LOGIN = "ssh_login";
    public static final String SSH_PASSWORD = "ssh_password";
    public static final String SSH_TIMEOUT_EXECUTE = "ssh_timeout_execute";
    public static final String SSH_TIMEOUT_CONNECT = "ssh_timeout_connect";

    public static final String SSH_LOGIN_VALUE = "root";
    public static final String SSH_PASSWORD_VALUE = "test_password";

    public static final String JDBC_URL = "jdbc_url";
    public static final String SQL_HOST = "db_host";
    public static final String SQL_LOGIN = "db_login";
    public static final String SQL_PASSWORD = "db_password";

    public static final String SQL_LOGIN_VALUE = "mia";
    public static final String SQL_PASSWORD_VALUE = "mia";

    public static final String CASSANDRA_HOST = "db_host";
    public static final String CASSANDRA_PORT = "db_port";
    public static final String CASSANDRA_DB_TYPE = "db_type";
    public static final String CASSANDRA_DB_VALUE = "CASSANDRA";

    public static final String SCHEMA = "schema";
    public static final String CASSANDRA_SCHEMA = "smart";
    public static final String CASSANDRA_QUERY = "SELECT now() FROM system.local;";
    public static final String POSTGRESQL_QUERY = "SELECT NOW();";
}
