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

package org.qubership.atp.mia.model.impl.executable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.qubership.atp.mia.model.pot.Marker;

import clover.com.google.common.base.Strings;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
public class Command extends GeneralModel {

    private static final long serialVersionUID = -4849070705138269312L;
    @Nullable
    private String pathForUpload;
    @Nullable
    private String regexpForFileRetrieve;
    @Nullable
    private String fileExtension;
    @Nullable
    private List<String> namesOfFilesForGeneration;
    @Nullable
    private List<String> ethalonFilesForGeneration;
    @Nullable
    private String delayForGeneration;
    @Nullable
    private List<String> pathsForDownload;
    @Nullable
    private LinkedHashMap<String, String> variablesToExtractFromLog;
    @Nullable
    private Marker marker;
    @Nullable
    private String description;
    @Nullable
    private String warningDescription;
    @Nullable
    private String logFileNameFormat;
    @Nullable
    private String saveGeneratedFilesToParameter;
    @Nullable
    private String charsetForGeneratedFile;
    private boolean displayDownloadedFileContent;
    @Nullable
    private List<String> filesForUpload;
    @Nullable
    private LinkedHashMap<String, String> atpValues;
    @Nullable
    private List<FileMarker> fileMarkers;
    @Nullable
    private transient String toExecute;
    @Nullable
    private TestDataParams testDataParams;
    @Nullable
    private Rest rest;
    @Nullable
    private Soap soap;

    /**
     * Creates {@code Command} copy.
     *
     * @param command original command
     */
    public Command(@Nonnull Command command) {
        this(command.getName(), command.getType(), command.getSystem(), command.getValues());
        pathForUpload = command.getPathForUpload();
        regexpForFileRetrieve = command.getRegexpForFileRetrieve();
        namesOfFilesForGeneration = command.getNamesOfFilesForGeneration();
        ethalonFilesForGeneration = command.getEthalonFilesForGeneration();
        delayForGeneration = command.getDelayForGeneration();
        pathsForDownload = command.getPathsForDownload();
        variablesToExtractFromLog = command.getVariablesToExtractFromLog();
        marker = command.getMarker();
        description = command.getDescription();
        warningDescription = command.getWarningDescription();
        logFileNameFormat = command.getLogFileNameFormat();
        saveGeneratedFilesToParameter = command.getSaveGeneratedFilesToParameter();
        charsetForGeneratedFile = command.getCharsetForGeneratedFile();
        displayDownloadedFileContent = command.getDisplayDownloadedFileContent();
        filesForUpload = command.getFilesForUpload();
        fileMarkers = command.getFileMarkers();
        toExecute = command.getToExecute();
        testDataParams = command.getTestDataParams();
        rest = command.getRest();
        soap = command.getSoap();
    }

    /**
     * Creates {@code Command} instance without parameters.
     */
    public Command() {
        super();
    }

    /**
     * Creates {@code Command} instance for simple command.
     *
     * @param toExecute toExecute
     */
    public Command(@Nonnull String toExecute) {
        super();
        super.setValue(toExecute);
        this.toExecute = toExecute;
    }

    /**
     * Creates {@code Command} instance with parameters.
     *
     * @param name   name
     * @param type   type
     * @param system system
     * @param values values
     */
    public Command(@Nonnull String name, @Nonnull String type, @Nonnull String system,
                   @Nonnull LinkedHashSet<String> values) {
        super(name, type, system, values);
        if (values != null && !values.isEmpty()) {
            this.toExecute = values.stream().findFirst().get();
        }
    }

    /**
     * Clones Command object.
     *
     * @return Command instance.
     */
    public Command clone() {
        CommandBuilder builder = this.toBuilder();
        builder.values(cloneValues());
        if (namesOfFilesForGeneration != null) {
            builder.namesOfFilesForGeneration(new ArrayList<>(namesOfFilesForGeneration));
        }
        if (ethalonFilesForGeneration != null) {
            builder.ethalonFilesForGeneration(new ArrayList<>(ethalonFilesForGeneration));
        }
        if (pathsForDownload != null) {
            builder.pathsForDownload(new ArrayList<>(pathsForDownload));
        }
        if (variablesToExtractFromLog != null) {
            builder.variablesToExtractFromLog(new LinkedHashMap<>(variablesToExtractFromLog));
        }
        if (marker != null) {
            builder.marker(marker.clone());
        }
        if (filesForUpload != null) {
            builder.filesForUpload(new ArrayList<>(filesForUpload));
        }
        if (atpValues != null) {
            builder.atpValues(new LinkedHashMap<>(atpValues));
        }
        if (fileMarkers != null) {
            builder.fileMarkers(new ArrayList<>(fileMarkers));
        }
        if (testDataParams != null) {
            builder.testDataParams(testDataParams.toBuilder().build());
        }
        if (rest != null) {
            builder.rest(rest.toBuilder().build());
        }
        if (soap != null) {
            builder.soap(soap.toBuilder().build());
        }
        return builder.build();
    }

    /**
     * Sets value. Do not call! Used during parsing config.
     */
    @Override
    public void setValue(String value) {
        super.addValue(value);
        this.toExecute = value;
        this.value = value;
    }

    @Override
    public String getValue() {
        if (this.toExecute != null) {
            return this.toExecute;
        }
        return this.value != null ? this.value : super.getValue();
    }

    /**
     * Sets values. Do not call! Used during parsing config.
     */
    public void setValues(LinkedHashSet<String> values) {
        if (values != null) {
            values.stream().filter(Objects::nonNull).forEach(super::addValue);
            if (!values.isEmpty()) {
                this.toExecute = values.stream().findFirst().get();
            }
        }
    }

    /**
     * Gets ATP values.
     */
    public Map<String, String> getAtpValues() {
        if (atpValues == null) {
            setAtpValues(new LinkedHashMap<>());
        }
        return this.atpValues;
    }

    /**
     * Get charset for generated file.
     *
     * @return Charset
     */
    public Charset definedCharsetForGeneratedFile() {
        if (charsetForGeneratedFile == null) {
            return StandardCharsets.US_ASCII;
        } else {
            return Charset.forName(charsetForGeneratedFile);
        }
    }

    /**
     * Gets toExecute.
     *
     * @return toExecute
     */
    public String getToExecute() {
        if (this.toExecute != null) {
            return this.toExecute;
        }
        return super.getValue();
    }

    /**
     * Sets toExecute.
     *
     * @param toExecute toExecute
     * @return {@code Command} instance
     */
    public Command setToExecute(String toExecute) {
        this.toExecute = toExecute;
        return this;
    }

    @Nullable
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Sets fileExtension.
     *
     * @param fileExtension fileExtension
     * @return {@code Command} instance
     */
    public Command setFileExtension(@Nullable String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    @Nullable
    public String getPathForUpload() {
        return pathForUpload;
    }

    /**
     * Sets pathForUpload.
     *
     * @param pathForUpload pathForUpload
     * @return {@code Command} instance
     */
    public Command setPathForUpload(@Nullable String pathForUpload) {
        this.pathForUpload = pathForUpload;
        return this;
    }

    @Nullable
    public List<String> getNamesOfFilesForGeneration() {
        return namesOfFilesForGeneration;
    }

    /**
     * Sets namesOfFilesForGeneration.
     *
     * @param namesOfFilesForGeneration namesOfFilesForGeneration
     * @return {@code Command} instance
     */
    public Command setNamesOfFilesForGeneration(@Nullable List<String> namesOfFilesForGeneration) {
        this.namesOfFilesForGeneration = namesOfFilesForGeneration;
        return this;
    }

    @Nullable
    public List<String> getEthalonFilesForGeneration() {
        return ethalonFilesForGeneration;
    }

    /**
     * Sets ethalonFilesForGeneration.
     *
     * @param ethalonFilesForGeneration ethalonFilesForGeneration
     * @return {@code Command} instance
     */
    public Command setEthalonFilesForGeneration(@Nullable List<String> ethalonFilesForGeneration) {
        this.ethalonFilesForGeneration = ethalonFilesForGeneration;
        return this;
    }

    @Nullable
    public String getRegexpForFileRetrieve() {
        return regexpForFileRetrieve;
    }

    /**
     * Sets regexpForFileRetrieve.
     *
     * @param regexpForFileRetrieve regexpForFileRetrieve
     * @return {@code Command} instance
     */
    public Command setRegexpForFileRetrieve(@Nullable String regexpForFileRetrieve) {
        this.regexpForFileRetrieve = regexpForFileRetrieve;
        return this;
    }

    @Nullable
    public String getDelayForGeneration() {
        return delayForGeneration;
    }

    /**
     * Sets delayForGeneration.
     *
     * @param delayForGeneration delayForGeneration
     * @return {@code Command} instance
     */
    public Command setDelayForGeneration(@Nullable String delayForGeneration) {
        this.delayForGeneration = delayForGeneration;
        return this;
    }

    @Nullable
    public List<String> getPathsForDownload() {
        return pathsForDownload;
    }

    /**
     * Sets pathsForDownload.
     *
     * @param pathsForDownload pathsForDownload
     * @return {@code Command} instance
     */
    public Command setPathsForDownload(@Nullable List<String> pathsForDownload) {
        this.pathsForDownload = pathsForDownload;
        return this;
    }

    @Nullable
    public Marker getMarker() {
        return marker;
    }

    /**
     * Sets marker.
     *
     * @param marker marker
     * @return {@code Command} instance
     */
    public Command setMarker(@Nullable Marker marker) {
        this.marker = marker;
        return this;
    }

    /**
     * Gets saveGeneratedFilesToParameter.
     *
     * @return saveGeneratedFilesToParameter value
     */
    @Nullable
    public String getSaveGeneratedFilesToParameter() {
        return this.saveGeneratedFilesToParameter;
    }

    /**
     * Sets saveGeneratedFilesToParameter.
     *
     * @param saveGeneratedFilesToParameter saveGeneratedFilesToParameter
     * @return {@code Command} instance
     */
    public Command setSaveGeneratedFilesToParameter(@Nullable String saveGeneratedFilesToParameter) {
        this.saveGeneratedFilesToParameter = saveGeneratedFilesToParameter;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     *
     * @param description description
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getWarningDescription() {
        return this.warningDescription;
    }

    /**
     * Sets warningDescription.
     *
     * @param warningDescription warningDescription
     */
    public void setWarningDescription(@Nullable String warningDescription) {
        this.warningDescription = warningDescription;
    }

    @Nullable
    public String getLogFileNameFormat() {
        return logFileNameFormat;
    }

    /**
     * Sets fileExtension.
     *
     * @param logFileNameFormat logFileNameFormat
     * @return {@code Command} instance
     */
    public Command setLogFileNameFormat(@Nullable String logFileNameFormat) {
        this.logFileNameFormat = Strings.emptyToNull(logFileNameFormat);
        return this;
    }

    @Nullable
    public LinkedHashMap<String, String> getVariablesToExtractFromLog() {
        return variablesToExtractFromLog;
    }

    /**
     * Sets variablesToExtractFromLog.
     *
     * @param variablesToExtractFromLog variablesToExtractFromLog
     * @return {@code Command} instance
     */
    public Command setVariablesToExtractFromLog(@Nullable LinkedHashMap<String, String> variablesToExtractFromLog) {
        this.variablesToExtractFromLog = variablesToExtractFromLog;
        return this;
    }

    @Nullable
    public String getCharsetForGeneratedFile() {
        return charsetForGeneratedFile;
    }

    /**
     * Sets charsetForGeneratedFile.
     *
     * @param charsetForGeneratedFile charsetForGeneratedFile
     * @return {@code Command} instance
     */
    public Command setCharsetForGeneratedFile(@Nullable String charsetForGeneratedFile) {
        this.charsetForGeneratedFile = charsetForGeneratedFile;
        return this;
    }

    /**
     * Sets ATP values. Do not call! Used during parsing config.
     */
    public Command setAtpValues(LinkedHashMap<String, String> atpValues) {
        if (atpValues != null) {
            this.atpValues = atpValues;
            this.atpValues.values().forEach(super::addValue);
            if (!atpValues.isEmpty()) {
                this.toExecute = atpValues.values().stream().findFirst().get();
            }
        }
        return this;
    }

    public boolean getDisplayDownloadedFileContent() {
        return displayDownloadedFileContent;
    }

    public Command setDisplayDownloadedFileContent(boolean displayDownloadedFileContent) {
        this.displayDownloadedFileContent = displayDownloadedFileContent;
        return this;
    }

    @Nullable
    public List<String> getFilesForUpload() {
        return filesForUpload;
    }

    public Command setFilesForUpload(@Nullable List<String> filesForUpload) {
        this.filesForUpload = filesForUpload;
        return this;
    }

    @Nullable
    public List<FileMarker> getFileMarkers() {
        return fileMarkers;
    }

    public void setFileMarkers(@Nullable List<FileMarker> fileMarkers) {
        this.fileMarkers = fileMarkers;
    }

    /**
     * Gets testDataParams value.
     *
     * @return testDataParams
     */
    @Nonnull
    public TestDataParams getTestDataParams() {
        return testDataParams;
    }

    /**
     * Sets testDataParams value.
     *
     * @param testDataParams testDataParams value
     */
    public void setTestDataParams(@Nonnull TestDataParams testDataParams) {
        this.testDataParams = testDataParams;
    }

    /**
     * Gets rest value.
     *
     * @return rest
     */
    @Nullable
    public Rest getRest() {
        return rest;
    }

    /**
     * Sets rest value.
     *
     * @param rest rest value
     */
    public void setRest(@Nullable Rest rest) {
        this.rest = rest;
    }

    /**
     * Gets soap value.
     *
     * @return soap
     */
    @Nullable
    public Soap getSoap() {
        return soap;
    }

    /**
     * Sets soap value.
     *
     * @param soap soap value
     */
    public void setSoap(@Nullable Soap soap) {
        this.soap = soap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Command command = (Command) o;
        return Objects.equals(getType(), command.getType())
                && Objects.equals(getSystem(), command.getSystem())
                && Objects.equals(getValues(), command.getValues())
                && Objects.equals(atpValues, command.atpValues)
                && Objects.equals(toExecute, command.toExecute)
                && Objects.equals(pathForUpload, command.pathForUpload)
                && Objects.equals(regexpForFileRetrieve, command.regexpForFileRetrieve)
                && Objects.equals(fileExtension, command.fileExtension)
                && Objects.equals(namesOfFilesForGeneration, command.namesOfFilesForGeneration)
                && Objects.equals(ethalonFilesForGeneration, command.ethalonFilesForGeneration)
                && Objects.equals(delayForGeneration, command.delayForGeneration)
                && Objects.equals(variablesToExtractFromLog, command.variablesToExtractFromLog)
                && Objects.equals(marker, command.marker)
                && Objects.equals(pathsForDownload, command.pathsForDownload)
                && Objects.equals(description, command.description)
                && Objects.equals(logFileNameFormat, command.logFileNameFormat)
                && Objects.equals(saveGeneratedFilesToParameter, command.saveGeneratedFilesToParameter)
                && Objects.equals(charsetForGeneratedFile, command.charsetForGeneratedFile)
                && Objects.equals(displayDownloadedFileContent, command.displayDownloadedFileContent)
                && Objects.equals(filesForUpload, command.filesForUpload)
                && Objects.equals(testDataParams, command.testDataParams)
                && Objects.equals(fileMarkers, command.fileMarkers)
                && Objects.equals(rest, command.rest)
                && Objects.equals(soap, command.soap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getSystem(), getValues(), atpValues, toExecute, pathForUpload,
                regexpForFileRetrieve, fileExtension, namesOfFilesForGeneration, ethalonFilesForGeneration,
                delayForGeneration, variablesToExtractFromLog, marker, pathsForDownload, description, logFileNameFormat,
                saveGeneratedFilesToParameter, charsetForGeneratedFile, displayDownloadedFileContent, filesForUpload,
                fileMarkers, testDataParams, rest, soap);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Command.class.getSimpleName() + "[", "]")
                .add("name='" + getName() + "'")
                .add("type='" + getType() + "'")
                .add("system='" + getSystem() + "'")
                .add("value='" + getValue() + "'")
                .add("values='" + getValues() + "'")
                .add("atpValues=" + atpValues + "'")
                .add("toExecute=" + toExecute + "'")
                .add("pathForUpload='" + pathForUpload + "'")
                .add("regexpForFileRetrieve='" + regexpForFileRetrieve + "'")
                .add("fileExtension='" + fileExtension + "'")
                .add("namesOfFilesForGeneration=" + namesOfFilesForGeneration + "'")
                .add("ethalonFilesForGeneration=" + ethalonFilesForGeneration + "'")
                .add("delayForGeneration='" + delayForGeneration + "'")
                .add("pathsForDownload=" + pathsForDownload + "'")
                .add("variablesToExtractFromLog=" + variablesToExtractFromLog + "'")
                .add("marker=" + marker + "'")
                .add("description=" + description + "'")
                .add("logFileNameFormat=" + logFileNameFormat + "'")
                .add("saveGeneratedFilesToParameter='" + saveGeneratedFilesToParameter + "'")
                .add("charsetForGeneratedFile='" + charsetForGeneratedFile + "'")
                .add("displayDownloadedFileContent='" + displayDownloadedFileContent + "'")
                .add("fileForUpload='" + filesForUpload + "'")
                .add("fileMarkers='" + fileMarkers + "'")
                .add("rest='" + rest + "'")
                .add("testDataParams='" + testDataParams + "'")
                .add("soap='" + soap + "'")
                .toString();
    }
}
