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

package org.qubership.atp.mia.model.pot.db;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlUpdateQueryFailException;
import org.qubership.atp.mia.model.pot.db.table.DbTable;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DbAnswer {

    @Getter
    private final boolean status;
    private ResultSet resultSet;
    @Getter
    private final DbTable dbTable;
    @Getter
    private final int updateCount;

    /**
     * Creates a result wrapper for operations that return tabular data.
     *
     * @param status      true if the operation returned a result set
     * @param dbTable     parsed result set
     * @param updateCount number of affected rows (or -1 if not applicable)
     */
    public DbAnswer(boolean status, DbTable dbTable, int updateCount) {
        this.status = status;
        this.dbTable = dbTable;
        this.updateCount = updateCount;
    }

    /**
     * Creates a result wrapper for update-only operations.
     *
     * @param status      true if the operation succeeded
     * @param updateCount number of affected rows
     */
    public DbAnswer(boolean status, int updateCount) {
        this.status = status;
        this.dbTable = null;
        this.updateCount = updateCount;
    }

    /**
     * Legacy constructor for JDBC stored procedures.
     *
     * @param status    true if the procedure returned a result set
     * @param statement the CallableStatement used
     * @throws SQLException if result extraction fails
     * @deprecated Use {@link #DbAnswer(boolean, DbTable, int)} instead
     */
    @Deprecated
    public DbAnswer(boolean status, CallableStatement statement) throws SQLException {
        this.status = status;
        this.dbTable = null;
        try {
            if (status) {
                resultSet = statement.getResultSet();
                updateCount = -1;
            } else {
                updateCount = statement.getUpdateCount();
            }
        } catch (SQLException e) {
            log.error("Error during getResults from statement.", e);
            throw e;
        }
    }

    /**
     * Populates a SqlResponse with either result data or update count.
     *
     * @param response the response to update
     */
    public void updateSqlResponse(SqlResponse response) {
        if (status) {
            if (dbTable != null) {
                response.setData(dbTable);
                response.setRecords(dbTable.getData().size());
            } else if (resultSet != null) {
                try {
                    DbTable table = SqlUtils.resultSetToDbTable(resultSet);
                    response.setData(table);
                    response.setRecords(table.getData().size());
                } catch (SQLException e) {
                    throw new SqlUpdateQueryFailException(e);
                }
            }
        } else {
            response.setDescription("Affected rows: " + updateCount);
        }
    }
}
