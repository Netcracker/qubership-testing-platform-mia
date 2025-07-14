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

import org.junit.Test;

public class CycleTextGenerationMacrosTest {

    /**
     * Method under test: default or parameterless constructor of
     * {@link CycleTextGenerationMacros}
     */
    @Test
    public void testConstructor() {
        assertEquals("CycleTextGeneration", (new CycleTextGenerationMacros()).getName());
    }

    /**
     * Method under test: {@link CycleTextGenerationMacros#evaluate(String[])}
     */
    @Test
    public void testEvaluate() {
        assertEquals(
                "ERROR IN GENERATION TEXT: NUMBER OF PARAMETERS MACROS SHOULD BE MORE THEN 3\n"
                        + "MACROS format: $ { CycleTextGeneration('Text for generation in cycle [OtherArgumentName1], "
                        + "[ArgumentNameCreatesCycle] [OtherArgumentNameN]', 'ArgumentNameCreatesCycle', 'OtherArgumentName1->"
                        + " [1, 2, 3] -> string', 'ArgumentNameCreatesCycle -> [A,B,C] -> string', 'OtherArgumentNameN -> 0' ) }"
                        + "\n" + "As result will be generated following:\n" + "Text for generation in cycle 1 A 0\n"
                        + "Text for generation in cycle 2 B 0\n" + "Text for generation in cycle 3 B 0",
                (new CycleTextGenerationMacros()).evaluate(new String[]{"Inputs"}));
    }
}
