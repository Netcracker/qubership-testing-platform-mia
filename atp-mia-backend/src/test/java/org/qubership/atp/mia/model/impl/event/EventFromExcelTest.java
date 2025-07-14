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

import static org.junit.Assert.assertEquals;
import static org.qubership.atp.mia.model.Constants.EVENT_GENERATION_FILE;
import static org.qubership.atp.mia.model.Constants.EVENT_GENERATION_SCENARIO;
import static org.qubership.atp.mia.model.Constants.EVENT_GENERATION_SHEET;
import static org.qubership.atp.mia.model.Constants.EVENT_GENERATION_TC;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.utils.FileUtils;
import org.springframework.boot.test.mock.mockito.SpyBean;

@ExtendWith(SkipTestInJenkins.class)
public class EventFromExcelTest extends ConfigTestBean {

    @SpyBean
    EventExcelMetricService eventExcelMetricService;

    @Test
    public void eventGenerationFromFile_ProjectFour() throws IOException {
        eventExcelMetricService.init();
        FlowData flowData = miaContext.get().getFlowData();
        miaContext.get().getFlowData().setParameters(null);
        FileUtils.copyFile(Paths.get("./src/test/resources/EventGeneration/Rating_Matrix_ProjectFour.xlsx"),
                miaContext.get().getUploadsPath().resolve("Rating_Matrix_ProjectFour.xlsx"));
        flowData.addParameter(EVENT_GENERATION_FILE, "Rating_Matrix_ProjectFour.xlsx");
        EventFromExcel eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[, , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , "
                        + ", , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , ,"
                        + " , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , , ]",
                flowData.getParameters().get("EventAttr13"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.addParameter(EVENT_GENERATION_SHEET, "Rating Matrix");
        flowData.addParameter(EVENT_GENERATION_SCENARIO, "Enterprise Voice - NYC to Nassau");
        flowData.addParameter(EVENT_GENERATION_TC, "TC.1.1");
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[J0075]", flowData.getParameters().get("EventAttr4"));
        assertEquals("[AMITYVILLE]", flowData.getParameters().get("EventAttr10"));
        assertEquals("[333227]", flowData.getParameters().get("EventAttr18"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.removeParameter(EVENT_GENERATION_SCENARIO);
        flowData.addParameter(EVENT_GENERATION_TC, "TC.6.2");
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[854701, 854702, 854703, 854704, 854705]", flowData.getParameters().get("EventAttr17"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.removeParameter(EVENT_GENERATION_SHEET);
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[2223334455, 2223334455, 2223334455, 2223334455, 2223334455]",
                flowData.getParameters().get("EventAttr1"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.addParameter(EVENT_GENERATION_TC, "TC.1.*");
        flowData.addParameter(EVENT_GENERATION_SCENARIO, "INTL - A.*");
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[15, 100]", flowData.getParameters().get("EventAttr3"));
        assertEquals("[3, 3]", flowData.getParameters().get("EVENT_TYPE_ID"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
    }

    @Test
    public void eventGenerationFromFile_ProjectFive() throws IOException {
        FlowData flowData = miaContext.get().getFlowData();
        flowData.setParameters(new HashMap<>());
        FileUtils.copyFile(Paths.get("./src/test/resources/EventGeneration/Rating_Matrix_ProjectFive.xlsx"),
                miaContext.get().getUploadsPath().resolve("Rating_Matrix_ProjectFive.xlsx"));
        flowData.addParameter(EVENT_GENERATION_FILE, "Rating_Matrix_ProjectFive.xlsx");
        flowData.addParameter(EVENT_GENERATION_SHEET, "Rating Matrix");
        flowData.addParameter(EVENT_GENERATION_SCENARIO, "Enterprise Voice - NYC to Nassau");
        flowData.addParameter(EVENT_GENERATION_TC, "TC.1.1");
        EventFromExcel eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[O]", flowData.getParameters().get("EventAttr4"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.removeParameter(EVENT_GENERATION_SCENARIO);
        flowData.addParameter(EVENT_GENERATION_TC, "TC.2.1.E2E.TV.EOS");
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[V, P]", flowData.getParameters().get("EventAttr3"));
        assertEquals("[, ]", flowData.getParameters().get("EventAttr18"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.removeParameter(EVENT_GENERATION_SHEET);
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[V, P]", flowData.getParameters().get("EventAttr3"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
        flowData.addParameter(EVENT_GENERATION_TC, "TC.2.1.E2E.INT.*");
        flowData.addParameter(EVENT_GENERATION_SCENARIO, "Fixed Fiber.*");
        eventFromExcel = new EventFromExcel(miaContext.get());
        assertEquals("[104857600, 41943040, 325058560, 246415360]", flowData.getParameters().get("EventAttr1"));
        eventFromExcel.removeEventAttributesFromFlowData(miaContext.get().getFlowData());
    }
}
