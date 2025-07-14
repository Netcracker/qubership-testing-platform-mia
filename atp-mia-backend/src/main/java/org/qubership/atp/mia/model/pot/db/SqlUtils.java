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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlParseResultFailException;
import org.qubership.atp.mia.model.pot.db.table.DbTable;
import org.qubership.atp.mia.utils.Utils;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlUtils {

    /**
     * Transforms {@link ResultSet} to {@link DbTable}.
     *
     * @param rs {@link ResultSet} itself.
     * @return {@link DbTable}.
     * @throws SQLException if error occurres.
     */
    public static DbTable resultSetToDbTable(ResultSet rs) throws SQLException {
        return resultSetToDbTable(rs, 0);
    }

    /**
     * Transforms {@link ResultSet} to {@link DbTable}.
     *
     * @param rs           {@link ResultSet} itself.
     * @param limitRecords to Limit Processing Of Records to improve performance
     * @return {@link DbTable}.
     * @throws SQLException if error occurres.
     */
    public static DbTable resultSetToDbTable(ResultSet rs, int limitRecords) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnsCount = rsmd.getColumnCount();
        List<String> headers = Lists.newArrayListWithExpectedSize(columnsCount);
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            String columnName = rsmd.getColumnName(i);
            headers.add(columnName);
        }
        return new DbTable(headers,
                Utils.streamOf(new RsIter(rs, columnsCount)).limit(limitRecords > 0 ? limitRecords : Long.MAX_VALUE)
                        .collect(Collectors.toList()));
    }

    private static class RsIter extends AbstractIterator<List<String>> {

        private final ResultSet rs;
        private final int columnsCount;

        private RsIter(ResultSet rs, int columnsCount) {
            this.rs = rs;
            this.columnsCount = columnsCount;
        }

        @Override
        protected List<String> computeNext() {
            try {
                if (rs.next()) {
                    List<String> row = Lists.newArrayListWithExpectedSize(columnsCount);
                    for (int i = 1; i <= columnsCount; i++) {
                        row.add(Objects.toString(rs.getObject(i)));
                    }
                    return row;
                }
                return endOfData();
            } catch (SQLException e) {
                throw new SqlParseResultFailException(e);
            }
        }
    }
}
