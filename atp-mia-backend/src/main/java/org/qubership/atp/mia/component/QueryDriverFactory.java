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

package org.qubership.atp.mia.component;

import java.util.List;

import org.qubership.atp.mia.exceptions.businesslogic.sql.SqlLoadDriverFailException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.repo.driver.QueryDriver;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QueryDriverFactory {

    private final List<QueryDriver<?>> queryDrivers;

    /**
     * Get driver pool service for server.
     *
     * @param server server
     * @return DriverPoolService
     */
    public QueryDriver<?> getDriver(Server server) {
        QueryDriver<?> queryDriver;
        String dbType = server.getProperty("db_type");
        queryDriver = tryToGetDriver(dbType);
        if (queryDriver != null) {
            return queryDriver;
        }
        dbType = server.getProperty("jdbc_url");
        queryDriver = tryToGetDriver(dbType);
        if (queryDriver != null) {
            return queryDriver;
        }
        throw new SqlLoadDriverFailException(server.getProperty("jdbc_url"));
    }

    private QueryDriver<?> tryToGetDriver(String dbType) {
        if (!Strings.isNullOrEmpty(dbType)) {
            for (QueryDriver<?> queryDriver : queryDrivers) {
                if (dbType.toLowerCase().contains(queryDriver.getDriverType().toLowerCase())) {
                    return queryDriver;
                }
            }
        }
        return null;
    }
}
