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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.qubership.atp.mia.exceptions.macrosandevaluations.MacrosTimestampIncorrectFormatException;
import org.qubership.atp.mia.model.impl.macros.Macros;
import org.qubership.atp.mia.model.impl.macros.MacrosType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimestampMacros extends Macros<String> {

    public TimestampMacros() {
        super(MacrosType.Timestamp.name());
    }

    @Override
    public String evaluate(String[] inputs) {
        if (inputs.length != 1) {
            throw new MacrosTimestampIncorrectFormatException();
        }
        return new SimpleDateFormat(inputs[0]).format(Calendar.getInstance().getTime());
    }
}
