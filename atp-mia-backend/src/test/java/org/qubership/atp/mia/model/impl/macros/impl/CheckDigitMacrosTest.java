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

package org.qubership.atp.mia.model.impl.macros.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;

public class CheckDigitMacrosTest {

    @Test
    public void evaluate() {
        String[] inputs = {"1234567890"};
        String expectedResult = "3";
        assertEquals(expectedResult, new CheckDigitMacros().evaluate(inputs));
    }

    @Test
    public void evaluate_whenEmptyString_thenReturnNull() {
        String[] inputs = {""};
        assertNull(new CheckDigitMacros().evaluate(inputs));
    }

    @Test
    public void evaluate_whenNonDigidString_thenReturnNull() {
        String[] inputs = {" 123"};
        assertNull(new CheckDigitMacros().evaluate(inputs));
    }

    @Test
    public void evaluate_whenOneDigit_thenReturnDigit() {
        String[] inputs = {"1"};
        assertEquals("8", new CheckDigitMacros().evaluate(inputs));
    }
}
