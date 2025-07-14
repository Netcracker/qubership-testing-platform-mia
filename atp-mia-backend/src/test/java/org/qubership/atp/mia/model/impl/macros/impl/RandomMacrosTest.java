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
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosRandomIncorrectFormatException;
import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosRandomIncorrectLengthException;

public class RandomMacrosTest {

    @Test
    public void evaluate() {
        String[] inputs = {"10"};
        String actualResult = new RandomMacros().evaluate(inputs);
        assertEquals(true, Integer.valueOf(actualResult) >= 0
                && Integer.valueOf(actualResult) < Integer.valueOf(inputs[0]));
    }

    @Test
    public void evaluate_negativeTest() {
        assertThrows(MacrosRandomIncorrectFormatException.class,
                () -> (new RandomMacros()).evaluate(new String[]{"Inputs"}));
        assertThrows(MacrosRandomIncorrectFormatException.class, () -> (new RandomMacros()).evaluate(new String[]{"foo"}));
        assertThrows(MacrosRandomIncorrectLengthException.class, () -> (new RandomMacros()).evaluate(new String[]{}));
    }
}
