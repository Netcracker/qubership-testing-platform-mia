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

package org.qubership.atp.mia.model.exception;

import java.util.MissingFormatArgumentException;

import lombok.extern.slf4j.Slf4j;

/**
 * 0000 - No errors.
 * 0001-0999 Error with runtime or file scripts configurations.
 * 0050-0099 Configuration error
 * 0100-0149 Git error
 * 0150-0169 SSL
 * 0170-0199 WS - web socket
 * 0200-0249 Edit process/compound/section errors
 * 1000-2999 Errors in Business logic, algorithm or requirements.
 * 1100-1299 SSH.
 * 1300-1399 SQL.
 * 1400-1499 REST.
 * 1500-1699 SOAP.
 * 1700-1899 Test data. Matrices.
 * 1900-2000 Evaluation, Macros.
 * 2000-2050 dos2unix.
 * 2050-2099 FileService: download, upload, etc.
 * 2100-2149 GridFS.
 * 2150-2199 POT - proof of testing.
 * 2200-2249 Prerequisites
 * 3000-4999 Errors with working with data sources or Connection to DB.
 * 5000-5999 Problems in Integration with some external system.
 * 6000-6999 Errors for object grant management and system access limitation.
 * 7000-7049 Export/import problems
 * 7050-9998 Reserve for overhead by any other category.
 * 0000;9999 System global errors reserved for throw global system message with white or red flag.
 */
@Slf4j
public enum ErrorCodes {
    MIA_0000_NO_ERRORS("MIA-0000", "No errors", 0),
    MIA_0001_STOP_ON_FAIL("MIA-0001", "Process (%s) failed, because stopOnFail activated and %s"
            + ", so stop compound execution", 0),
    MIA_0002_UNSUPPORTED_COMMAND("MIA-0002", "Unsupported command: %s", 1),
    MIA_0003_UNSUPPORTED_COMMAND_TYPE("MIA-0003", "Unsupported command type: %s. "
            + "Please ask developers to add it.", 1),
    MIA_0049_PROJECT_NOT_FOUND("MIA-0049", "Project with ID '%s' not found", 0),
    MIA_0050_DUPLICATE_CONFIG("MIA-0050", "You have duplicates in configuration,"
            + " please rename repeating processes: %s", 0),
    MIA_0051_COMPOUND_NULL("MIA-0051", "%s\nPlease resolve all repeats by renaming processes!", 0),
    MIA_0052_FOUND_PROCESS_NOT_COMPOUND("MIA-0052", "Found process '%s' is not a compound", 1),
    MIA_0053_COMPOUND_HAS_NO_PROCESSES("MIA-0053", "Compound with name '%s' has no processes!", 1),
    MIA_0054_FLOW_CONTAIN_ERROR("MIA-0054", "Flow.json configuration file contains errors in [%s] part,"
            + " message: [%s]", 1),
    MIA_0055_("MIA-0055", "Flow.json configuration file contains errors in [%s] part,"
            + " message: [%s]", 1),
    MIA_0056_SERIALIZE_CONFIG_CREATE_DIR_FAIL("MIA-0056", "Can't serialize config, "
            + "because directory creation failed: %s. Exception: %s", 1),
    MIA_0057_SERIALIZE_CONFIG_REMOVE_DIR_FAIL("MIA-0057", "Can't remove temporary folder "
            + "with config: %s. Exception: %s", 0),
    MIA_0058_SERIALIZE_CONFIG_COPY_DIR_FAIL("MIA-0058", "Can't finish config serialization,"
            + "because failed to copy config folder, from: %s to: %s. Exception: %s", 1),
    MIA_0059_WRITE_JSON_TO_FILE_FAIL("MIA-0059", "Can't open file to write json,"
            + "because IOException during open: %s. Exception: %s", 1),
    MIA_0060_DESERIALIZE_JSON_CONFIG_ERROR("MIA-0060", "%s. Exception: %s", 1),
    MIA_0061_DESERIALIZE_ERROR_IN_FILE("MIA-0061", "Error in file [%s], while reading of process: [%s]. "
            + "Exception: %s", 1),
    MIA_0062_WRONG_VARIABLE_FORMAT("MIA-0062", "Wrong variable format (%s), please make sure"
            + "that you correctly escaped all special characters.", 1),
    MIA_0065_VALIDATE_CONFIG_FOR_NULL_FILE_PATH_IN_PROCESSES("MIA-0065", "Please check the Flow.json file "
            + "configuration for correctness. \n\"pathToFile\" might have missed for processes in %s.", 1),
    MIA_0066_SECTION_NOT_FOUND("MIA-0066", "Section with name/id '%s' not found!", 1),
    MIA_0067_COMPOUND_NOT_FOUND("MIA-0067", "Compound with name/id '%s' not found!", 1),
    MIA_0068_PROCESS_NOT_FOUND("MIA-0068", "Process with name/id '%s' not found!", 1),
    MIA_0069_DIRECTORY_NOT_FOUND("MIA-0069", "Directory with name/id '%s' not found!", 1),
    MIA_0070_FILE_NOT_FOUND("MIA-0070", "Project file with name/id '%s' not found!", 1),
    MIA_0071_SECTION_DUPLICATION("MIA-0071", "Section with name '%s' already present", 1),
    MIA_0072_COMPOUND_DUPLICATION("MIA-0072", "Compound with name %s already present!", 1),
    MIA_0073_PROCESS_DUPLICATION("MIA-0073", "Process with name %s already present!", 1),
    MIA_0074_DIRECTORY_DUPLICATION("MIA-0074", "Directory with name %s already present!", 1),
    MIA_0075_FILE_DUPLICATION("MIA-0075", "Project file with name %s already present!", 1),
    MIA_0076_SECTION_CYCLIC_DEPENDENCY("MIA-0076", "Cyclic dependence for section ID %s and parent section ID %s "
            + "has been found", 1),
    MIA_0077_DIRECTORY_CYCLIC_DEPENDENCY("MIA-0077", "Cyclic dependence for directory ID %s and parent directory ID %s "
            + "and has been found", 1),
    MIA_0078_STORE_ETALON_FILES_TO_DB_FAILED("MIA-0078", "Some problem occurred to Store etalon-files to DB. Error %s",
            1),
    MIA_0079_FILE_META_DATA_NOT_FOUND("MIA-0079", "FileMetaData not defined for file %s. Try to avoid if path "
            + "navigations from current directory like ..\\", 1),
    MIA_0080_CREATE_SECTION_PROBLEM("MIA-0080", "Problem occurred during section creation", 1),
    MIA_0081_CREATE_COMPOUND_PROBLEM("MIA-0081", "Problem occurred during compound creation. Exception: %s", 1),
    MIA_0082_CREATE_PROCESS_PROBLEM("MIA-0082", "Problem occurred during process creation. Exception %s", 1),
    MIA_0083_CREATE_DIRECTORY_PROBLEM("MIA-0083", "Problem occurred during directory creation. Exception: %s", 1),
    MIA_0084_CREATE_FILE_PROBLEM("MIA-0084", "Problem occurred during file creation", 1),
    MIA_0085_UPDATE_SECTION_PROBLEM("MIA-0085", "Problem occurred during section update. Exception: %s", 1),
    MIA_0086_UPDATE_COMPOUND_PROBLEM("MIA-0086", "Problem occurred during compound update. Exception %s", 1),
    MIA_0087_UPDATE_PROCESS_PROBLEM("MIA-0087", "Problem occurred during process update. Exception %s", 1),
    MIA_0088_UPDATE_DIRECTORY_PROBLEM("MIA-0088", "Problem occurred during directory update. Exception %s", 1),
    MIA_0089_UPDATE_FILE_PROBLEM("MIA-0089", "Problem occurred during file update. Exception: %s", 1),
    MIA_0090_DELETE_SECTION_PROBLEM("MIA-0090", "Problem occurred during section delete. Exception: %s", 1),
    MIA_0091_DELETE_COMPOUND_PROBLEM("MIA-0091", "Problem occurred during compound delete. Exception: %s", 1),
    MIA_0092_DELETE_PROCESS_PROBLEM("MIA-0092", "Problem occurred during process delete. Exception: %s", 1),
    MIA_0093_DELETE_DIRECTORY_PROBLEM("MIA-0093", "Problem occurred during directory delete. Exception: %s", 1),
    MIA_0094_DELETE_FILE_PROBLEM("MIA-0094", "Problem occurred during file delete. Exception: %s", 1),
    MIA_0095_UPDATE_CONFIGURATION_PROBLEM("MIA-0095", "Problem occurred during update configuration. Exception: %s", 1),
    MIA_0096_PROCESS_OR_COMPOUND_NOT_FOUND("MIA-0096", "Process or Compound with name/id '%s' not found!", 1),
    MIA_0097_CURRENT_STATEMENT_LIST_EMPTY("MIA-0097", "Current statement list is empty for "
            + "Process or Compound with name/id '%s'!", 1),
    MIA_0098_DELETE_FILE_GIT_SYNC_PROBLEM("MIA-0098", "Error during pushing changes to git repository."
            + " Please synchronize with git before removing file: "
            + "1. Click \"Update configuration\" icon; 2. Click \"Reload configuration from GIT\" icon."
            + " Exception: %s", 1),

    MIA_0100_GIT_ERROR("MIA-0100", "Error during git operation."
            + " Probably you forgot to add x_kube2vcs to your GIT project. Exception: %s", 1),
    MIA_0101_GIT_RESET_ERROR("MIA-0101", "Error during git reset command. Exception: %s", 1),
    MIA_0102_GIT_REPO_BUILD_ERROR("MIA-0102", "Error during building gir repository. Exception: %s", 1),
    MIA_0103_LOAD_CONFIG_COPY_FAIL("MIA-0103", "Load config error, can't copy config. ProjectId: %s,"
            + " destination: %s. Exception: %s", 1),
    MIA_0104_LOAD_CONFIG_FAIL("MIA-0104", "Load config failed for projectId: %s. Exception: %s", 1),
    MIA_0105_UPDATE_CONFIG_FAIL("MIA-0105", "Update file config failed: '%s'. Exception: %s", 1),
    MIA_0106_READ_CONFIG_JSON_ERROR("MIA-0106", "Error while reading of config '%s' for project '%s'."
            + "Error line: %s, column: %s. Exception: %s", 1),
    MIA_0107_READ_CONFIG_FAIL("MIA-0107", "Error while reading of config '%s' for project '%s'. Exception: %s", 1),
    MIA_0108_REPO_ENCODE_FAIL("MIA-0108", "Can't encode repository name %s for repository: %s", 1),
    MIA_0109_REPO_USER_NOT_SET("MIA-0109", "User for git repository not set. Please set git.reposUser", 1),
    MIA_0110_REPO_PASS_NOT_SET("MIA-0110", "Pass for git repository not set. Please set git.reposPass", 1),
    MIA_0111_REPO_MAIL_NOT_SET("MIA-0111", "Email for git repository not set. Please set git.reposEmail", 1),
    MIA_0112_AUTH_FAIL("MIA-0112", "Cannot authorize in git repository! "
            + "Make sure user/ password to git repository is correct. To fix it, change in application.properties "
            + "parameters GIT_USER, GIT_PASS.", 1),
    MIA_0113_GIT_VALIDATION_PROJECT_NOT_FOUND("MIA-0113", "WARNING: Project not found during validation! "
            + "Make sure you're using actual project name, it could have been updated.", 0),
    MIA_0114_GIT_VALIDATION_USER_NOT_FOUND("MIA-0114", "WARNING: User not found during validation! "
            + "Make sure you've added x_kube2vcs user(or non default) to your git repository as Maintainer.", 0),
    MIA_0115_GIT_VALIDATION_USER_NO_RIGHTS("MIA-0115", "WARNING: Make sure x_kube2vcs user has "
            + "at least maintainer rights in %s", 0),
    MIA_0116_GIT_VALIDATION_UNEXPECTED_ERROR("MIA-0116", "WARNING: Can't validate repository for "
            + "x_kube2vcs user due to unexpected error %s", 0),

    MIA_0150_SSL_ALGORITHM_NOT_PRESENT("MIA-0150", "Can't get SSL instance. Exception: %s", 1),
    MIA_0151_SSL_INIT_ERROR("MIA-0151", "An error occurred while trying to init sslContext. Exception: %s", 1),

    MIA_0170_WS_INCORRECT_MSG_TYPE("MIA-0170", "CONNECT and DISCONNECT are only available message types, use them.", 1),
    MIA_0171_WS_PROJECT_ID_NULL("MIA-0171", "ProjectId cannot be empty.", 1),

    MIA_0200_EDIT_COMPOUND_SECTION_NOT_FOUND("MIA-0200", "Cannot found section with such executable: %s", 1),
    MIA_0201_SECTION_NAME_NOT_FOUND("MIA-0201", "Section with name '%s' not found", 1),
    MIA_0202_UPDATE_CONFIGURATION_WRONG_TYPE("MIA-0202", "Request type is not supported: %s", 1),
    MIA_0203_SECTION_ACTION_WRONG_TYPE("MIA-0203", "Unknown section action type: %s", 1),
    MIA_0204_SECTION_ACTION_FAIL("MIA-0204", "Error occurred on [%s], with section [%s] and project [%s]", 1),

    MIA_0215_EDIT_SECTION("MIA-0215", "You have multiple occurrence of the process/compound, "
            + "don't know which one to update.\n Please check all processes/compounds for "
            + "a name: %s ; in a section: %s", 1),
    MIA_0216_EDIT_SECTION_CONTAIN_NULL("MIA-0216", "Error during retrieving executable, "
            + "probably you didn't use find section executable before method use. Exception: %s", 1),

    MIA_0250_CANT_PARSE_EXPECTED_CODE("MIA-0250", "Expected HTTP code contains error, please make"
            + " sure that it correct regexp!", 1),
    MIA_0251_CODE_NOT_PRESENT_WHEN_FLAG_ON("MIA-0251", "Can't find any expected HTTP codes when "
            + "'Check Status Code' flag is on. Add some codes beneath this flag or just turn it off.", 1),
    MIA_0260_MARKER_REGEX_ERROR("MIA-0260", "Can't parse regular expression for %s marker: %s. "
            + "Please correct marker in process configuration.", 1),

    MIA_1101_SSH_RSA_ADD_FAILED("MIA-1101", "Add RSA key file failed from '%s'. Exception: %s", 1),
    MIA_1102_SSH_CHANNELS_BUSY("MIA-1102", "All channels in session occupied! Timeout limit exceeded: %s sec.", 1),
    MIA_1103_SSH_EXECUTION_WRONG_EXIT("MIA-1103", "Incorrect exit in %s, incorrect command: %s", 1),
    MIA_1104_SSH_EXCEPTION("MIA-1104", "%s in channel [%s, %s]. Exception: %s", 1),
    MIA_1105_SSH_CHANNEL_CREATE_FAIL("MIA-1105", "Can't open ssh channel with ID %s in last attempt also: %s", 1),
    MIA_1106_SSH_CHANNEL_CREATE_INTERRUPT("MIA-1106", "Await interrupted during creation channel with ID %s."
            + " Exception: %s", 1),
    MIA_1107_SSH_MISSED_PARAMETER("MIA-1107", "Error while getting params from config of command. "
            + "Please check this fields: %s.", 1),
    MIA_1108_SSH_TRANSFER_FILE_FAIL("MIA-1108", "Error while transferring file from [%s] to [%s]. Exception: %s", 1),
    MIA_1109_SSH_CREATE_SESSION_FAIL("MIA-1109", "Can't get JSch session! Please check at environments"
            + " all SSH credentials such as host/port, login/pass etc. Also make sure that such host is available. "
            + "ConnectionProperties: %s, Exception: %s", 1),
    MIA_1110_SSH_CREATE_SESSION_EXCEPTION("MIA-1110", "Exception occurred while tyring to connect Session."
            + "ConnectionProperties: %s, Exception: %s", 1),
    MIA_1111_SSH_SFTP_EXCEPTION("MIA-1111", "SFTP exception %s%s in channel [%s, %s] .", 1),
    MIA_1112_SSH_PATH_FOR_DOWNLOAD_EMPTY("MIA-1112", "You haven't added paths for downloading! "
            + "If you not planning to download files change process type to SSH. Otherwise specify download path "
            + "process settings (pathsForDownload parameter)", 1),

    MIA_1113_SSH_EXECUTION_TIMEOUT("MIA-1113", "Ssh command execution was interrupted by timeout. \n"
            + "command: [\" %s \"], timeout: [\" %s ms\"]", 1),

    MIA_1300_SQL_COMMAND_UNSUPPORTED("MIA-1300", "Unsupported sql command: %s", 1),
    MIA_1301_SQL_UPDATE_QUERY_FAIL("MIA-1301", "SqlException during updateSqlResponse with resultSet."
            + " Exception: %s", 1),
    MIA_1302_SQL_PARSE_RESULT_FAIL("MIA-1302", "Could not get row from result set. Exception: %s", 1),
    MIA_1303_SQL_STORED_PROCEDURE_FAIL("MIA-1303", "Could not execute stored procedure. Exception: %s", 1),
    MIA_1304_SQL_TIMEOUT("MIA-1304", "Timeout during query execution [timeout = %s %s; query = %s]", 1),
    MIA_1305_SQL_EXECUTE_FAIL("MIA-1305", "Could not execute db command: %s. Exception: %s", 1),
    MIA_1306_SQL_FIRST_VALUE_NOT_PRESENT("MIA-1306", "No rows found for query %s.", 1),
    MIA_1307_CASANDRA_PARAMETER_NOT_FOUND("MIA-1307", "SQL query contains parameter '%s' which "
            + "is not found in FlowData", 1),
    MIA_1308_SQL_CACHE_RETRIEVE_FAIL("MIA-1308", "Error while retrieving server '%s' from SQL LoadingCache."
            + " Exception: %s", 1),
    MIA_1309_SQL_LOAD_DRIVER_FAIL("MIA-1309", "Could not create connection because "
            + "can't load driver class for '%s'", 1),
    MIA_1310_SQL_CONNECTION_FAIL("MIA-1310", "Can't connect to sql server '%s'. Exception: %s", 1),
    MIA_1311_STORE_CSV_FILE_NOT_FOUND("MIA-1311", "File not found for writing resulting SQL table "
            + "to CSV file: %s.", 1),
    MIA_1312_STORE_CSV_IO_ERROR_DURING_CLOSE("MIA-1312", "IOException occurred during closing file: %s."
            + " Exception: %s", 1),
    MIA_1313_STORE_CSV_ERROR_DURING_SAVE("MIA-1313", "IOException occurred during"
            + " saving CSV table to file: %s. Exception: %s.", 1),
    MIA_1314_STORE_CSV_WRITING_ROW_FAIL("MIA-1314", "Error in CSV file while writing row: '%s'. Exception: %s", 1),
    MIA_1315_SQL_BILL_DATE_FAIL("MIA-1315", "Could not get next bill date in data base for '%s' account number."
            + " Index out of range.", 1),
    MIA_1316_SQL_JDBC_URL_EMPTY("MIA-1316", "Can't connect to sql server because JDBC URL is null, "
            + "please go to environments service and click 'Generate JDBC URL'.", 1),
    MIA_1317_SQL_VALIDATION_TIMEOUT("MIA-1317", "Validation timeout occurred. Please make sure"
            + " that all your validation query can be done in %s %s", 1),
    MIA_1318_CASSANDRA_AUTHENTICATION_ERROR("MIA-1318", "Unable to connect to Cassandra DB. Please check the "
            + "Cassandra DB is UP AND All the DB properties are provided correctly in ENV Service.  Error: %s", 1),
    MIA_1319_CASSANDRA_DB_CONNECTION_ISSUE("MIA-1319", "Unable to connect to Cassandra DB. Error: %s", 1),
    MIA_1320_CASSANDRA_POOL_ISSUE("MIA-1320", "Unable to create cluster pool. Error: %s", 1),

    MIA_1400_REST_INCORRECT_ENDPOINT("MIA-1400", "Incorrect End Point. Error while getting params from command. "
            + "Please check these inputs: Rest method, Rest endpoint. Exception: %s", 1),
    MIA_1401_REST_INCORRECT_URL("MIA-1401", "Error while getting URL from REST connection. "
            + "Please check this field. ", 1),
    MIA_1402_UNSUPPORTED_REST_METHOD("MIA-1402", "Unsupported rest method: %s", 1),
    MIA_1403_UNSUPPORTED_REST_ENCODING("MIA-1403", "Could not set body on rest request, "
            + "because REST body uses incorrect encoding. Exception: %s", 1),
    MIA_1404_REST_ERROR_DURING_EXECUTION("MIA-1404", "Error occurred during rest request execution."
            + " Exception: %s", 1),
    MIA_1405_REST_RESULT_WRITE_TO_FILE_ERROR("MIA-1405", "Error occurred while writing rest response to file."
            + " Exception: %s", 1),
    MIA_1406_REST_HEADERS_INCORRECT_FORMAT("MIA-1406", "Header '%s' have incorrect format "
            + "( should be 'HeaderName:HeaderValue').", 1),
    MIA_1407_REST_CREATE_CONNECTION_FAIL("MIA-1407", "Failed to create REST and HTTP connection. Exception: %s", 1),
    MIA_1408_REST_PARSE_ERROR("MIA-1408", "Errors occurred while parsing rest response. Exception: %s", 1),
    MIA_1409_REST_NOT_FOUND("MIA-1409", "REST not found in command. Check UG - user guide", 1),
    MIA_1410_REST_CONTENT_TYPE_NOT_SUPPORT("MIA-1410", "REST header 'content-type: (%s)' is not supported", 1),
    MIA_1411_REST_COPY_RESULT_TO_STRING("MIA-1411", "Error occurred while copy rest response from file to string."
            + " Probably file is not exist: %s. Exception: %s", 1),
    MIA_1412_REST_FORMAT_NOT_CORRECT("MIA-1412", "Rest file format is not correct", 1),
    MIA_1413_REST_FILE_NOT_FOUND("MIA-1413", "REST file not found. Please upload file to git configuration.", 1),
    MIA_1414_REST_EXECUTION_TIMEOUT("MIA-1414", "Timeout during REST execution [timeout = %s %s; EndPoint = %s]", 1),
    MIA_1500_SOAP_GET_INSTANCE_FAIL("MIA-1500", "Errors occurred during "
            + "MessageFactory instance getting. Exception: %s", 1),
    MIA_1501_SOAP_CREATE_REQUEST_FAIL("MIA-1501", "Errors occurred during soap request creating. Exception: %s", 1),
    MIA_1502_SOAP_CREATE_CONNECTION_FAIL("MIA-1502", "Failed to create SOAP HTTP connection. Exception: %s", 1),
    MIA_1503_SOAP_EXECUTION_FAIL("MIA-1503", "Errors occurred during soap request execution. Exception: %s", 1),
    MIA_1504_SOAP_WRITE_IO_ERROR("MIA-1504", "Errors occurred while writing soap response to file. Exception: %s", 1),
    MIA_1505_SOAP_WRITE_OUTPUT_FAIL("MIA-1505", "Errors occurred while reading soap response. Exception: %s", 1),
    MIA_1506_SOAP_NOT_FOUND("MIA-1506", "SOAP not found in command. Check UG", 1),

    MIA_1700_MATRIX_VALIDATION_ERROR("MIA-1700", "Exception during test data validation: %s", 1),
    MIA_1702_MATRIX_INCORRECT_PARAMETER("MIA-1702", "'%s' in excel file (input) not defined.", 1),
    MIA_1703_MATRIX_COMMAND_INCORRECT_PARAMETER("MIA-1703", "'%s' in command not defined.", 1),
    MIA_1704_MATRIX_SCENARIO_NOT_FOUND("MIA-1704", "Scenario not found, error: %s", 1),
    MIA_1705_MATRIX_EXCEL_PARSE_FAIL("MIA-1705", "File problem while parsing %s sheet: %s. Exception: %s", 1),
    MIA_1706_MATRIX_EXCEL_WRITE_FAIL("MIA-1706", "Write to file '%s' has failed. Exception: %s", 1),
    MIA_1707_MATRIX_EXCEL_CLOSE_FAIL("MIA-1707", "Close of file '%s' has failed. Exception: %s", 1),
    MIA_1708_MATRIX_QUERY_SHEET_MISSED_DB_TYPE("MIA-1708", "DB Type is missing for query - %s", 1),

    MIA_1710_MATRIX_ETHALON_READ_FAIL("MIA-1710", "Can't read content of ethalon file: %s. Exception: %s", 1),
    MIA_1711_MATRIX_ETHALON_WRITE_FAIL("MIA-1711", "Can't write content into file: %s. Exception: %s", 1),

    MIA_1800_UUID_EMPTY("MIA-1800", "UUID can not be empty in ITF request", 1),
    MIA_1801_ITF_PROJECT_ID_EMPTY("MIA-1801", "Project ID can not be empty in ITF request", 1),
    MIA_1802_ITF_MIA_PATH_EMPTY("MIA-1802", "Mia path can not be empty in ITF request", 1),
    MIA_1803_ITF_PROCESS_NAME_EMPTY("MIA-1803", "ITF Process Name can not be empty", 1),
    MIA_1804_ITF_ID_EMPTY("MIA-1804", "Itf id can not be empty", 1),
    MIA_1805_ITF_METHOD_EMPTY("MIA-1805", "Itf Rest method detail can not be empty", 1),
    MIA_1806_ITF_URL_EMPTY("MIA-1806", "Itf url can not be empty", 1),
    MIA_1807_ITF_HEADERS_EMPTY("MIA-1807", "Itf headers can not be empty", 1),
    MIA_1808_ITF_REQUEST_IN_PROGRESS("MIA-1808", "Export with 'id: %s' is in progress with same "
            + "miaPath and projectID", 1),
    MIA_1809_ITF_SECTION_ALREADY_PRESENT("MIA-1809", "Section of 'name: %s' is already present", 1),
    MIA_1810_PROCESS_IS_NULL("MIA-1810", "Process shouldn't be null. projectId=%s, miaPath=%s", 1),
    MIA_1811_ITF_PROCESS_CREATION_TIMEOUT("MIA-1811", "Process creation has been terminated due to timeout %s", 1),
    MIA_1812_MIA_URL_ENCODE_FAIL("MIA-1812", "Enoding of mia url for processName=%s has been failed", 1),
    MIA_1813_MIA_PROCESS_ALREADY_PRESENT("MIA-1813", "Process with name: %s is already present with same "
            + "miaPath and projectID", 1),
    MIA_1814_PROCESS_NAME_INCORRECT("MIA-1814", "Process name: %s should not contains not word "
            + "symbols or spaces.", 1),
    MIA_1815_PROCESS_NOT_CREATED("MIA-1815", "Process is still not created in mia ", 0),

    MIA_1900_MACROS_RANDOM_INCORRECT_LENGTH("MIA-1900", "Error while getting params "
            + "from macros. Please check params of macros: number for random (one parameter which > 0). "
            + "Example: ${Random(5)}", 1),
    MIA_1901_MACROS_RANDOM_INCORRECT_FORMAT("MIA-1901", "Could not convert %s value to integer", 1),
    MIA_1902_MACROS_TIMESTAMP_INCORRECT_FORMAT("MIA-1902", "Error while getting params from macros."
            + " Please check params of macros: date format. Example: ${Timestamp(YYYYMMDD_hhmmssSS)}", 1),
    MIA_1903_VAR_INSIDE_VAR_FORBIDDEN("MIA-1903", "You can't use "
            + "'variableInsideVariable' feature while default 'variableFormat' is using", 1),
    MIA_1904_MACROS_DATE_INCORRECT_INPUT("MIA-1904", "Error while getting params from macros. "
            + "Please check params of macros: current date, current date format and new date format. "
            + "Example: ${Date_Formatter(20190826 12000000, yyyyMMdd hhmmssSS, yyyy-MMMM-dd)}", 1),
    MIA_1905_MACROS_DATE_INCORRECT_FORMAT("MIA-1905", "Could not parse date %s to %s format, "
            + "please check. Exception: %s", 1),
    MIA_1906_INCORRECT_ENVIRONMENT_VARIABLE_FORMAT("MIA-1906", "Incorrect format of Input Environment Variable %s."
            + "\nSyntax : ${ENV.SYSTEM_NAME.CONNECTION_NAME.parameter}"
            + "\n\nExample: ${ENV.Billing System.DB.db_type}", 1),
    MIA_1907_PATTERN_COMPILE_EXCEPTION("MIA-1907", "Pattern Compile Error. Exception: %s", 1),

    MIA_2000_DOS2UNIX_WINDOWS_FAIL("MIA-2000", "Error during dos2unix execution at Windows. Exception: %s.", 1),
    MIA_2001_DOS2UNIX_LINUX_FAIL("MIA-2001", "Error during dos2unix execution at Linux. "
            + "dos2unix error: 1) check dos2unix command in linux PATH; "
            + "2) check whether file saved at server in directory: [%s];"
            + "3) make sure dos2unix installed at server with MIA on path: [%s];"
            + "4) dos2unix should be executable, don't forget to apply command chmod +x "
            + "5) otherwise you can change this path in application.properties dos2unix.path variable.", 1),
    MIA_2002_DOS2UNIX_INCORRECT_TOOL_PATH("MIA-2002", "Can't find dos2unix in the path, "
            + "make sure that dos2unix file is present: %s. Otherwise you can change this path "
            + "in application.properties file, in dos2unix.path variable. Exception: %s", 1),
    MIA_2003_DOS2UNIX_INCORRECT_FILE_PATH("MIA-2003", "Can't run dos2unix, because path is not "
            + "valid or file not saved at server, please make sure that file present at path: %s", 1),

    MIA_2050_ARCHIVE_FILE_NOT_FOUND("MIA-2050", "File for downloading haven't created."
            + " Make sure that the path is correct: %s.", 1),
    MIA_2051_ARCHIVE_IO_ERROR_DURING_SAVE("MIA-2051", "IOException occurred during"
            + " saving file to archive, path : %s. Exception: %s.", 1),
    MIA_2052_ARCHIVE_IO_ERROR_DURING_CLOSE("MIA-2052", "IOException occurred during"
            + " OutputStream auto close, path: %s. Exception: %s", 1),
    MIA_2053_READ_FAIL_FILE_NOT_FOUND("MIA-2053", "File doesn't exists,"
            + " please correct the path: %s.", 1),
    MIA_2054_READ_FAIL_IO_ERROR_DURING_OPERATION("MIA-2054", "IOException occurred "
            + "during reading content of the file: %s. Exception: %s.", 1),
    MIA_2055_CREATE_DIR_FAIL("MIA-2055", "IOException occurred "
            + "during file creation: %s. Exception: %s.", 1),
    MIA_2056_IO_EXCEPTION("MIA-2056", "IOException occurred "
            + "during operation on the file: %s. Exception: %s.", 1),
    MIA_2057_FILE_TRANSFER_FAIL("MIA-2057", "File transfer to '%s' failed. Exception: %s", 1),
    MIA_2058_FILE_EMPTY_FAIL("MIA-2058", "File '%s' is empty", 1),
    MIA_2059_UPLOAD_TEST_DATA_FAIL("MIA-2059", "Can't upload test data, maybe there is no scenarios"
            + "or incorrect format of main page.", 1),
    MIA_2060_WRONG_FILE_PATH("MIA-2060", "File should not contains '..' or './' or '.\\' (%s).", 1),

    MIA_2075_ZIP_FILE_OR_DIR_NOT_PRESENT("MIA-2075", "%s_Error_file_is_directory_or_not_present_%s", 0),

    MIA_2100_GRID_FS_EXCEPTION("MIA-2100", "Error during saving log response to DB. Exception: %s", 1),
    MIA_2101_GRID_FS_EXCEPTION("MIA-2101", "Error during getting file %s from GridFs DB. Exception: %s", 1),
    MIA_2102_GRID_FS_EXCEPTION("MIA-2102", "Error file not found, path %s. Exception: %s", 1),

    MIA_2150_POT_TEMPLATE_NOT_FOUND("MIA-2150", "POT template not found on path: %s. Exception: %s", 1),
    MIA_2151_POT_CLOSE_ERROR("MIA-2151", "POT template '%s' cannot be closed due to exception: %s", 1),
    MIA_2152_POT_FILE_TO_WRITE_NOT_FOUND("MIA-2152", "POT file to write not found on path: %s. Exception: %s", 1),
    MIA_2153_POT_WRITE_FAIL("MIA-2153", "POT failed to write into document based on template: %s. Exception: %s", 1),
    MIA_2154_POT_HEADER_TYPE_UNSUPPORTED("MIA-2154", "POT header type unsupported: %s", 1),
    MIA_2155_POT_CREATE_FILE_FAIL("MIA-2155", "POT file creation failed for path: %s. Exception: %s", 1),
    MIA_2156_POT_SESSION_NOT_FOUND("MIA-2156", "POT session not found. Make sure that you started it "
            + "before saving it. If you use ATP make sure you have command: 'Start recording session'", 1),
    MIA_2157_POT_PRINT_ROW_ERROR("MIA-2157", "Error during printing row to POT document. "
            + "Total rows: %s, current row: %s, data size: %s.", 1),
    MIA_2158_POT_PRINT_FILE_NOT_FOUND("MIA-2158", "Error during printing file to POT document. "
            + "%s", 1),
    MIA_2159_POT_IO_ERROR("MIA-2159", "POT IO error during file "
            + "open with iterator - %s", 1),
    MIA_2160_POT_STEP_LIST_EMPTY("MIA-2160", "You haven't executed any processes. Document is empty.", 1),
    MIA_2161_POT_UUID_PARSE_ERROR("MIA-2161", "RecordingSessionService can't parse old session id as UUID: %s", 1),
    MIA_2162_POT_SESSION_ID_NOT_FOUND("MIA-2162", "POT session id not found. Make sure that you started it "
            + "before saving it. If you use ATP make sure you have command:"
            + " 'Start recording session' Session id: %s", 1),
    MIA_2163_POT_SAVE_ERROR("MIA-2163", "Error during saving POT results: %s", 1),
    MIA_2164_POT_PROCESS_STATUS_MISSED("MIA-2164", "Error during saving POT results. "
            + "Process status `%s` missed.", 1),

    MIA_2200_PREREQUISITE_TYPE_UNSUPPORTED("MIA-2200", "Unsupported prerequisite type: '%s'", 1),
    MIA_2201_PREREQUISITE_NO_RECORDS_ADDED("MIA-2201", "No records added or existing rows affected due to '%s'", 1),
    MIA_2202_PREREQUISITE_NO_VALID_QUERY("MIA-2202", "No valid query in current prerequisite. "
            + "Make sure that they start with any of such operators: %s", 1),
    MIA_2203_PREREQUISITE_CMD_ON_LOCAL_INTERRUPTED("MIA-2203", "One of prerequisites '%s' failed. "
            + "Tried to run Command '%s' and execution was interrupted. Exception: %s", 1),
    MIA_2204_PREREQUISITE_CMD_ON_LOCAL_FAILED("MIA-2204", "One of prerequisites '%s' failed. "
            + "Error occurred in Execute command on local host process. Exception: %s", 1),

    MIA_3000_NO_SUCH_DB("MIA-3000", "You are using an unsupported database (not Oracle/Cassandra)."
            + " Current dbType = '%s'. Contact the developers if you receive this message", 1),

    MIA_5000_ENV_SYSTEM_NOT_FOUND("MIA-5000", "Could not find '%s' system on environment.", 1),
    MIA_5001_ENV_SYSTEM_NOT_SPECIFIED("MIA-5001", "Could not execute command, "
            + "because no system specified on the executable command with name: '%s'.", 1),
    MIA_5002_ENV_SYSTEM_NOT_FOUND_IN_CONTEXT("MIA-5002", "No system with name (%s) in environment!", 1),
    MIA_5003_ENV_CONNECTION_NOT_FOUND_IN_CONTEXT("MIA-5003", "Could not found connection "
            + "with name (%s) in system (%s).", 1),
    MIA_5004_ENV_CONNECTION_NOT_FOUND_IN_CONTEXT("MIA-5004", "Null pointer exception during "
            + "system (%s) or connection search (%s).", 1),
    MIA_5005_PROJECT_NOT_FOUND("MIA-5005", "Project with ID '%s' not found!", 1),
    MIA_5006_ENVIRONMENT_RESPONSE_INCORRECT("MIA-5006", "Response from Environment service is incorrect!. Exception: "
            + "%s", 1),

    MIA_6000_ENCRYPT_ERROR("MIA-6000", "Problem with encrypt data. Exception: %s", 1),
    MIA_6001_DECRYPT_ERROR("MIA-6001", "Problem with parsing encrypted data. Exception: %s", 1),

    MIA_7000_NOT_SUPPORT_EXPORT("MIA-7000", "MIA does not support export type '%s'", 1),
    MIA_7001_NOT_SUPPORT_IMPORT("MIA-7001", "MIA does not support import type '%s'", 1),
    MIA_7003_IMPORT_CONFIG_NOT_LOADED("MIA-7003", "Can not load %s configuration by path '%s'", 1),
    MIA_7004_EXPORT_COPY_FILE_FAIL("MIA-7004", "Failed to copy into ExportFile from GridFS, filename: [%s]. "
            + "Exception: %s", 1),
    MIA_7005_GRID_FS_FILE_NOT_FOUND("MIA-7005", "Failed to find file in GridFs while it is present in Postgres,"
            + " filename: [%s], gridFs Object ID: [%s]", 1),
    MIA_7006_PARENT_DIRECTORY_NOT_FOUND("MIA-7006", "Failed to find parent directory for %s named %s", 1),
    MIA_7007_IMPORT_SECTION("MIA-7007", "Parent section with ID [%s] not found for section with ID [%s] "
            + "and name \"%s\"", 1),
    MIA_7008_IMPORT_GIT_ENABLED("MIA-7008", "ATP MIA entities are skipped "
            + "because project has synchronization with GIT."
            + " If you want to import ATP MIA entities please turn off GIT URL in 'Update configuration' popup", 0),

    MIA_8000_UNEXPECTED_ERROR("MIA-8000", "Unexpected error occurred. Exception: %s", 1),
    MIA_8001_SUBSTITUTION_ERROR("MIA-8001", "Error during handle error, source message: ", 1),
    MIA_8002_REQUEST_TOO_BIG("MIA-8002", "Request too big, it's bigger than 1 MB (10485760 bit). "
            + "In case of ATP run, please try to reduce your dataset size or use another action for process execution: "
            + "`Execute with prefix \"<COMMAND_NAME>\" command`", 1),
    MIA_9000_ABSOLUTE_TRAVERSAL_VULNERABULITY_EXCEPTION("MIA-9000", "This file path %s is vulnerable "
            + "to Absolute File Path Traversal", 1),
    MIA_9000_SERIALIZE_ERROR("MIA-9000", "Error occurred during Serialization", 1),
    MIA_9001_SERIALIZE_FLOW_ERROR("MIA-9001", "Error occurred during Serialization of Flow.json file. Exception: %s",
            1),
    MIA_9002_SERIALIZE_PROCESS_FILE_ERROR("MIA-9002", "Error occurred during Serialization of Process file", 1);

    private final String errorCode;
    private final String errorMessageTemplate;
    private final Integer priority;

    ErrorCodes(String errorCode, String errorMessageTemplate, Integer priority) {
        this.errorCode = errorCode;
        this.errorMessageTemplate = errorMessageTemplate;
        this.priority = priority;
    }

    /**
     * Get error message for the specified error code.
     *
     * @param params params to use in message template
     * @return The Message
     */
    public String getMessage(Object... params) {
        try {
            return errorCode + ": " + String.format(errorMessageTemplate, params);
        } catch (NullPointerException e) {
            log.error("You passed null value to error template! Add null checking before the method call.", e);
        } catch (MissingFormatArgumentException e) {
            log.error("You missed parameter during passing to template! "
                    + "Make sure that you used all %s fields in error.", e);
        }
        return MIA_8001_SUBSTITUTION_ERROR.errorMessageTemplate + errorCode + ": " + errorMessageTemplate;
    }

    public String getCode() {
        return errorCode;
    }

    public Integer getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return errorMessageTemplate + " (code: " + errorCode + ")";
    }
}
