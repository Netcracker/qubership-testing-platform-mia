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

package org.qubership.atp.mia.model.pot.db.table;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.qubership.atp.mia.model.pot.Statuses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TableMarkerResult {

    private TableMarkerRowCount tableRowCount;
    private List<TableMarkerColumnStatus> columnStatuses;

    /**
     * Adds checked column name and status to columnStatuses.
     *
     * @param columnName    checked columnName
     * @param status        validation status
     * @param actualValue   actual value
     * @param expectedValue expected value
     */
    public void addColumnStatus(String columnName, Statuses status, String actualValue, String expectedValue) {
        if (this.columnStatuses == null) {
            this.columnStatuses = new ArrayList<>();
        }
        this.columnStatuses.add(new TableMarkerColumnStatus(expectedValue, actualValue, columnName, status));
    }

    public void setTableRowCount(String expectedResult, String actualResult, Statuses status) {
        this.tableRowCount = new TableMarkerRowCount(expectedResult, actualResult, status);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TableMarkerColumnStatus {

        private String expectedResult;
        private String actualResult;
        private String columnName;
        private Statuses status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableMarkerRowCount {

        private String expectedResult;
        private String actualResult;
        @Nullable
        private Statuses status;
    }
}
