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

package org.qubership.atp.mia.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.ContentType;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.impl.FlowData;
import org.qubership.atp.mia.model.impl.VariableFormat;
//@Disabled("Temporarily disabled for refactoring")
@ExtendWith(SkipTestInJenkins.class)
public class UtilsTest extends ConfigTestBean {

    private static final String DEFAULT_VAR_FORMAT = ":\\(VARIABLE_NAME\\)";
    private static final Pattern uuidPattern =
            Pattern.compile("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})");
    private String headerName = "Content-Disposition";

    private void macroAssert(String actualInput, String expected) {
        String actual = miaContext.get().evaluate(actualInput);
        String macroFailMsg = String.format("Macros failed. actual [%s]\n is not equal to expected [%s].",
                actual, expected);
        assertEquals(macroFailMsg, expected, actual);
    }

    HttpResponse response = Mockito.mock(HttpResponse.class);

    @Test
    public void evaluateMacros_whenSeveralSameInLine() {
        String actual = "\nRandom ${Random(1)} Random ${Random(1)} Random ${Random(1)}";
        String expected = "\nRandom 0 Random 0 Random 0";
        macroAssert(actual, expected);
    }

    @Test
    public void evaluateMacros_whenSeveralDifferentInLine() {
        String actual = "\nRandom ${Random(1)}\nRandom ${Random(1)}\nRandom ${Random(1)}"
                + "Date ${Date_Formatter(20190826 12000000, yyyyMMdd hhmmssSS, yyyy-MMMM-dd)}"
                + "\nTimestamp ${Timestamp(YYYY)}";
        String expected = "\nRandom 0\nRandom 0\nRandom 0"
                + "Date 2019-August-26"
                + "\nTimestamp " + new SimpleDateFormat("YYYY").format(Calendar.getInstance().getTime());
        macroAssert(actual, expected);
    }

    @Test
    public void evaluateMacros_whenSeveralMacros_andSymbolsBeforeThem() {
        String actual = "\nRandom \"${Random(1)}\"\nRandom /${Random(1)}\nRandom $$${Random(1)}"
                + " Date {{}}${Date_Formatter(20190826 12000000, yyyyMMdd hhmmssSS, yyyy-MMMM-dd)}"
                + "\nTimestamp ${Timestamp(YYYY)}";
        String expected = "\nRandom \"0\"\nRandom /0\nRandom $$0"
                + " Date {{}}2019-August-26"
                + "\nTimestamp " + new SimpleDateFormat("YYYY").format(Calendar.getInstance().getTime());
        macroAssert(actual, expected);
    }

    private void macroAssert(String actualInput, Pattern expectedPattern, int times) {
        times = Math.max(times, 1);
        Matcher matcher = expectedPattern.matcher(miaContext.get().evaluate(actualInput));
        String actual = miaContext.get().evaluate(actualInput);
        for (int i = 1; i <= times; i++) {
            String macroFailMsg = String.format("Macros error! Matcher failed at the %d iteration of find(),"
                            + "Found %d occurrences of expected pattern: [%s], in actual string: [%s], but need [%d]",
                    i, i - 1, expectedPattern.pattern(), actual, i);
            assertTrue(macroFailMsg, matcher.find());
        }
    }

    @Test
    public void evaluateMacros_whenSeveralMacros_andSymbolsBeforeThem2() {
        String actual = "\nRandom \"${Random(1)}\"\nRandom /${Random(1)}\nRandom ${Random(1)}"
                + "Date ${Date_Formatter(20190826 12000000, yyyyMMdd hhmmssSS, yyyy-MMMM-dd)}"
                + "\nTimestamp ${Timestamp(YYYY)}";
        String expected = "\nRandom \"0\"\nRandom /0\nRandom 0"
                + "Date 2019-August-26"
                + "\nTimestamp " + new SimpleDateFormat("YYYY").format(Calendar.getInstance().getTime());
        macroAssert(actual, expected);
    }

    @Test
    public void evaluateMacrosUuidGen_whenSeveralDifferentInLine() {
        String actual = " uuid ${GenerateUuid()} "
                + "Random ${Random(1)} uuid ${GenerateUuid()}"
                + " Random ${Random(1)} Random ${Random(1)}";
        macroAssert(actual, uuidPattern, 2);
    }

    @Test
    public void evaluateMacros_whenOneAtTime() {
        String actual = "Date ${Date_Formatter(20190826 12000000, yyyyMMdd hhmmssSS, yyyy-MMMM-dd)}";
        String expectedResult = "Date 2019-August-26";
        macroAssert(actual, expectedResult);
        actual += "\nRandom ${Random(1)}";
        expectedResult += "\nRandom 0";
        macroAssert(actual, expectedResult);
        actual += "\nCheckDigit ${Check_Digit(123456789)}";
        expectedResult += "\nCheckDigit 7";
        macroAssert(actual, expectedResult);
        actual += "\nCycleTextGeneration ${CycleTextGeneration('EVENT: [EventAttr1],[EventAttr2],[EventAttr3],"
                + "[EventAttr4],[EventAttr5]', '\\n', 'EventAttr2', 'EventAttr1->[0, 1, 2]',"
                + "'EventAttr2->[9, 8, 7]', 'EventAttr3-> Z -> string', 'EventAttr5-> [I, , II] -> string')}";
        expectedResult += "\nCycleTextGeneration EVENT: 0,9,\"Z\",,\"I\"\nEVENT: 1,8,\"Z\",,\nEVENT: 2,7,\"Z\",,\"II\"";
        macroAssert(actual, expectedResult);
        actual += "\nTimestamp ${Timestamp(YYYY)}";
        expectedResult += "\nTimestamp " + new SimpleDateFormat("YYYY").format(Calendar.getInstance().getTime());
        assertEquals(expectedResult, miaContext.get().evaluate(actual));
    }

    @Test
    public void evaluateMacros_whenJsonExample() {
        String actual = "{\n"
                + "\"metadata\": {\n"
                + "\"name\": \"${GenerateUuid()}\",\n"
                + "\"labels\": {},\n"
                + "\"annotations\": {\n"
                + "\"username\": \"${Random(1)}\"\n"
                + "}\n"
                + "},\n"
                + "\"spec\": {\n"
                + "\"definition_name\": \"EFD\",\n"
                + "\"params\": [{}\n"
                + "],\n"
                + "\"enable_tracing\": false\n"
                + "}\n"
                + "}";
        macroAssert(actual, uuidPattern, 1);
        actual = miaContext.get().evaluate(actual);
        Matcher m = uuidPattern.matcher(actual);
        Assert.assertTrue(m.find());
        String uuid = m.group();
        String expected = String.format("{\n"
                + "\"metadata\": {\n"
                + "\"name\": \"%s\",\n"
                + "\"labels\": {},\n"
                + "\"annotations\": {\n"
                + "\"username\": \"0\"\n"
                + "}\n"
                + "},\n"
                + "\"spec\": {\n"
                + "\"definition_name\": \"EFD\",\n"
                + "\"params\": [{}\n"
                + "],\n"
                + "\"enable_tracing\": false\n"
                + "}\n"
                + "}", uuid);
        macroAssert(actual, expected);
    }

    @Test
    public void evaluate_evaluateParameters_whenUseDefaultFormat() {
        FlowData flowData = miaContext.get().getFlowData();
        flowData.addParameter("variable9", "+9(:variable8)");
        flowData.addParameter("variable1", "1");
        flowData.addParameter("variable5", "+5(:variable4)");
        flowData.addParameter("variable2", "+2(:variable1)");
        flowData.addParameter("variable4", "+4(:variable3)");
        flowData.addParameter("variable6", "+6(:variable5)");
        flowData.addParameter("variable7", "+7(:variable6)");
        flowData.addParameter("variable8", "+8(:variable9)");
        flowData.addParameter("variable3", "+3(:variable2)");
        flowData.addParameter("projectId", "46578a43-4cfb-46e4-80b8-95ce21151a9f");
        String text = miaContext.get().evaluate("Text includes :variable1 and :variable2 and :variable3 and :variable4 and "
                + ":variable5 and :variable6 and :variable8 and :variable9");
        assertEquals("Text includes 1 and +2(1) and +3(+2(1)) and +4(+3(+2(1))) and +5(+4(+3(+2(1)))) and "
                        + "+6(+5(+4(+3(+2(1))))) and +8(+9(+8(+9(+8(+9(+8(+9(+8(:variable9))))))))) and "
                        + "+9(+8(+9(+8(+9(+8(+9(+8(:variable9))))))))",
                text);
    }

    @Test
    public void evaluate_evaluateVariableInsideVariableInLoop_whenUseVariablesInsideVariable() {
        FlowData flowData = miaContext.get().getFlowData();
        CommonConfiguration commonConfiguration = miaContext.get().getConfig().getCommonConfiguration();
        commonConfiguration.setUseVariablesInsideVariable(true);
        commonConfiguration.setVariableFormat(":\\(" + VariableFormat.VAR_NAME + "\\)");
        flowData.addParameter("projectId", "46578a43-4cfb-46e4-80b8-95ce21151a9f");
        flowData.addParameter("variable9", "+9(:(variable8))");
        flowData.addParameter("variable1", "1");
        flowData.addParameter("variable5", "+5(:(variable4))");
        flowData.addParameter("variable2", "+2(:(variable1))");
        flowData.addParameter("variable4", "+4(:(variable3))");
        flowData.addParameter("variable6", "+6(:(variable5))");
        flowData.addParameter("variable7", "+7(:(variable6))");
        flowData.addParameter("variable8", "+8(:(variable9))");
        flowData.addParameter("variable3", "+3(:(variable2))");
        String text = miaContext.get().evaluate("Text includes :(variable1) and :(variable2) and :(variable3) and :(variable4) and "
                + ":(variable5) and :(variable6) and :(variable8) and :(variable9)");
        assertEquals("Text includes 1 and +2(1) and +3(+2(1)) and +4(+3(+2(1))) and +5(+4(+3(+2(1)))) and "
                        + "+6(+5(+4(+3(+2(1))))) and +8(+9(+8(+9(+8(+9(+8(+9(+8(:(variable9)))))))))) and "
                        + "+9(+8(+9(+8(+9(+8(+9(+8(:(variable9)))))))))",
                text);
        flowData.setParameters(new HashMap<>());
        flowData.addParameter("projectId", "46578a43-4cfb-46e4-80b8-95ce21151a9f");
        flowData.addParameter("variable8", "(8+:(variable7))");
        flowData.addParameter("variable5", "(:(variable1)+:(variable2)+:(variable3)+:(variable4)+5)");
        flowData.addParameter("variable2", "(:(variable1)+2)");
        flowData.addParameter("variable4", "(:(variable1)+:(variable2)+:(variable3)+4)");
        flowData.addParameter("variable7", "(7+:(variable8))");
        flowData.addParameter("variable3", "(:(variable1)+:(variable2)+3)");
        flowData.addParameter("variable1", "1");
        flowData.addParameter("toEvaluate",
                "Text includes :(variable1) and :(variable2) and :(variable3) and :(variable4) "
                        + "and :(variable5) and :(variable6) and :(variable7) and :(variable8)");
        flowData.addParameter("variable6", "(:(variable1)+:(variable2)+:(variable3)+:(variable4)+:(variable5)+6)");
        text = miaContext.get().evaluate(flowData.getParameters().get("toEvaluate"));
        assertEquals("Text includes 1 and (1+2) and (1+(1+2)+3) and (1+(1+2)+(1+(1+2)+3)+4) and "
                        + "(1+(1+2)+(1+(1+2)+3)+(1+(1+2)+(1+(1+2)+3)+4)+5) and "
                        + "(1+(1+2)+(1+(1+2)+3)+(1+(1+2)+(1+(1+2)+3)+4)+(1+(1+2)+(1+(1+2)+3)+(1+(1+2)+(1+(1+2)+3)+4)+5)+6) and "
                        + "(7+(8+(7+(8+(7+(8+(7+(8+(7+:(variable8)))))))))) and (8+(7+(8+(7+(8+(7+(8+(7+:(variable8)))))))))",
                text);
    }

    @Test
    public void evaluate_shouldThrow_whenDefaultVariableFormatAndUseVariablesInsideVariable() {
        miaContext.get().getConfig().getCommonConfiguration().setUseVariablesInsideVariable(true);
        miaContext.get().getFlowData().addParameter("variable1", "1");
        assertEquals("1", miaContext.get().evaluate(":variable1"));
    }

    @Test
    public void evaluateMacros_shouldWorkWithSingleInput() {
        String expected = "echo 0";
        String text = "echo :(var)";
        String k = "var";
        String v = "${Random(1)}";
        FlowData flowData = getFlowData(DEFAULT_VAR_FORMAT, true, null);
        flowData.addParameter(k, v);
        flowData.addParameter("projectId", "46578a43-4cfb-46e4-80b8-95ce21151a9f");
        String res = miaContext.get().evaluate(text);
        Assert.assertEquals(expected, res);
    }

    @Test
    public void evaluateMacros_shouldWorkWithVariableAndMacros() {
        String expected = "echo 0";
        String text = "echo :(macros)";
        FlowData flowData = getFlowData(DEFAULT_VAR_FORMAT, true, new HashMap<String, String>() {{
            put("macros", "${Random(1)}");
            put("var", ":(macros)");
        }});
        flowData.addParameter("projectId", "46578a43-4cfb-46e4-80b8-95ce21151a9f");
        String res = miaContext.get().evaluate(text);
        Assert.assertEquals(expected, res);
    }

    @Test
    public void whenContentDispositionPresent_returnTrue() {
        boolean res = Utils.isHeaderNamePresent(constructAndStubHttpResponse(headerName, "filename=text.zip"), headerName);
        assertTrue(res);
    }

    @Test
    public void whenContentDispositionNotPresent_isHeaderNamePresentReturnFalse() {
        String headerName = "Content-Type";
        assertFalse(Utils.isHeaderNamePresent(constructAndStubHttpResponse(headerName, "application/zip"), "Content-Disposition"));
    }

    @Test
    public void whenContentDispositionHaveFilenameWithoutQuote_returnFileName() {
        String headerValue = Utils.getHeaderValue(constructAndStubHttpResponse(headerName, "filename=text.zip"), headerName);
        String name = Utils.getFirstGroupFromStringByRegexp(headerValue, "filename=(.*)");
        assertEquals("filename=text.zip", headerValue);
        assertEquals("text.zip", name);
    }

    @Test
    public void whenContentDispositionHaveFilenameWithQuote_returnFileName() {
        String headerValue = Utils.getHeaderValue(constructAndStubHttpResponse(headerName, "filename=\"test.pdf\""), headerName);
        String name = Utils.getFirstGroupFromStringByRegexp(headerValue, "filename=(.*)");
        assertEquals("filename=\"test.pdf\"", headerValue);
        assertEquals("\"test.pdf\"", name);
    }

    @Test
    public void whenContentDispositionNotHaveFilename_returnFileName() {
        String headerValue = Utils.getHeaderValue(constructAndStubHttpResponse(headerName, "application/zip filename="), headerName);
        String name = Utils.getFirstGroupFromStringByRegexp(headerValue, "filename=(.*)");
        assertTrue(name.length() == 0);
    }

    @Test
    public void whenNotContentDisposition_returnFilename() {
        FlowData flowData = getFlowData(DEFAULT_VAR_FORMAT, true, null);
        flowData.addParameter("processName", "abcd");
        String res = miaContext.get().createFileName(ContentType.pdf);
        assertNotNull(res);
    }

    private FlowData getFlowData(String varFormat, boolean varInsideVar, Map<String, String> params) {
        CommonConfiguration commonConfiguration = miaContext.get().getConfig().getCommonConfiguration();
        commonConfiguration.setVariableFormat(varFormat);
        commonConfiguration.setUseVariablesInsideVariable(varInsideVar);
        if (params != null) {
            params.forEach(miaContext.get().getFlowData()::addParameter);
        }
        return miaContext.get().getFlowData();
    }

    private HttpResponse constructAndStubHttpResponse(String name, String value) {
        //construct
        Header[] headers = new Header[]{new BasicHeader(name, value)};
        //stub
        when(response.getAllHeaders()).thenReturn(headers);
        return response;
    }
}
