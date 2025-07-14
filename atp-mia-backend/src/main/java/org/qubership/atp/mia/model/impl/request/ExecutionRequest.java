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

import java.util.List;
import java.util.UUID;

import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.model.impl.testdata.TestDataFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {

    private String process;
    private CompoundRequest compound;
    private String command;
    private String type;
    private FlowData flowData;
    private TestDataFile testDataFile;
    private UUID sessionId;
    private List<Switcher> switchers;
    private List<Switcher> systemSwitchers;
    private Rest rest;
}
