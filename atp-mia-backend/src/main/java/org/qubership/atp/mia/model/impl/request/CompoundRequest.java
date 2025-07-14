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

package org.qubership.atp.mia.model.impl.request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.configuration.CompoundConfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompoundRequest {

    String name;
    List<ProcessRequest> processList;

    /**
     * Get CompoundRequest fromCompoundConfiguration.
     *
     * @param compound CompoundConfiguration
     * @return CompoundRequest
     */
    public static CompoundRequest fromCompoundConfiguration(CompoundConfiguration compound) {
        List<ProcessRequest> processList = compound.getProcesses() == null
                ? new ArrayList<>()
                : compound.getProcesses().stream()
                .map(p -> ProcessRequest.fromProcessConfiguration(p))
                .collect(Collectors.toList());
        return new CompoundRequest(compound.getName(), processList);
    }
}
