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

package org.qubership.atp.mia.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.qubership.atp.mia.controllers.api.MiaGeneralControllerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class MiaGeneralController implements MiaGeneralControllerApi {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${frontend.variables.path:assets/}")
    protected String feVariablesPath;
    @Value("${frontend.variables.name:env-variables.json}")
    protected String feVariablesFileName;

    @Override
    public ResponseEntity<Map<String, String>> feVariables() {
        try {
            File file = Paths.get(feVariablesPath, feVariablesFileName).toFile();
            log.debug("Fetching FE variables from path: {}", file);
            String jsonContent = FileUtils.readFileToString(file, "UTF-8");
            Map<String, String> feVariables = objectMapper.readValue(
                    jsonContent, new TypeReference<Map<String, String>>() {}
            );
            return ResponseEntity.ok(feVariables);
        } catch (IOException e) {
            log.error("Can't find FE variables Map {}", feVariablesPath);
            try {
                String fallbackJsonContent = FileUtils.readFileToString(
                        new File("../atp-mia-distribution/src/main/resources/env-variables.json"),
                        "UTF-8"
                );
                Map<String, String> feVariables = objectMapper.readValue(
                        fallbackJsonContent,
                        new TypeReference<Map<String, String>>() {
                        }
                );
                return ResponseEntity.ok(feVariables);
            } catch (IOException ex) {
                // Return empty map instead of undefined string on error
                log.error("Can't find default FE variables map");
                return ResponseEntity.ok(new HashMap<>());
            }
        }
    }

    @Override
    public ResponseEntity<String> version() {
        for (String path : new String[]{"config/version.txt", "../atp-mia-distribution/src/main/resources/version.txt"}) {
            try {
                String version = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
                return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(version.trim());
            } catch (IOException ignored) { }
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("undefined");
    }
}
