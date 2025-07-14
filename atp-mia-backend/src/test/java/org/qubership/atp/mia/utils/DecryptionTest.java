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

package org.qubership.atp.mia.utils;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.VariableFormat;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@ExtendWith(SkipTestInJenkins.class)
@SpringBootTest(classes = {CryptoUtils.class, Decryptor.class, Encryptor.class}, properties = {"spring.cloud.vault.enabled=false"})
public class DecryptionTest extends ConfigTestBean {

    @MockBean
    protected SqlExecutionHelperService sqlService;

    @Test
    public void evaluate_whenSensitiveData() {
        FlowData flowData = miaContext.get().getFlowData();
        CommonConfiguration commonConfiguration = miaContext.get().getConfig().getCommonConfiguration();
        commonConfiguration.setUseVariablesInsideVariable(true);
        commonConfiguration.setVariableFormat(":\\{" + VariableFormat.VAR_NAME + "\\}");
        String var1 = "value1->:{var2}";
        String var2 = "value2->:{var3}";
        String var3 = "value3";
        flowData.addParameter("var1", CryptoUtils.encryptValue(var1));
        flowData.addParameter("var2", CryptoUtils.encryptValue(var2));
        flowData.addParameter("var3", CryptoUtils.encryptValue(var3));
        String evaluated = miaContext.get().evaluate("Evaluated text::{var1}.");
        assertEquals("Evaluated text:value1->value2->value3.", evaluated);
    }
}
