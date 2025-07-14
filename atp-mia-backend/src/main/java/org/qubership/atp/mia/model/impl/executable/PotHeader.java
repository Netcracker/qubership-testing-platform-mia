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

package org.qubership.atp.mia.model.impl.executable;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.javers.core.metamodel.annotation.Value;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@Value
public class PotHeader implements Serializable {

    private static final long serialVersionUID = 3009547447129986844L;
    private String name;
    private String type;
    private String system = "Billing System";
    private String value;

    /**
     * Creates {@code Prerequisite} instance with parameters.
     *
     * @param name   name
     * @param type   type
     * @param system system
     * @param value  value
     */
    public PotHeader(@Nonnull String name, @Nonnull String type, @Nullable String system, @Nonnull String value) {
        this.name = name;
        this.type = type;
        if (system != null) {
            this.system = system;
        }
        this.value = value;
    }
}
