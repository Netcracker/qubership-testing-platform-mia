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

package org.qubership.atp.mia.utils.dos2unix.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.qubership.atp.mia.exceptions.dos2unix.Dos2unixIncorrectFilePathException;
import org.qubership.atp.mia.exceptions.dos2unix.Dos2unixIncorrectToolPathException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Dos2UnixUtils {

    /**
     * Transforms dos2unix path to {@code RealPath}, it allows to check whether it exists.
     *
     * @param pathToDos2UnixS start of path.
     * @param pathToDos2Unix  the rest of path.
     * @return {@code RealPath} to dos2unix tool.
     */
    public static Path getDos2UnixPath(String pathToDos2UnixS, String... pathToDos2Unix) {
        try {
            return Paths.get(pathToDos2UnixS, pathToDos2Unix).toRealPath();
        } catch (IOException e) {
            throw new Dos2unixIncorrectToolPathException(Paths.get(pathToDos2UnixS, pathToDos2Unix).toAbsolutePath(),
                    e);
        }
    }

    /**
     * Transform existing path to file,
     * which need will be transformed by dos2unix.
     * Need to check whether given path exists.
     *
     * @param dest path to file.
     * @return {@code RealPath} of given file.
     */
    public static Path destinationToRealPath(Path dest) {
        try {
            dest = dest.toRealPath();
            log.info("Real path to file which will be converted by dos2unix: {}", dest.toString());
            return dest;
        } catch (IOException e) {
            throw new Dos2unixIncorrectFilePathException(dest.toString(), e);
        }
    }
}
