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

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;

import org.qubership.atp.mia.exceptions.dos2unix.Dos2unixLinuxFailException;
import org.qubership.atp.mia.utils.dos2unix.Dos2Unix;
import org.qubership.atp.mia.utils.dos2unix.utils.StreamGobbler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LinuxDos2Unix implements Dos2Unix {

    private static final String PERMISSION_ERROR = "Permission denied";
    private static final String DOS2UNIX = "dos2unix";

    private final String pathToDos2Unix;

    public LinuxDos2Unix(String pathToDos2Unix) {
        this.pathToDos2Unix = pathToDos2Unix;
    }

    @Override
    public void runDos2Unix(Path destSource) {
        log.info("Run dos2unix for linux");
        String dest = destinationToRealPath(destSource).toString();
        if (runDos2Unix(DOS2UNIX, dest, false) == 0) {
            log.info("File conversion to Unix Format is Success (1st try PATH present)! filepath: {}", dest);
        } else {
            log.warn("File conversion to Unix Format is Fail (1st try PATH is not present)!, filepath: {};", dest);
            String dosPath = getDos2UnixPath(pathToDos2Unix).toString();
            log.info("Try to run dos2unix in this path: {}", dosPath);
            if (runDos2Unix(dosPath, dest, false) == 0) {
                log.info("File conversion to Unix Format is Success (2nd try)! filepath: {}", dest);
            } else {
                log.error("Dos2Unix error (2nd try): dos2unix path provided by user not working: {}", pathToDos2Unix);
                throw new Dos2unixLinuxFailException(dest, dosPath);
            }
        }
    }

    private int runDos2Unix(String command, String dest, boolean isRetry) {
        int exitVal = -1;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(command, dest);
            Process process = builder.start();
            Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getInputStream(), log::trace));
            Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getErrorStream(), log::trace));
            exitVal = process.waitFor();
            log.info("dos2unix executed with code: {}", exitVal);
        } catch (IOException e) {
            log.warn("Dos2Unix error: IOException {}", e.getMessage());
            if (e.getMessage().contains(PERMISSION_ERROR) && !isRetry) {
                log.warn("Dos2Unix error: contains permission error, try to run chmod +x on dos2unix tool");
                runChmod(command);
                exitVal = runDos2Unix(command, dest, true);
            }
        } catch (InterruptedException e) {
            log.warn("Dos2Unix error: Can't wait dos2unix process to the end, it was interrupted!");
        }
        return exitVal;
    }

    /**
     * In case if dos2unix cannot be executed due permission error, chmod on it could resolve problem.
     *
     * @param path to dos2unix tool.
     */
    private void runChmod(String path) {
        log.trace("running chmod +x on path: {}", path);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("chmod", "+x", path);
            Process process = builder.start();
            Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getInputStream(), log::trace));
            Executors.newSingleThreadExecutor().submit(new StreamGobbler(process.getErrorStream(), log::trace));
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                log.trace("chmod +x is Success! Path: {}", path);
            } else {
                log.trace("chmod +x is Fail! Path: {}", path);
            }
        } catch (IOException e) {
            log.warn("chmod error: path: [{}], IOException {}", path, e.getMessage());
        } catch (InterruptedException e) {
            log.warn("chmod error: Can't wait chmod process to the end, it was interrupted! path: [{}],  {}",
                    path, e.getMessage());
        }
    }
}
