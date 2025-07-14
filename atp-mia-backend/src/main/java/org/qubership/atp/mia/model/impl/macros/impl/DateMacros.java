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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosIncorrectDateFormatException;
import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosIncorrectDateInputException;
import org.qubership.atp.mia.model.impl.macros.Macros;
import org.qubership.atp.mia.model.impl.macros.MacrosType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateMacros extends Macros<String> {

    public DateMacros() {
        super(MacrosType.Date_Formatter.name());
    }

    @Override
    public String evaluate(String[] inputs) {
        if (inputs.length != 3) {
            throw new MacrosIncorrectDateInputException();
        }
        String currentDateVal = inputs[0].trim();
        Locale locale = Locale.getDefault();
        SimpleDateFormat currentFormat = new SimpleDateFormat(inputs[1].trim(), locale);
        SimpleDateFormat newFormat = new SimpleDateFormat(inputs[2].trim(), locale);
        try {
            Date date = currentFormat.parse(currentDateVal);
            return newFormat.format(date);
        } catch (ParseException e) {
            throw new MacrosIncorrectDateFormatException(currentDateVal, currentFormat, e);
        }
    }
}
