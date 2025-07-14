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

package org.qubership.atp.mia.utils.dos2unix.impl;

import static org.qubership.atp.mia.utils.dos2unix.utils.Dos2UnixUtils.destinationToRealPath;
import static org.qubership.atp.mia.utils.dos2unix.utils.Dos2UnixUtils.getDos2UnixPath;

import java.nio.file.Path;
import java.util.concurrent.Executors;

import org.qubership.atp.mia.exceptions.dos2unix.Dos2unixWindowsFailException;
import org.qubership.atp.mia.utils.dos2unix.Dos2Unix;
import org.qubership.atp.mia.utils.dos2unix.utils.StreamGobbler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WindowsDos2Unix implements Dos2Unix {

    private final String pathToDos2Unix;

    public WindowsDos2Unix(String pathToDos2Unix) {
        this.pathToDos2Unix = pathToDos2Unix;
    }

    @Override
    public void runDos2Unix(Path destSource) {
        log.info("Run dos2unix for windows");
        String dosPath = getDos2UnixPath(pathToDos2Unix).toString();
        String dest = destinationToRealPath(destSource).toString();
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("cmd.exe", "/c", dosPath, dest);
        log.info("Command builder ready, running...");
        int exitVal = 0;
        try {
            Process process = builder.start();
            Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getInputStream(), log::trace));
            Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getErrorStream(), log::trace));
            exitVal = process.waitFor();
        } catch (Exception e) {
            throw new Dos2unixWindowsFailException(e);
        }
        if (exitVal == 0) {
            log.info("File conversion to Unix Format is Success! Path: {}", dest);
        } else {
            log.error("File conversion to Unix Format is Fail! & Command Exit Value is : {}. Path: {}", exitVal, dest);
        }
    }
}
