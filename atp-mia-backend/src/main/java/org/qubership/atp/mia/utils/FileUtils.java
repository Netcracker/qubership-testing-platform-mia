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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.xml.ws.Holder;

import org.apache.commons.io.filefilter.IOFileFilter;
import org.qubership.atp.mia.exceptions.fileservice.ArchiveIoExceptionDuringSave;
import org.qubership.atp.mia.exceptions.fileservice.CreateDirFailedException;
import org.qubership.atp.mia.exceptions.fileservice.ReadFailIoExceptionDuringOperation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {
    /**
     * Log information into file.
     *
     * @param logString Log information
     * @param file file
     */
    public static void logIntoFile(String logString, File file) {
        try {
            file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.append(logString);
                writer.flush();
            } catch (IOException e) {
                log.error("Can't write response to file - IOException", e);
            }
        } catch (IOException e) {
            log.error("Can't create file - IOException", e);
        }
    }

    /**
     * Saves selected {@code fileToZip} to zip output stream (zip archive).
     * This operation done recursively.
     *
     * @param zos                 zip output stream, which should be open.
     * @param fileToZip           this file (or directory) will be stored in archive.
     * @param parentDirectoryName used in next cycles of recursion, could be {@code null}.
     * @param filter              used to filter file/directories
     */
    public static void addDirToZipArchive(ZipOutputStream zos, File fileToZip,
                                          String parentDirectoryName, IOFileFilter filter) {
        if (fileToZip == null || !fileToZip.exists()) {
            return;
        }
        if (filter != null && !filter.accept(fileToZip)) {
            log.trace("skipping file during archive: {}", fileToZip.getName());
            return;
        }
        String zipEntryName = fileToZip.getName();
        if (parentDirectoryName != null && !parentDirectoryName.isEmpty()) {
            zipEntryName = parentDirectoryName + "/" + zipEntryName;
        }
        if (fileToZip.isDirectory()) {
            log.trace("|" + zipEntryName);
            File[] files = fileToZip.listFiles();
            if (files != null) {
                for (File file : files) {
                    addDirToZipArchive(zos, file, zipEntryName, filter);
                }
            }
        } else {
            log.trace(" +" + zipEntryName);
            byte[] buffer = new byte[1024];
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                zos.putNextEntry(new ZipEntry(zipEntryName));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            } catch (FileNotFoundException e) {
                log.error("A file in archiving folder does not exist: %s", e);
            } catch (IOException e) {
                throw new ArchiveIoExceptionDuringSave(fileToZip.toString(), e);
            }
        }
    }

    /**
     * Unzip archive into the destination directory.
     */
    public static File unzipConfig(final File zipPath, final File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            if (!destDir.exists()) {
                createDirectories(destDir.toPath());
            }
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            return destDir;
        } catch (FileNotFoundException e) {
            throw new IOException("Archive file not found! file: " + zipPath.getPath());
        }
    }

    /**
     * Create file with protection from ZipSlip vulnerability,
     * so it is not possible to create outside the current dir.
     *
     * @param destinationDir dir to create.
     * @param zipEntry       opened zip archive.
     * @return {@link File} of created dir.
     * @throws IOException if the created entry is outside of the target dir.
     */
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    /**
     * Copy files from current directory to destination folder.
     *
     * @param src  from
     * @param dest to
     * @throws IOException IOException during coping files
     */
    public static void copyFilesFromCurrentDir(File src, File dest) throws IOException {
        org.apache.commons.io.FileUtils.copyDirectory(src, dest);
        org.apache.commons.io.FileUtils.copyDirectory(src, dest);
    }

    /**
     * Deletes a folder content and the folder itself
     * if deleteNowDir parameter is {@code true}.
     *
     * @param dir          dir which should be cleaned.
     * @param deleteNowDir if true then delete root dir, otherwise keeps it (with cleaned content).
     */
    public static void deleteFolder(File dir, boolean deleteNowDir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteFolder(f, true);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        if (deleteNowDir) {
            dir.delete();
        }
    }

    /**
     * Takes every directory from dirs parameter and clean
     * this directory with {@code deleteFolder} method.
     *
     * @param deleteNowDir if true then delete root dir, otherwise keeps it (with cleaned content).
     *                     Affects every directory from dirs parameter.
     * @param dirs         array with directories to clean.
     */
    public static void deleteFolders(boolean deleteNowDir, File[] dirs) {
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir.exists() && dir.isDirectory()) {
                    deleteFolder(dir, deleteNowDir);
                }
            }
        }
    }

    /**
     * Finds directory which name equals to {@code searchDirName}.
     * Search occurs in {@code inputDir} directory and its subdirectories.
     * Very handy to use it in case to find flow directory in config.
     *
     * @param inputDir      directory where search occur.
     * @param searchDirName name of searched directory.
     * @return {@code File} which linked to directory or exception thrown.
     * @throws FileNotFoundException in case when file not found the exception thrown.
     */
    public static File findFileAndGetParent(File inputDir, String searchDirName)
            throws FileNotFoundException {
        File foundFile = null;
        List<File> directories = new ArrayList<>();
        if (inputDir.isDirectory()) {
            File[] files = inputDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().equals(searchDirName)) {
                        foundFile = f;
                        break;
                    }
                    if (f.isDirectory()) {
                        directories.add(f);
                    }
                }
                if (foundFile == null && !directories.isEmpty()) {
                    for (File f : directories) {
                        foundFile = findFileAndGetParent(f, searchDirName);
                        if (foundFile != null) {
                            break;
                        }
                    }
                }
            }
        }
        if (foundFile == null) {
            throw new FileNotFoundException("File not found:" + searchDirName);
        }
        return foundFile;
    }

    /**
     * Reads file by name from directory of flow config.
     */
    public static String readFile(Path pathToFile) {
        try {
            //return new String(Files.readAllBytes(pathToFile));
            return new String(Files.readAllBytes(pathToFile.toRealPath().normalize()));
        } catch (IOException e) {
            throw new ReadFailIoExceptionDuringOperation(pathToFile.toString(), e);
        }
    }

    /**
     * Copy folder.
     *
     * @param src  from
     * @param dest to
     * @throws IOException IOException during coping files
     */
    public static void copyFolder(Path src, Path dest) throws IOException {
        Holder<IOException> exception = new Holder<>(null);
        Files.walk(src).forEach(s -> {
            try {
                Path d = dest.resolve(src.relativize(s));
                if (Files.isDirectory(s)) {
                    if (!Files.exists(d)) {
                        Files.createDirectory(d);
                    }
                    return;
                }
                Files.copy(s, d, REPLACE_EXISTING);
            } catch (IOException e) {
                exception.value = e;
            }
        });
        if (exception.value != null) {
            throw exception.value;
        }
    }

    /**
     * Copy file.
     *
     * @param src  from
     * @param dest to
     * @throws IOException IOException during coping files
     */
    public static void copyFile(Path src, Path dest) throws IOException {
        Files.copy(src, dest, REPLACE_EXISTING);
    }

    /**
     * Remove file.
     *
     * @param fileToDelete fileToDelete
     */
    public static void removeFile(String fileToDelete) {
        try {
            Files.deleteIfExists(Paths.get(fileToDelete));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Creates directories.
     *
     * @param pathToCreate pathToCreate
     */
    public static void createDirectories(Path pathToCreate) {
        try {
            Files.createDirectories(pathToCreate);
        } catch (IOException e) {
            throw new CreateDirFailedException(pathToCreate, e);
        }
    }

    /**
     * Correct path to file.
     *
     * @param dir      dir
     * @param fileName file name
     * @return correct path to file
     */
    @Nonnull
    public static String tempFileName(final String dir, String fileName) {
        String path = "/";
        if (dir != null) {
            final String startSlash = dir.startsWith("/") || dir.isEmpty() ? "" : "/";
            final String endSlash = dir.endsWith("/") ? "" : "/";
            path = startSlash + dir + endSlash;
        }
        return path + fileName;
    }

    /**
     * Parses the file path and removes till flow folder.
     * If file directly inside flow, it gives file name. Ex: Template_SSH.json
     * Else if file is inside subfolder of flow folder, then it gives the path from flow directory. Ex: ./SSH/SSH.json
     *
     * @param file actualFile
     * @return correct path to file,
     */
    public static Path getPathToFileFromFile(File file) {
        Path path = Paths.get(file.getName());
        Path parent = Paths.get(file.getParent());
        while (!parent.getFileName().toString().equals("flow")) {
            path = Paths.get(parent.getFileName().toString()).resolve(path);
            parent = parent.getParent();
        }
        return path.normalize();
    }

    /**
     * Create folder.
     */
    public static void createFolder(File targetFile) {
        if (!targetFile.exists()) {
            try {
                Files.createDirectories(targetFile.toPath().getParent());
            } catch (IOException e) {
                throw new CreateDirFailedException(targetFile.toPath().getParent(), e);
            }
        }
    }
}
