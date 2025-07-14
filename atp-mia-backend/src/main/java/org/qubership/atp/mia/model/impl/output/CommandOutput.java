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

package org.qubership.atp.mia.model.impl.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.qubership.atp.mia.exceptions.fileservice.ReadFailFileNotFoundException;
import org.qubership.atp.mia.exceptions.fileservice.ReadFailIoExceptionDuringOperation;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.pot.Link;
import org.qubership.atp.mia.model.pot.Statuses;
import org.qubership.atp.mia.service.MiaContext;
import org.springframework.lang.NonNull;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class CommandOutput {

    @Nonnull
    private final String internalPathToFile;
    private final String externalPathToFile;
    private Link link;
    private final boolean displayed;
    //Keep content for backward compatible
    private LinkedList<String> content = new LinkedList<>();
    //new content contains line and its state
    private LinkedList<MarkedContent> markedContent = new LinkedList<>();
    private boolean isSizeExceedLimit = false;

    // For deserialization purpose only
    private CommandOutput() {
        internalPathToFile = "";
        externalPathToFile = "";
        link = null;
        displayed = true;
    }

    /**
     * Command output output is generated from file
     * with check of need to display on UI.
     *
     * @param internalPathToFile path to output file on local machine
     * @param externalPathToFile path to output file on server
     * @param isDisplayed        need to display on UI
     */
    public CommandOutput(String internalPathToFile, String externalPathToFile, boolean isDisplayed,
                         MiaContext miaContext) {
        this(internalPathToFile, externalPathToFile, null, isDisplayed, miaContext);
    }

    /**
     * Constructor when file not found.
     *
     * @param internalPathToFile path to output file on local machine
     * @param externalPathToFile path to output file on server
     * @param errorMessage       errorMessage
     * @param isDisplayed        need to display on UI
     */
    public CommandOutput(String internalPathToFile, String externalPathToFile, List<String> errorMessage,
                         boolean isDisplayed, MiaContext miaContext) {
        this.internalPathToFile = internalPathToFile;
        this.externalPathToFile = externalPathToFile;
        this.link = miaContext.getLogLinkOnUi(internalPathToFile);
        if (errorMessage != null) {
            errorMessage.forEach(e -> addContent(e, Statuses.FAIL));
        }
        this.displayed = isDisplayed;
    }

    /**
     * Gets file by internal path.
     */
    public LinkedList<String> contentFromFile() {
        String internalPathToFile = this.internalPathToFile;
        if (internalPathToFile.contains("..") || internalPathToFile.contains("//")
                || internalPathToFile.startsWith("/") || internalPathToFile.startsWith("\\")) {
            throw new SecurityException("Invalid path: Path traversal attempt detected -> " + internalPathToFile);
        }
        File file = new File(internalPathToFile).getAbsoluteFile();
        LinkedList<String> content = this.content;
        if (content == null || content.isEmpty()) {
            log.info("Read full content of file [{}]...", internalPathToFile);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                content = new LinkedList<>();
                String line;
                while ((line = br.readLine()) != null) {
                    content.add(line);
                }
                log.info("Read full content of file [{}] DONE", internalPathToFile);
            } catch (FileNotFoundException e) {
                throw new ReadFailFileNotFoundException(internalPathToFile);
            } catch (IOException e) {
                throw new ReadFailIoExceptionDuringOperation(internalPathToFile, e);
            }
        }
        return content;
    }

    /**
     * Save latest line to content.
     *
     * @param amountOfLines  - not more than 10.
     * @param limitSizeBytes - limit size in bytes.
     */
    public void saveLatestLineToContent(int amountOfLines, long limitSizeBytes) {
        int linesCount = Math.min(10, amountOfLines);
        log.info("Saving last line of the content. Getting last line of file [{}]...", this.internalPathToFile);
        File file = new File(this.internalPathToFile);
        if (file.exists()) {
            try (ReversedLinesFileReader lastLineReader = new ReversedLinesFileReader(file)) {
                if (markedContent != null && !markedContent.isEmpty()) {
                    log.info("Rewriting Marked Content is not empty = [{}]", markedContent);
                }
                markedContent = new LinkedList<>();
                while (linesCount-- > 0) {
                    String lastLine = lastLineReader.readLine();
                    if (lastLine == null) {
                        log.warn("File is empty [{}], set last line as empty string", this.internalPathToFile);
                        lastLine = "";
                        addContent(lastLine, Statuses.UNKNOWN);
                        break;
                    }
                    addContent(lastLine, Statuses.UNKNOWN);
                }
                log.info("Get last line of file [{}] DONE", this.internalPathToFile);
            } catch (IOException e) {
                throw new ReadFailIoExceptionDuringOperation(file.toString(), e);
            }
            isSizeExceedLimit = file.length() > limitSizeBytes;
        } else {
            addContent(ErrorCodes.MIA_2053_READ_FAIL_FILE_NOT_FOUND.getMessage(file.toString()), Statuses.FAIL);
        }
    }

    /**
     * Add to content.
     */
    public void addContent(@NonNull String contentLine, @NonNull Statuses state) {
        markedContent.add(new MarkedContent(contentLine, state));
    }

    /**
     * Add to content.
     */
    public void addContent(@NonNull String contentLine) {
        markedContent.add(new MarkedContent(contentLine, Statuses.UNKNOWN));
    }

    /**
     * Adds to the existing marked content a new one.
     * It helps to keep in CommandOutput as a single response.
     */
    public void concatContent(CommandOutput commandOutput) {
        if (commandOutput != null && commandOutput.getMarkedContent() != null) {
            if (markedContent == null) {
                markedContent = new LinkedList<>();
            }
            markedContent.addAll(commandOutput.getMarkedContent());
        }
    }

    public boolean containsMarkedContentWithState(Statuses state) {
        return markedContent.stream().anyMatch(mc -> mc.getState().equals(state));
    }

    public LinkedList<String> getContent() {
        return new LinkedList<>();
    }
}
