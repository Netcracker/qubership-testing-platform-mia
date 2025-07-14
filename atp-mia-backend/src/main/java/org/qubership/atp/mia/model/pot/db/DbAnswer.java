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
    private int updateCount;

    /**
     * Creates answer containing {@link CallableStatement} result. Allows to close statement and don't hang connection.
     *
     * @param status    of {@link CallableStatement#execute()}
     * @param statement {@link CallableStatement} itself.
     * @throws SQLException if error occur during {@link CallableStatement#getResultSet()}
     *                      or {@link CallableStatement#getUpdateCount()}.
     */
    public DbAnswer(boolean status, CallableStatement statement) throws SQLException {
        this.status = status;
        try {
            if (status) {
                resultSet = statement.getResultSet();
                // TODO: statement.getMoreResults();
            } else {
                updateCount = statement.getUpdateCount();
            }
        } catch (SQLException e) {
            log.error("Error during getResults from statement.", e);
            throw e;
        }
    }

    /**
     * Constructor.
     */
    public DbAnswer(boolean status, int updateCount) {
        this.status = status;
        this.updateCount = updateCount;
    }

    /**
     * UpdateSqlResponse.
     *
     * @param response of Type SqlResponse
     */
    public void updateSqlResponse(SqlResponse response) {
        if (status && resultSet != null) {
            try {
                DbTable dbTable = SqlUtils.resultSetToDbTable(resultSet);
                response.setData(dbTable);
                response.setRecords(dbTable.getData().size());
            } catch (SQLException e) {
                throw new SqlUpdateQueryFailException(e);
            }
        } else {
            response.setDescription("Affected rows: " + updateCount);
        }
    }
}
