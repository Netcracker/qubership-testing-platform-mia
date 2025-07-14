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

import org.apache.commons.io.FileUtils;
import org.qubership.atp.mia.controllers.api.MiaGeneralControllerApi;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MiaGeneralController implements MiaGeneralControllerApi {

    @Override
    public ResponseEntity<String> version() {
        try {
            String version = FileUtils.readFileToString(new File("config/version.txt"), "UTF-8");
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(version);
            //return ResponseEntity.ok(FileUtils.readFileToString(new File("config/version.txt"), "UTF-8"));
        } catch (IOException e) {
            try {
                String version = FileUtils.readFileToString(
                        new File("../atp-mia-distribution/src/main/resources/version.txt"), "UTF-8");
                return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(version);
                //return ResponseEntity.ok(FileUtils.readFileToString(
                // new File("../atp-mia-distribution/src/main/resources/version.txt"), "UTF-8"));
            } catch (IOException ex) {
                return ResponseEntity.ok("undefined");
            }
        }
    }
}
