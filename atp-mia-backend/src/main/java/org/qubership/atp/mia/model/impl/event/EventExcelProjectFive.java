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

package org.qubership.atp.mia.model.impl.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.xssf.usermodel.XSSFSheet;

import clover.org.apache.commons.lang3.Range;

public class EventExcelProjectFive extends EventExcelProjectFour {

    EventExcelProjectFive(@Nonnull XSSFSheet sheet, @Nullable String scenario, @Nullable String testCase) {
        super(sheet, scenario, testCase);
        eventInfoRange = Range.between(4, 28);
        identifications.put("Partial Event Identifier", false);
        identifications.put("Sequence", false);
        identifications.put("Record Type", false);
    }
}
