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
import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosIncorrectDateFormatException;
import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosIncorrectDateInputException;

public class DateMacrosTest {

    /**
     * Method under test: default or parameterless constructor of {@link DateMacros}
     */
    @Test
    public void testConstructor() {
        assertEquals("Date_Formatter", (new DateMacros()).getName());
    }

    @Test
    public void evaluate() {
        String[] inputs = {"20190826 12000000", "yyyyMMdd hhmmssSS", "yyyy-MMMM-dd"};
        String expectedResult = "2019-August-26";
        assertEquals(expectedResult, new DateMacros().evaluate(inputs));
    }

    /**
     * Method under test: {@link DateMacros#evaluate(String[])}
     */
    @Test
    public void testEvaluate() {
        assertThrows(MacrosIncorrectDateInputException.class, () -> (new DateMacros()).evaluate(new String[]{"Inputs"}));
        assertThrows(MacrosIncorrectDateInputException.class, () -> (new DateMacros()).evaluate(new String[]{"foo"}));
        assertThrows(MacrosIncorrectDateFormatException.class,
                () -> (new DateMacros()).evaluate(new String[]{": ", "42", ": "}));
        assertEquals(":", (new DateMacros()).evaluate(new String[]{"42", "42", ": "}));
    }
}
