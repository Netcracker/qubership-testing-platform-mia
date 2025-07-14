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

package org.qubership.atp.mia.model.impl.generation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.qubership.atp.mia.exceptions.testdata.MatrixEthalonReadFailException;
import org.qubership.atp.mia.exceptions.testdata.MatrixEthalonWriteFailException;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.impl.VariableFormat;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.file.MiaFileService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Template {

    private final MiaContext miaContext;
    private final MiaFileService miaFileService;
    private final Path pathToEthalonFile;
    private final String fileExtension;
    private final Charset charset;
    private final String fileName;
    private final String generationFileName;

    private String content;
    private Path generationFile;

    /**
     * File generated on ethalon file basis.
     *
     * @param pathToEthalonFile  name of ethalon file
     * @param generationFileName name of generated file
     * @param fileExtension      extension of generated file
     */
    public Template(MiaContext miaContext, MiaFileService miaFileService, String pathToEthalonFile,
                    String generationFileName, String fileExtension, Charset charset) {
        this.miaContext = miaContext;
        this.miaFileService = miaFileService;
        //this.pathToEthalonFile = miaContext.getProjectFilePath().resolve(pathToEthalonFile).normalize();
        this.pathToEthalonFile = miaContext.getProjectFilePath().resolve(
                sanitizePathTraversal(pathToEthalonFile)).normalize();
        //this.fileName = generationFileName;
        //this.generationFileName = this.fileName;
        //this.generationFileName += fileExtension != null ? fileExtension : "";
        String sanitizedName = sanitizePathTraversal(generationFileName);
        this.fileName = sanitizedName;
        this.generationFileName = sanitizedName + (fileExtension != null ? fileExtension : "");
        this.fileExtension = fileExtension;
        this.charset = (charset == null) ? StandardCharsets.UTF_8 : charset;
    }

    private static String sanitizePathTraversal(String fileName) {
        Path p = Paths.get(fileName);
        return p.getFileName().toString();
    }

    /**
     * Generates file from ethalon.
     */
    public void evaluateFile() {
        evaluateContent(miaContext.getFlowData().getParameters());
        writeContent();
    }

    /**
     * Reads content of the file.
     *
     * @return content as string
     */
    private String readContent() {
        try {
            miaFileService.getFile(pathToEthalonFile.toFile());
            content = new String(Files.readAllBytes(pathToEthalonFile), this.charset);
            content = miaContext.evaluate(content);
            return content;
        } catch (IOException e) {
            throw new MatrixEthalonReadFailException(pathToEthalonFile.toString(), e);
        }
    }

    /**
     * Get content of the file.
     *
     * @return content of the file. If it null reads it first from Ethalon File Path.
     */
    public String getContent() {
        return content == null ? readContent() : content;
    }

    /**
     * Evaluate content of the file.
     *
     * @param additionalParams additionalParams to evaluate
     * @return evaluated content
     */
    public String evaluateContent(Map<String, String> additionalParams) {
        getContent();
        additionalParams.put("CURRENT_TEMPLATE_FILE_NAME", fileName);
        additionalParams.put("CURRENT_TEMPLATE_FILE_EXTENSION", fileExtension);
        additionalParams.put("CURRENT_TEMPLATE_FILE_FULL_NAME", generationFileName);
        content = miaContext.evaluate(content, additionalParams);
        return content;
    }

    /**
     * Default single replace content operation.
     * Uses replaceFirst function.
     *
     * @param from string to replace.
     * @param to   string to substitute.
     */
    public void replaceContent(String from, String to) {
        if (from != null && !from.isEmpty() && to != null) {
            if (content == null) {
                readContent();
            }
            content = content.replaceFirst(from, to);
        }
    }

    /**
     * Replaces EventParameter with EventTemplate in the content of template.
     * Main purpose is to use in TestData operation for new line generation in control file.
     *
     * @param command    which contains EventParameter and EventTemplate.
     * @param commonConf to get Variable format.
     * @param params     for EventTemplate evaluation.
     */
    public void evaluateTemplate(Command command, CommonConfiguration commonConf, Map<String, String> params) {
        final String paramInTemplate = command.getTestDataParams().getEventParameterInTemplate();
        final String eventTemplate = miaContext.evaluate(command.getTestDataParams().getEventTemplate(), params);
        final VariableFormat varFormat = new VariableFormat(commonConf.getVariableFormat());
        final String neededParamInTemplate = varFormat.getVariableAccordingFormat(paramInTemplate);
        replaceContent(neededParamInTemplate, eventTemplate + "\n" + neededParamInTemplate);
    }

    /**
     * Evaluates and generates file.
     *
     * @return generated file generationFile
     */
    public File getFile() {
        if (generationFile == null) {
            evaluateFile();
        }
        return generationFile.toFile();
    }

    /**
     * Write content into file.
     */
    public void writeContent() {
        try {
            generationFile = miaContext.getLogPath().resolve(generationFileName);
            log.trace("Write content into {} file:\n{}", generationFile, content);
            Files.write(generationFile, content.getBytes(charset));
        } catch (IOException e) {
            throw new MatrixEthalonWriteFailException(generationFile, e);
        }
    }

    /**
     * Gets file name.
     *
     * @return generationFileName
     */
    public String getFileName() {
        return generationFileName;
    }
}

