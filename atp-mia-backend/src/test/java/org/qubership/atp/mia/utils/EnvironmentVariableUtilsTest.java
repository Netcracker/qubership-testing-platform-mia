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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.exceptions.macrosandevaluations.IncorrectEnvironmentVariableFormatException;
import org.springframework.boot.test.mock.mockito.SpyBean;

@ExtendWith(SkipTestInJenkins.class)
public class EnvironmentVariableUtilsTest extends ConfigTestBean {

    @SpyBean
    protected EnvironmentVariableUtils environmentVariableUtils;

    private void verify(String expected, String actual) {
        assertEquals(expected, miaContext.get().evaluate(actual));
    }

    @Test
    public void evaluate_ENV_variable_toGet_db_user() {
        verify("mia", "${ENV.Billing System.DB.db_login}");
    }

    @Test
    public void evaluate_ENV_variable_toGet_ssh_user() {
        verify("root", "${ENV.Billing System.SSH.ssh_login}");
    }

    @Test
    public void evaluate_ENV_variable_WhenSystemIsNotPresent() {
        verify("${ENV.Business Solution.DB.db_type}", "${ENV.Business Solution.DB.db_type}");
    }

    @Test
    public void evaluate_ENV_variable_WhenConnectionIsNotPresent() {
        verify("${ENV.Billing System.SSH.login}", "${ENV.Billing System.SSH.login}");
    }

    @Test
    public void evaluate_ENV_variable_WhenParameterNotPresent() {
        verify("${ENV.Billing System.DB.db_user_name}", "${ENV.Billing System.DB.db_user_name}");
    }

    @Test
    public void evaluate_ENV_variable_WrongFormatExtraWord() {
        assertThrows(IncorrectEnvironmentVariableFormatException.class, () ->
                miaContext.get().evaluate("${ENV.Billing System.DB.db_type.extraWordByMistake}")
        );
    }

    @Test
    public void evaluate_ENV_variable_WrongFormatFewerWords() {
        assertThrows(IncorrectEnvironmentVariableFormatException.class, () ->
                miaContext.get().evaluate("${ENV.Billing System.DB}")
        );
    }
}
