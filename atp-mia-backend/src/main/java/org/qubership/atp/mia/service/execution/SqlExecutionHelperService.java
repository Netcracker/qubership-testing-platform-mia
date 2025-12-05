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

import static org.qubership.atp.mia.model.environment.Server.ConnectionType.DB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javax.annotation.Nonnull;
import javax.xml.ws.Holder;

import org.qubership.atp.integration.configuration.annotation.AtpJaegerLog;
import org.qubership.atp.mia.component.QueryDriverFactory;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlBillDateFailException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlCommandUnsupportedException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.StoreCsvExceptionDuringSave;
import org.qubership.atp.mia.exceptions.businesslogic.sql.StoreCsvFileNotFoundException;
import org.qubership.atp.mia.exceptions.businesslogic.sql.StoreCsvIoExceptionDuringClose;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.pot.db.DbAnswer;
import org.qubership.atp.mia.model.pot.db.SqlResponse;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.repo.driver.QueryDriver;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqlExecutionHelperService {

    private static final byte[] EMPTY_BYTES = {};
    private final MiaContext miaContext;
    private final MiaFileService miaFileService;
    private final QueryDriverFactory driverFactory;
    private final String fileDownloadPrefix;
    @Value("${db.execution.records.limit:50}")
    protected int dbExecutionRecordsLimit;

    /**
     * Store result of SQL query to CSV file.
     */
    private static void storeTableToCsv(SqlResponse sqlResponse, File targetFile) {
        log.info("Initiating CSV export to: {}", targetFile.getAbsolutePath());
        Path safeDir = Optional.ofNullable(targetFile.getParentFile())
                .map(File::toPath)
                .orElse(Paths.get("."))
                .toAbsolutePath()
                .normalize();
        Path safePath = safeDir.resolve(targetFile.getName()).normalize();
        FileUtils.createFolder(safePath.toFile());
        log.debug("Resolved safe path for CSV: {}", safePath);
        try (FileOutputStream stream = new FileOutputStream(safePath.toFile())) {
            if (sqlResponse.getData() != null) {
                Collection<String> columns = sqlResponse.getData().getColumns();
                writeLine(columns, stream, true);
                List<List<String>> data = sqlResponse.getData().getData();
                data.forEach(row -> {
                    writeLine(row, stream, true);
                });
            } else {
                writeLine(Collections.singleton(""), stream, true);
            }
            writeLine(Collections.singleton(""), stream, true);
            if (sqlResponse.getQuery() != null) {
                writeLine(Collections.singleton(sqlResponse.getQuery()), stream, false);
            } else {
                writeLine(Collections.singleton(""), stream, true);
            }
            writeLine(Collections.singleton(""), stream, true);
            if (sqlResponse.getDescription() != null) {
                writeLine(Collections.singleton(sqlResponse.getDescription()), stream, true);
            } else {
                writeLine(Collections.singleton(""), stream, true);
            }
            log.info("CSV export completed: {}", safePath.toFile());
        } catch (FileNotFoundException e) {
            throw new StoreCsvFileNotFoundException(targetFile.toPath());
        } catch (IOException e) {
            throw new StoreCsvIoExceptionDuringClose(targetFile.toPath(), e);
        } catch (Exception e) {
            throw new StoreCsvExceptionDuringSave(targetFile.toPath(), e);
        }
    }

    private static void writeLine(Collection<String> columns, FileOutputStream stream, boolean replace) {
        columns.forEach(column -> {
            try {
                byte[] value;
                if (column == null) {
                    value = EMPTY_BYTES;
                } else {
                    if (replace) {
                        column = column.replace(",", ".");
                    }
                    value = column.getBytes(StandardCharsets.UTF_8);
                }
                stream.write(value);
                stream.write(',');
            } catch (IOException e) {
                throw new SqlBillDateFailException(columns, e);
            }
        });
        try {
            stream.write('\n');
        } catch (IOException e) {
            throw new SqlBillDateFailException(columns, e);
        }
    }

    /**
     * Executes SQL command.
     *
     * @param query  query or path to .sql file with queries
     * @param system system name
     * @return {@code CommandResponse}
     */
    @AtpJaegerLog()
    public List<CommandResponse> executeCommand(String query, String system, Map<String, String> additionalParams,
                                                boolean toLimitRecords) {
        log.info("Execution for executeCommand method ");
        List<CommandResponse> responses = new ArrayList<>();
        Server server = miaContext.getFlowData().getSystem(system).getServer(DB);
        miaContext.getFlowData().addParameters(server.getProperties());
        query = miaContext.evaluate(query);
        log.info("query evaluate done !! ");
        if (query.toLowerCase().endsWith(".sql")) {
            log.info("FileUtils.readFile starts");
            final String content = FileUtils.readFile(miaFileService.getFile(query).toPath());
            log.info("FileUtils.readFile end");
            final String sqlToExecute = miaContext.evaluate(miaContext.evaluate(content), additionalParams);
            log.info("sqlToExecute to execute:{}", sqlToExecute);
            if (sqlToExecute.toLowerCase().startsWith("declare") || sqlToExecute.toLowerCase().startsWith("begin")
                    || sqlToExecute.toLowerCase().startsWith("do")) {
                log.info("Start executing stored procedure");
                SqlResponse sqlResponse = new SqlResponse(server);
                sqlResponse.setQuery(sqlToExecute);
                driverFactory.getDriver(server).executeStoredProcedure(server, sqlToExecute);
                log.info("executed StoredProcedure");
                final String status = "SUCCESS";
                final DbTable dbTable = new DbTable(Collections.singletonList("STORE PROCEDURE STATUS"),
                        Collections.singletonList(Collections.singletonList(status)));
                sqlResponse.setData(dbTable);
                sqlResponse.setRecords(1);
                responses.add(new CommandResponse(sqlResponse));
                log.info("Ended  executing stored procedure");
                log.info("saveSqlTableToFile call..");
                saveSqlTableToFile(Collections.singletonList(sqlResponse));
                log.info("saveSqlTableToFile call Ended...");
            } else {
                log.info("handleSingleQuery start: {}", query);
                for (SqlResponse sqlResponse : handleSingleQuery(sqlToExecute, server, toLimitRecords)) {
                    responses.add(new CommandResponse(sqlResponse));
                }
                log.info("handleSingleQuery end: {}", query);
            }
        } else {
            log.info("Start executing direct query: {}", query);
            for (SqlResponse sqlResponse : handleSingleQuery(query, server, toLimitRecords)) {
                responses.add(new CommandResponse(sqlResponse));
            }
            log.info("End executing direct query: {}", query);
        }
        log.info("Execution for executeCommand method Ended ");
        return responses;
    }

    /**
     * Executes SQL command.
     *
     * @param query  query or path to .sql file with queries
     * @param system system name
     * @return {@code CommandResponse}
     */
    public List<CommandResponse> executeCommand(@Nonnull String query, @Nonnull String system) {
        return executeCommand(query, system, new HashMap<>(), true);
    }

    /**
     * Executes validations.
     */
    @AtpJaegerLog()
    public List<SqlResponse> executeValidations(List<Validation> validations, Command command) {
        log.info("executeValidations start");
        List<SqlResponse> response = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(validations.size());
        for (Validation validation : validations) {
            String systemName = validation.getSystem();
            validation.setSystem(miaContext.evaluate(systemName));
            if (skipValidation(validation, command)) {
                log.debug("Validation has been skipped : " + validation);
                latch.countDown();
                continue;
            }
            Server server = miaContext.getFlowData().getSystem(systemName).getServer(DB);
            miaContext.getFlowData().addParameters(server.getProperties());
            String value = miaContext.evaluate(validation.getValue());
            String tableName = validation.getTableName();
            List<String> queries;
            log.trace("Prepare query: " + value);
            if (value != null && value.endsWith(".sql")) {
                /*String content = miaContext.evaluate(FileUtils.readFile(miaFileService.getFile(value).toPath()));
                queries = Arrays.asList(content.split(";\\r?\\n?"));*/
                try {
                    //Secure dynamic file path resolution
                    Path safePath = getSafeValidatedPath(value);
                    // Read SQL file content safely
                    String content = miaContext.evaluate(FileUtils.readFile(safePath));
                    // Split queries by ';' delimiter
                    queries = Arrays.asList(content.split(";\\r?\\n?"));
                } catch (SecurityException se) {
                    String error = "Invalid SQL file path detected (path traversal attempt?): " + value;
                    addSqlResponseWithError(response, error, server);
                    log.error(error, se);
                    latch.countDown();
                    continue;
                }

            } else {
                queries = Collections.singletonList(value);
            }
            if (queries.size() > 0) {
                for (String query : queries) {
                    log.debug("Execute validation query start ,query: " + query);
                    response.add(executeQuery(server, query, tableName,
                            validation.isSaveToWordFile(), validation.isSaveToZipFile()));
                    log.debug("Execute validation query " );
                }
            } else {
                String warnMessage = String.format("No queries were found in the file %s for validation", value);
                addSqlResponseWithError(response, warnMessage, server);
                log.warn(warnMessage);
            }
        }
        log.info("call saveSqlTableToFile Before");
        saveSqlTableToFile(response);
        log.info("call saveSqlTableToFile After");
        response.stream()
                .map(SqlResponse::getLink)
                .filter(Objects::nonNull)
                .forEach(link -> link.setPath(fileDownloadPrefix + link.getPath()));
        log.info("executeValidations end");
        return response;
    }

    private Path getSafeValidatedPath(String relativePath) {
        Path baseDir = miaContext.getProjectFilePath().toAbsolutePath().normalize();
        Path targetPath = baseDir.resolve(relativePath).normalize();
        // Reject if path traversal detected
        if (!targetPath.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt detected: " + relativePath);
        }
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new SecurityException("SQL file does not exist or is not a regular file: " + relativePath);
        }
        return targetPath;
    }

    /**
     * Handle single query.
     *
     * @param query  query
     * @param server server
     * @return Sql Response
     */
    @AtpJaegerLog()
    public List<SqlResponse> handleSingleQuery(String query, Server server, boolean toLimitRecords) {
        log.info("handleSingleQuery Method call ");
        query = miaContext.evaluate(query);
        List<String> queries = new ArrayList<>();
        List<SqlResponse> responses = new ArrayList<>();
        if (query.contains(";")) {
            queries.addAll(Arrays.asList(query.split(";\\r?\\n?")));
        } else {
            queries.add(query);
        }
        log.info("driverFactory.getDriver start");
        QueryDriver<?> driver = driverFactory.getDriver(server);
        log.info("driverFactory.getDriver End");
        for (String queryFromList : queries) {
            queryFromList = queryFromList.trim();
            SqlResponse response = new SqlResponse(server);
            response.setQuery(queryFromList);
            log.debug("Execute query: " + query);
            if (queryFromList.toLowerCase().startsWith("select")) {
                DbTable dbTable = driver.executeQuery(server, queryFromList, toLimitRecords
                        ? dbExecutionRecordsLimit : 0);
                log.debug("Query executed...");
                if (toLimitRecords && dbTable.getActualDataSizeBeforeLimit() > dbExecutionRecordsLimit) {
                    response.setLimitRecordsMessage("The number of returned rows exceeds the maximum "
                            + "allowed number of " + dbExecutionRecordsLimit + " rows");
                    log.info("The number of returned rows exceeds the maximum allowed number of {} rows. Actual "
                                    + "Records size is {}", dbExecutionRecordsLimit,
                            dbTable.getActualDataSizeBeforeLimit());
                }
                response.setData(dbTable);
                response.setRecords(dbTable.getData().size());
            } else if (queryFromList.toLowerCase().startsWith("update")
                    || queryFromList.toLowerCase().startsWith("insert")
                    || queryFromList.toLowerCase().startsWith("drop")
                    || queryFromList.toLowerCase().startsWith("create")
                    || queryFromList.toLowerCase().startsWith("delete")) {
                log.info("driver.executeUpdate start");
                int affected = driver.executeUpdate(server, queryFromList);
                log.info("driver.executeUpdate end");
                response.setDescription("Affected rows: " + affected);
            } else if (queryFromList.toLowerCase().startsWith("with")) {
                log.info("driver.executeStoredProcedure start");
                DbAnswer res = driver.executeStoredProcedure(server, queryFromList);
                log.info("driver.executeStoredProcedure end");
                res.updateSqlResponse(response);
            } else {
                throw new SqlCommandUnsupportedException(queryFromList);
            }
            log.info("query execution done");
            responses.add(response);
        }
        log.info("call saveSqlTableToFile...");
        saveSqlTableToFile(responses);
        log.info("call saveSqlTableToFile End");
        log.info("handleSingleQuery Method call Ended");
        return responses;
    }

    /**
     * Get NextBillDate.
     */
    @AtpJaegerLog()
    public String getNextBillDate() {
        String nextBillDateSql = miaContext.getConfig().getCommonConfiguration().getNextBillDateSql();
        String system = miaContext.getConfig().getCommonConfiguration().getDefaultSystem();
        String accountNumberTrim =
                miaContext.getFlowData().getCustom(Constants.CustomParameters.ACCOUNT_NUMBER, miaContext)
                        .trim().replaceAll(" ", "_");
        log.trace("Performing of nextBillDate query for {} system", system);
        Server server = miaContext.getFlowData().getSystem(system).getServer(DB);
        try {
            return driverFactory.getDriver(server).executeQueryAndGetFirstValue(server,
                    nextBillDateSql.replace(":accountNumber", accountNumberTrim));
        } catch (IndexOutOfBoundsException e) {
            throw new SqlBillDateFailException(accountNumberTrim, e);
        }
    }

    /**
     * Reset DB cashe for provided system.
     *
     * @return true if execution is OK, false otherwise
     */
    @AtpJaegerLog()
    public boolean resetDbCache() {
        final CommonConfiguration commonConfig = miaContext.getConfig().getCommonConfiguration();
        String system = commonConfig.getDefaultSystem();
        log.trace("Performing of resetCache for {} system", system);
        Server server = miaContext.getFlowData().getSystem(system).getServer(DB);
        return driverFactory.getDriver(server)
                .executeStoredProcedure(server, commonConfig.getResetCacheSql()).isStatus();
    }

    /**
     * Save sql result to file.
     */
    public void saveSqlTableToFile(List<SqlResponse> sqlResponses) {
        if (miaContext.getConfig().getCommonConfiguration().isSaveSqlTablesToFile()) {
            for (SqlResponse sqlResponse : sqlResponses) {
                String fileName = miaContext.createTableFileName(sqlResponse.getTableName());
                final Holder<File> file = new Holder<>(miaContext.getLogPath().resolve(fileName).toFile());
                storeTableToCsv(sqlResponse, file.value);
                String internalPathToFile = file.value.getPath();
                sqlResponse.setInternalPathToFile(internalPathToFile, miaContext);
            }
        }
    }

    /**
     * Check if need to skip the validation.
     *
     * @param validation validation
     * @return {@code true} if need, {@code false} otherwise
     */
    private boolean skipValidation(@Nonnull Validation validation, @Nonnull Command command) {
        final List<String> refers = validation.getReferToCommandExecution();
        return !(refers == null || refers.stream().anyMatch(r ->
                r.equalsIgnoreCase("any")
                        || command.getToExecute().toLowerCase().contains(r.toLowerCase())));
    }

    /**
     * Adds sql response with error in description field to main response.
     */
    private void addSqlResponseWithError(List<SqlResponse> response, String error, Server server) {
        SqlResponse sqlResponse = new SqlResponse(server);
        sqlResponse.setDescription(error);
        response.add(sqlResponse);
    }

    private SqlResponse executeQuery(Server server,
                                     String query,
                                     String tableName,
                                     boolean saveToWordFile,
                                     boolean saveToZipFile) {
        return driverFactory.getDriver(server).executeQuery(server, query, tableName, saveToWordFile, saveToZipFile,
                dbExecutionRecordsLimit);
    }
}

