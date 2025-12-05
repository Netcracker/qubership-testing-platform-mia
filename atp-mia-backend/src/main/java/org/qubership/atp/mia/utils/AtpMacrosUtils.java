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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.qubership.atp.macros.core.client.MacrosFeignClient;
import org.qubership.atp.macros.core.converter.MacrosDtoConvertService;
import org.qubership.atp.macros.core.model.Macros;
import org.qubership.atp.macros.core.repository.MacrosRepository;
import org.qubership.atp.mia.service.execution.MacrosService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class AtpMacrosUtils {

    private static MacrosRepository macrosRepositoryStatic;
    private static MacrosService macrosServiceStatic;
    private static boolean macrosEnableStatic;
    private final MacrosFeignClient macrosFeignClient;
    private final MacrosService macrosService;
    private final MacrosDtoConvertService macrosDtoConvertService;
    @Value("${feign.atp.macros.enabled}")
    private boolean macrosEnable;

    /**
     * Replaces ATP macros with its values in a given string.
     */
    public static String evaluateWithAtpMacros(String texToEvaluate, UUID projectId) {
        log.info("evaluateWithAtpMacros start");
        if (macrosEnableStatic) {
            List<Macros> macros = macrosServiceStatic.getMacros(projectId, macrosRepositoryStatic);
            Optional<Macros> macro = macros
                    .stream()
                    .filter(m -> texToEvaluate.contains(m.getName()))
                    .findFirst();
            if (macro.isPresent()) {
                return macrosRepositoryStatic.evaluate(texToEvaluate);
            }
        }
        log.info("evaluateWithAtpMacros ends");
        return texToEvaluate;
    }

    /**
     * Initialize static variables.
     */
    @PostConstruct
    public void init() {
        macrosRepositoryStatic = new MacrosRepository(macrosFeignClient, macrosDtoConvertService);
        macrosServiceStatic = macrosService;
        macrosEnableStatic = macrosEnable;
    }
}
