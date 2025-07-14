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

import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class System extends AbstractConfiguratorModel {

    private UUID environmentId;
    private List<Connection> connections;

    /**
     * Get server by connection name.
     *
     * @param type connection type
     * @return Server
     * @throws IllegalArgumentException if connection not found
     */
    public Server getServer(Server.ConnectionType type) throws IllegalArgumentException {
        return getServer(type.getType());
    }

    /**
     * Get server by connection name.
     *
     * @param name connection name
     * @return Server
     * @throws IllegalArgumentException if connection not found
     */
    public Server getServer(String name) throws IllegalArgumentException {
        return new Server(
                connections.stream()
                        .filter(system -> name.equalsIgnoreCase(system.getName()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Connection '" + name + "' not found")),
                name);
    }

    /**
     * Get server by templateId.
     *
     * @param templateId template id
     * @return Server
     * @throws IllegalArgumentException if connection not found
     */
    public Server getServer(Connection.SourceTemplateId templateId) throws IllegalArgumentException {
        final String name = templateId.name().toLowerCase();
        return new Server(
                connections.stream()
                        .filter(connection -> templateId.id.equals(connection.getSourceTemplateId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Connection '" + name + "' not found")),
                templateId.name().toUpperCase());
    }
}
