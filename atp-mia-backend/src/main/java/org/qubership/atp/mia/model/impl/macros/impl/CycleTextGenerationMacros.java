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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import javax.xml.ws.Holder;

import org.qubership.atp.mia.model.impl.macros.Macros;
import org.qubership.atp.mia.model.impl.macros.MacrosType;

public class CycleTextGenerationMacros extends Macros<String> {

    private StringJoiner finalText;
    private String textFormat;
    private String argumentNameCreatesCycle;
    private LinkedHashMap<String, LinkedHashMap<Integer, String>> argNameValues;

    public CycleTextGenerationMacros() {
        super(MacrosType.CycleTextGeneration.name());
    }

    @Override
    public String evaluate(String[] inputs) {
        finalText = new StringJoiner("\n");
        argNameValues = new LinkedHashMap<>();
        if (inputs.length < 4) {
            finalText.add("ERROR IN GENERATION TEXT: NUMBER OF PARAMETERS MACROS SHOULD BE MORE THEN 3");
            addFormatMacros();
        } else {
            textFormat = inputs[0];
            finalText = new StringJoiner(inputs[1].equals("\\n") ? "\n" : inputs[1]);
            argumentNameCreatesCycle = inputs[2].trim();
            if (parseAttributes(inputs) && checkAttributesLength()) {
                for (int argIdx = 0; argIdx < argNameValues.get(argumentNameCreatesCycle).size(); argIdx++) {
                    final LinkedList<String> toAdd = new LinkedList<>();
                    toAdd.add(textFormat);
                    for (Map.Entry<String, LinkedHashMap<Integer, String>> entry : argNameValues.entrySet()) {
                        final String argumentN = entry.getKey();
                        String valueToChange = entry.getValue().get(argIdx);
                        if (valueToChange == null) {
                            valueToChange = entry.getValue().get(0);
                        }
                        toAdd.add(toAdd.get(toAdd.size() - 1).replace("[" + argumentN + "]", valueToChange.trim()));
                    }
                    toAdd.add(toAdd.get(toAdd.size() - 1).replaceAll("\\[\\w+\\]", ""));
                    finalText.add(toAdd.get(toAdd.size() - 1));
                }
            }
        }
        return finalText.toString();
    }

    /**
     * Parses and saves attributes.
     *
     * @param inputs inputs for attributes
     */
    private boolean parseAttributes(String[] inputs) {
        for (int inputIdx = 3; inputIdx < inputs.length; inputIdx++) {
            if (inputs[inputIdx].split("->").length < 2) {
                finalText.add(format("Attribute (%d) of the macros has incorrect format!", inputIdx + 1));
                addFormatMacros();
                return false;
            }
            final String argumentN = inputs[inputIdx].split("->")[0].trim();
            final String argumentV = inputs[inputIdx].split("->")[1].trim();
            final Holder<Boolean> needToString = new Holder<>(false);
            final Holder<Boolean> needToAddSingleQuotationMarks = new Holder<>(false);
            try {
                final String toType = inputs[inputIdx].split("->")[2].trim();
                needToString.value = toType.equalsIgnoreCase("string");
                needToAddSingleQuotationMarks.value = toType.equalsIgnoreCase("singleQuotationMarks");
            } catch (Exception e) {
                //Type is not defined. No need modification.
            }
            Function<String, String> toStringType = (valueToString) -> {
                if (needToString.value && !valueToString.isEmpty()) {
                    return "\"" + valueToString.trim() + "\"";
                }
                if (needToAddSingleQuotationMarks.value && !valueToString.isEmpty()) {
                    return "'" + valueToString.trim() + "'";
                }
                return valueToString;
            };
            LinkedHashMap<Integer, String> argumentValues = new LinkedHashMap<>();
            if (argumentV.startsWith("[")) {
                for (String value : parseArgumentValue(argumentV, -1)) {
                    argumentValues.put(argumentValues.size(), toStringType.apply(value));
                }
            } else {
                argumentValues.put(argumentValues.size(), toStringType.apply(argumentV));
            }
            argNameValues.put(argumentN, argumentValues);
        }
        return true;
    }

    /**
     * Checks for correct length of all attributes.
     *
     * @return true if attributes are OK
     */
    private boolean checkAttributesLength() {
        if (argNameValues.get(argumentNameCreatesCycle) == null) {
            finalText.add(format("No attribute values found (%s)!", argumentNameCreatesCycle));
            addFormatMacros();
            return false;
        } else {
            final int argumentLengthCreatesCycle = argNameValues.get(argumentNameCreatesCycle).size();
            for (Map.Entry<String, LinkedHashMap<Integer, String>> entry : argNameValues.entrySet()) {
                final int valueSize = entry.getValue().size();
                if (valueSize != argumentLengthCreatesCycle && valueSize != 1) {
                    finalText.add(format("Values size (%d) of attribute (%s) is incorrect! Should be %d or 1",
                            valueSize,
                            entry.getKey(), argumentLengthCreatesCycle));
                    addFormatMacros();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Add to result text information about macros format.
     *
     */
    private void addFormatMacros() {
        finalText.add("MACROS format: $ { CycleTextGeneration('Text for generation in cycle [OtherArgumentName1],"
                + " [ArgumentNameCreatesCycle] [OtherArgumentNameN]', 'ArgumentNameCreatesCycle',"
                + " 'OtherArgumentName1-> [1, 2, 3] -> string', 'ArgumentNameCreatesCycle -> [A,B,C] -> string', "
                + "'OtherArgumentNameN "
                + "-> 0' ) }");
        finalText.add("As result will be generated following:");
        finalText.add("Text for generation in cycle 1 A 0");
        finalText.add("Text for generation in cycle 2 B 0");
        finalText.add("Text for generation in cycle 3 B 0");
    }

    /**
     * Parse argument value.
     *
     * @param argumentV    argument value to parse
     * @param replaceIndex index of element for replacement
     * @return parsed argument
     */
    private List<String> parseArgumentValue(String argumentV, int replaceIndex) {
        final ArrayList<String> returnValue = new ArrayList<>();
        int elementIndex = -1;
        int bracketNum = 0;
        int startElementId = -1;
        int endElementId = -1;
        int startBlockId = -1;
        int endBlockId = -1;
        argumentV = argumentV.replaceAll("\\[\\]", "");
        for (int symbolId = 0; symbolId < argumentV.length(); symbolId++) {
            if (argumentV.charAt(symbolId) == '[') {
                if (bracketNum == 0) {
                    startElementId = symbolId;
                    startBlockId = symbolId;
                }
                bracketNum++;
            }
            if (argumentV.charAt(symbolId) == ']' && bracketNum > 0) {
                bracketNum--;
            }
            if (argumentV.charAt(symbolId) == ']' && bracketNum == 0 || symbolId == argumentV.length() - 1) {
                endElementId = symbolId;
                endBlockId = symbolId;
            }
            if (argumentV.charAt(symbolId) == ',' && argumentV.charAt(symbolId + 1) == ' ' && bracketNum == 1) {
                endElementId = symbolId;
            }
            if (endElementId > 0) {
                elementIndex++;
                String block = argumentV.substring(startElementId + 1, endElementId);
                while (block.contains("[") && block.contains("]")) {
                    block = parseArgumentValue(block, elementIndex).get(0);
                }
                startElementId = symbolId + 1;
                endElementId = -1;
                returnValue.add(block);
            }
            if (endBlockId > 0 && replaceIndex >= 0) {
                final String replaceValue = returnValue.size() > replaceIndex ? returnValue.get(replaceIndex) : "";
                return Collections.singletonList(
                        new StringBuffer(argumentV).replace(startBlockId, endBlockId + 1, replaceValue).toString());
            }
        }
        if (returnValue.isEmpty()) {
            returnValue.add(argumentV);
        }
        return returnValue;
    }
}
