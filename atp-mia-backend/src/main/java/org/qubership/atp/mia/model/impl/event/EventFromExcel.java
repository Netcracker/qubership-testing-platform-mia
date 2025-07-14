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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.utils.ExcelParserHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Class for parsing excel file in case generating events.
 */
@Slf4j
public class EventFromExcel {

    public static final String ERROR_MESSAGE = "Please add field and provide path to Excel file in it";

    @Nonnull
    private final XSSFSheet sheet;
    @Nonnull
    private EventExcelCustomer eventExcelCustomer;

    /**
     * Defines and parses excel file.
     *
     * @param miaContext miaContext
     * @throws IllegalArgumentException in case incorrect parsing Excel file
     */
    public EventFromExcel(MiaContext miaContext) throws IllegalArgumentException {
        final Map<String, String> params = miaContext.getFlowData().getParameters();
        if (!params.containsKey(Constants.EVENT_GENERATION_FILE)) {
            throw new IllegalArgumentException("Field with name " + Constants.EVENT_GENERATION_FILE + " is not "
                    + "specified. " + ERROR_MESSAGE);
        }
        this.sheet = ExcelParserHelper.getSheet(
                miaContext.getUploadsPath().resolve(params.get(Constants.EVENT_GENERATION_FILE)),
                params.get(Constants.EVENT_GENERATION_SHEET));
        this.eventExcelCustomer = this.defineCustomerOfExcelFile(params.get(Constants.EVENT_GENERATION_SCENARIO),
                params.get(Constants.EVENT_GENERATION_TC));
        this.saveAttributesIntoFlowData(miaContext.getFlowData());
    }

    /**
     * Saves Event parameters to flowData.
     */
    private void saveAttributesIntoFlowData(FlowData flowData) {
        eventExcelCustomer.getAttrNameAndValue().forEach((name, value) -> {
            flowData.addParameter(name, value.toString());
            log.trace("Saved to flowData: {} with value {}", name, value);
        });
    }

    /**
     * Remove Event parameters from flowData.
     */
    public void removeEventAttributesFromFlowData(FlowData flowData) {
        eventExcelCustomer.getAttrNameAndValue().keySet().forEach(flowData::removeParameter);
    }

    /**
     * Defines customer of excel file.
     *
     * @return Customer of excel file
     */
    private EventExcelCustomer defineCustomerOfExcelFile(@Nullable String scenario,
                                                         @Nullable String testCase) {
        EventExcelCustomer eventExcel = new EventExcelProjectFive(sheet, scenario, testCase);
        if (eventExcel.isCurrentFormat()) {
            log.info("Parse Excel file as Telekom");
            eventExcel.parse();
            return eventExcel;
        }
        eventExcel = new EventExcelProjectFour(sheet, scenario, testCase);
        if (eventExcel.isCurrentFormat()) {
            log.info("Parse Excel file as O2UK");
            eventExcel.parse();
            return eventExcel;
        }
        eventExcel = new EventExcelProjectOne(sheet, scenario, testCase);
        if (eventExcel.isCurrentFormat()) {
            log.info("Parse Excel file as Project One");
            eventExcel.parse();
            return eventExcel;
        }
        throw new IllegalArgumentException("Customer of Excel file is not defined");
    }
}
