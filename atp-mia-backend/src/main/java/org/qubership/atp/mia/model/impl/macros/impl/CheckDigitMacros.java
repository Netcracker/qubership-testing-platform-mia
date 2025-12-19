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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.mia.model.impl.macros.Macros;
import org.qubership.atp.mia.model.impl.macros.MacrosType;

public class CheckDigitMacros extends Macros<String> {

    public CheckDigitMacros() {
        super(MacrosType.Check_Digit.name());
    }

    @Override
    @Nullable
    public String evaluate(@Nonnull String[] inputs) {
        String card = inputs[0];
        if (!card.matches("\\d+")) {
            return null;
        }
        /* convert to array of int for simplicity */
        int[] digits = new int[card.length()];
        for (int cardIdx = 0; cardIdx < card.length(); cardIdx++) {
            digits[cardIdx] = Character.getNumericValue(card.charAt(cardIdx));
        }

        /* double every other starting from right - jumping from 2 in 2 */
        for (int digitIdx = digits.length - 1; digitIdx >= 0; digitIdx -= 2) {
            digits[digitIdx] += digits[digitIdx];

            /* taking the sum of digits grater than 10 - simple trick by substract 9 */
            if (digits[digitIdx] >= 10) {
                digits[digitIdx] = digits[digitIdx] - 9;
            }
        }
        int sum = 0;
        for (int i : digits) {
            sum += i;
        }
        /* multiply by 9 step */
        sum = sum * 9;

        /* convert to string to be easier to take the last digit */
        String digit = String.valueOf(sum);
        return digit.substring(digit.length() - 1);
    }
}
