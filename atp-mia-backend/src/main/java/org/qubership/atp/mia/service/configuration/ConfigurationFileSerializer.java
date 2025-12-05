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

package org.qubership.atp.mia.service.configuration;

import static org.qubership.atp.mia.model.file.FileMetaData.PROJECT_FOLDER;
import static org.qubership.atp.mia.utils.FileUtils.deleteFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.util.Strings;
import org.modelmapper.ModelMapper;
import org.qubership.atp.mia.controllers.api.dto.ExecutableDto;
import org.qubership.atp.mia.controllers.api.dto.FlowConfigDto;
import org.qubership.atp.mia.controllers.api.dto.FlowConfigSectionDto;
import org.qubership.atp.mia.exceptions.configuration.SerializeFlowJsonFailedException;
import org.qubership.atp.mia.exceptions.configuration.SerializeProcessFileException;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.exception.MiaException;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.file.ProjectFileType;
import org.qubership.atp.mia.service.AtpUserService;
import org.qubership.atp.mia.service.file.MiaFileService;
import org.qubership.atp.mia.service.git.GitService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationFileSerializer {

    public static final String FILE_FLOW_JSON = "Flow.json";
    private static final ObjectMapper objMapper = JsonMapper.builder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
            .build();
    private final GitService gitService;
    private final MiaFileService miaFileService;
    private final ModelMapper modelMapper;
    private final AtpUserService atpUserService;

    /**
     * Make old version of config for backward compatible.
     *
     * @param projectConfiguration project configuration
     * @return {@link FlowConfigDto}
     */
    public FlowConfigDto getOldConfig(ProjectConfiguration projectConfiguration, boolean isForFile) {
        List<SectionConfiguration> sections = projectConfiguration.getRootSections();

        List<FlowConfigSectionDto> flowConfigSections = sections.stream()
                .filter(Objects::nonNull)
                .map(s -> makeOldSection(s, isForFile))
                .collect(Collectors.toList());

        FlowConfigDto flowConfigDto = modelMapper.map(projectConfiguration, FlowConfigDto.class);
        flowConfigDto.setDefaultSystem(projectConfiguration.getCommonConfiguration().getDefaultSystem());
        flowConfigDto.setSections(flowConfigSections);
        return flowConfigDto;
    }

    /**
     * Serialize Project Configuration to Git files.
     *
     * @param config ProjectConfiguration.
     * @throws MiaException Exception.
     */
    public void serialize(ProjectConfiguration config, Path projectConfigurationPath, boolean isEtalonFiles)
            throws MiaException {
        try {
            log.info("Serialize configuration for project with ID #{} into folder {}",
                    config.getProjectId(), projectConfigurationPath);
            gitService.downloadGitRepo(config.getGitUrl(), projectConfigurationPath);
            if (isEtalonFiles) {
                serializeEtalonFiles(config, projectConfigurationPath);
            } else {
                serializeGeneralConfiguration(config, projectConfigurationPath);
            }
            gitService.gitCommitAndPush(projectConfigurationPath,
                    "Automatic update FROM MIA tool triggered by " + atpUserService.getAtpUser());
        } catch (Exception e) {
            log.error("Error during serialization: {}", e.getMessage(), e);
            throw new SerializeFlowJsonFailedException(e.getMessage());
        } finally {
            try {
                deleteFolder(projectConfigurationPath.toFile(), true);
            } catch (Exception e) {
                log.info("Problem Deleting local directory, {}", e.getMessage());
            }
        }
    }

    /**
     * Serialize configuration to path.
     *
     * @param config ProjectConfiguration
     * @param projectConfigurationPath path
     * @throws MiaException if any problem.
     */
    public void serializeToPath(ProjectConfiguration config, Path projectConfigurationPath)
            throws MiaException {
        try {
            serializeGeneralConfiguration(config, projectConfigurationPath);
            serializeEtalonFiles(config, projectConfigurationPath);
        } catch (Exception e) {
            log.error("Error during serialization to path: {}", e.getMessage(), e);
            throw new SerializeFlowJsonFailedException(e.getMessage());
        }
    }

    private ExecutableDto makeOldProcess(@NonNull ProcessConfiguration process, boolean isForFile) {
        ExecutableDto executableDto = isForFile
                ? modelMapper.map(process, ExecutableDto.class).id(process.getId())
                : modelMapper.map(process.getProcessSettings(), ExecutableDto.class);
        return executableDto.execType("Process").name(process.getName());
    }

    private FlowConfigSectionDto makeOldSection(SectionConfiguration section, boolean isForFile) {
        FlowConfigSectionDto flowConfigSection = new FlowConfigSectionDto().name(section.getName()).id(section.getId());
        if (!CollectionUtils.isEmpty(section.getSections())) {
            List<FlowConfigSectionDto> flowConfigSections = section.getSections().stream()
                    .filter(Objects::nonNull)
                    .map(s -> makeOldSection(s, isForFile))
                    .collect(Collectors.toList());
            flowConfigSection.sections(flowConfigSections);
        }

        List<ExecutableDto> executableDtoList = new ArrayList<>();
        // Use getFullCompounds to get full data including referToInput and processes
        List<CompoundConfiguration> fullCompounds = section.getFullCompounds();
        if (!CollectionUtils.isEmpty(fullCompounds)) {
            for (CompoundConfiguration c : fullCompounds) {
                ExecutableDto executableDto = new ExecutableDto()
                        .id(c.getId())
                        .name(c.getName())
                        .execType("Compound")
                        .referToInput(c.getReferToInput())
                        .processList(new ArrayList<>());
                if (c.getProcesses() != null && !c.getProcesses().isEmpty()) {
                    c.getProcesses().stream()
                            .filter(Objects::nonNull)
                            .forEach(p -> executableDto.getProcessList().add(makeOldProcess(p, isForFile)));
                }
                executableDtoList.add(executableDto);
            }
        }

        // Use getFullProcesses to get full data including processSettings
        List<ProcessConfiguration> fullProcesses = section.getFullProcesses();
        if (!CollectionUtils.isEmpty(fullProcesses)) {
            fullProcesses.stream()
                    .filter(Objects::nonNull)
                    .forEach(p -> executableDtoList.add(makeOldProcess(p, isForFile)));
        }

        if (!CollectionUtils.isEmpty(executableDtoList)) {
            flowConfigSection.processes(executableDtoList);
        }
        return flowConfigSection;
    }

    /**
     * Serialize project files.
     *
     * @param config                   Project Configuration Object.
     * @param projectConfigurationPath project configuration git path.
     */
    private void serializeEtalonFiles(ProjectConfiguration config, Path projectConfigurationPath) {
        log.info("Serializing of etalon files is started for project: {}, configuration path: {}",
                config.getProjectId(), projectConfigurationPath);
        Path etalonFilesPath = projectConfigurationPath.resolve(config.getCommonConfiguration().getEthalonFilesPath());
        try {
            FileUtils.deleteDirectory(etalonFilesPath.toFile());
        } catch (IOException e) {
            log.warn("Could not delete existing etalon directory: {}. Reason: {}", etalonFilesPath, e.getMessage());
        }
        etalonFilesPath.toFile().mkdirs();

        // Create required directories under the etalon path
        config.getAllDirectories().forEach(d -> {
            File dirFile = etalonFilesPath.resolve(d.getPathDirectory()).toFile();
            if (dirFile.exists() && dirFile.isFile()) {
                dirFile.delete();
            }
            dirFile.mkdirs();
        });

        // Copy project files to the etalon directory
        for (ProjectFile f : config.getFiles()) {
            Path filePath = f.getPathFile();
            File sourceFile = PROJECT_FOLDER
                    .resolve(config.getProjectId().toString())
                    .resolve(ProjectFileType.MIA_FILE_TYPE_PROJECT.toString())
                    .resolve(filePath)
                    .toFile();

            try (InputStream is = Files.newInputStream(miaFileService.getFile(sourceFile).toPath())) {
                File targetFile = etalonFilesPath.resolve(filePath).toFile();
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                Files.copy(is, targetFile.toPath());
            } catch (IOException e) {
                log.error("Failed to copy file: {} to etalon path", sourceFile.getAbsolutePath(), e);
            }
        }
        log.info("Serializing of etalon files is completed for project: {}, configuration path: {}",
                config.getProjectId(), projectConfigurationPath);
    }

    private void serializeGeneralConfiguration(ProjectConfiguration config, Path projectConfigurationPath)
            throws IOException {
        Path flowPath = projectConfigurationPath.resolve("flow");
        File flowJsonFile = flowPath.resolve(FILE_FLOW_JSON).toFile();
        flowJsonFile.getParentFile().mkdirs();
        if (flowJsonFile.exists()) {
            flowJsonFile.delete();
        }
        objMapper.writerWithDefaultPrettyPrinter().writeValue(flowJsonFile, getOldConfig(config, true));
        updateProcessFiles(config.getProcesses(), flowPath);
    }

    /**
     * Updates Process Files from Git configuration and re-writes.
     *
     * @param processes processes
     * @param flowPath  flow folder path
     * @throws IOException IOException of reading file
     */
    private void updateProcessFiles(List<ProcessConfiguration> processes, Path flowPath) throws IOException {
        for (ProcessConfiguration proc : processes) {
            proc.getProcessSettings().setName(proc.getName());
            File processFilePath = Strings.isNotBlank(proc.getPathToFile())
                    ? flowPath.resolve(proc.getPathToFile()).toFile()
                    : flowPath.resolve(proc.getName() + ".json").toFile();
            try {
                processFilePath.getParentFile().mkdirs();
                if (processFilePath.exists()) {
                    processFilePath.delete();
                }
            } catch (Exception e) {
                throw new SerializeProcessFileException(e);
            }
            objMapper.writerWithDefaultPrettyPrinter().writeValue(processFilePath, proc.getProcessSettings());
        }
    }
}
