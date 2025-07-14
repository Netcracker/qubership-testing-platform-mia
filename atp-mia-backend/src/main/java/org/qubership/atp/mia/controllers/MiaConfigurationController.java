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

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.mia.controllers.api.MiaConfigurationControllerApi;
import org.qubership.atp.mia.controllers.api.dto.CompoundDto;
import org.qubership.atp.mia.controllers.api.dto.CompoundShortDto;
import org.qubership.atp.mia.controllers.api.dto.FlowConfigDto;
import org.qubership.atp.mia.controllers.api.dto.MoveDirectoryRequestDto;
import org.qubership.atp.mia.controllers.api.dto.MoveProjectFileRequestDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessDto;
import org.qubership.atp.mia.controllers.api.dto.ProcessShortDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectConfigurationDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectDirectoriesDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectDirectoryDto;
import org.qubership.atp.mia.controllers.api.dto.ProjectFileDto;
import org.qubership.atp.mia.controllers.api.dto.SectionDto;
import org.qubership.atp.mia.controllers.api.dto.SectionsDto;
import org.qubership.atp.mia.exceptions.configuration.CompoundNotFoundException;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.service.configuration.CompoundConfigurationService;
import org.qubership.atp.mia.service.configuration.DirectoryConfigurationService;
import org.qubership.atp.mia.service.configuration.FileConfigurationService;
import org.qubership.atp.mia.service.configuration.ProcessConfigurationService;
import org.qubership.atp.mia.service.configuration.ProjectConfigurationService;
import org.qubership.atp.mia.service.configuration.SectionConfigurationService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MiaConfigurationController implements MiaConfigurationControllerApi {

    private final ProjectConfigurationService projectConfigurationService;
    private final ProcessConfigurationService processConfigurationService;
    private final CompoundConfigurationService compoundConfigurationService;
    private final SectionConfigurationService sectionConfigurationService;
    private final DirectoryConfigurationService directoryConfigurationService;
    private final FileConfigurationService fileConfigurationService;
    private final ServletContext servletContext;
    private final LockManager lockManager;

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(), #projectId, 'CREATE')")
    @AuditAction(auditAction = "Add Compound initiated with name \"{{#compoundDto.name}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<CompoundDto> addCompound(UUID projectId, CompoundDto compoundDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(compoundConfigurationService.addCompound(projectConfiguration, compoundDto)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).DIRECTORY.getName(), #projectId, 'CREATE')")
    @AuditAction(auditAction = "Add Directory initiated with name \"{{#projectDirectoryDto.name}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<Void> addDirectory(UUID projectId, ProjectDirectoryDto projectDirectoryDto) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            directoryConfigurationService.addDirectory(projectConfiguration, projectDirectoryDto);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(), #projectId, 'CREATE')")
    @AuditAction(auditAction = "Add Process initiated with name \"{{#processDto.name}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<ProcessDto> addProcess(UUID projectId, ProcessDto processDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(processConfigurationService.addProcess(projectConfiguration, processDto)));
    }

    @Override
    public ResponseEntity<Void> addProjectFile(UUID projectId, ProjectFileDto projectFileDto, MultipartFile file) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            fileConfigurationService.addProjectFile(projectConfiguration, file, projectFileDto);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).SECTION.getName(), #projectId, 'CREATE')")
    @AuditAction(auditAction = "Add Section initiated with name \"{{#sectionDto.name}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<List<SectionsDto>> addSection(UUID projectId, SectionDto sectionDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(sectionConfigurationService.addSection(projectConfiguration, sectionDto)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(), #projectId, 'DELETE')")
    @AuditAction(auditAction = "Delete Compound initiated for compound \"{{#compoundId.name}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<List<CompoundShortDto>> deleteCompound(UUID projectId, UUID compoundId) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(compoundConfigurationService.deleteCompound(projectConfiguration, compoundId)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'DELETE')")
    @AuditAction(auditAction = "Delete Configuration initiated for directory with id \"{{#directoryId}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<Void> deleteConfiguration(UUID projectId, Boolean withPot) {
        projectConfigurationService.removeProject(projectId, withPot);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).DIRECTORY.getName(), #projectId, 'DELETE')")
    @AuditAction(auditAction = "Delete Directory initiated for directory with id \"{{#directoryId}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<Void> deleteDirectory(UUID projectId, UUID directoryId) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            directoryConfigurationService.deleteDirectory(projectConfiguration, directoryId);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(), #projectId, 'DELETE')")
    @AuditAction(auditAction = "Delete Process initiated for process with id \"{{#directoryId}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<List<ProcessShortDto>> deleteProcess(UUID projectId, UUID processId) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(processConfigurationService.deleteProcess(projectConfiguration, processId)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).FILE.getName(), #projectId, 'DELETE')")
    @AuditAction(auditAction = "Delete Project File initiated for file with id \"{{#fileId}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<Void> deleteProjectFile(UUID projectId, UUID fileId) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            fileConfigurationService.deleteProjectFile(projectConfiguration, fileId);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).SECTION.getName(), #projectId, 'DELETE')")
    @AuditAction(auditAction = "Delete Section initiated for section with id \"{{#sectionId}}\" in Project - "
            + "{{#projectId}}")
    public ResponseEntity<List<SectionsDto>> deleteSection(UUID projectId, UUID sectionId) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(sectionConfigurationService.deleteSection(projectConfiguration, sectionId)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Download zip config initiated from Project - {{#projectId}}")
    public ResponseEntity<Resource> downloadZipConfig(UUID projectId) {
        return projectConfigurationService.archiveConfigToZip(projectId);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Compound initiated from Project - {{#projectId}} for compound id - {{#compoundId}}")
    public ResponseEntity<CompoundDto> getCompound(UUID projectId, UUID compoundId) {
        CompoundConfiguration compoundConfiguration = projectConfigurationService.getConfigByProjectId(projectId)
                .getCompounds().stream().filter(cf -> cf.getId().equals(compoundId)).findFirst()
                .orElseThrow(() -> new CompoundNotFoundException(compoundId));
        return ResponseEntity.ok(compoundConfigurationService.toDto(compoundConfiguration));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Compounds initiated for Project - {{#projectId}}")
    public ResponseEntity<List<CompoundShortDto>> getCompounds(UUID projectId) {
        return ResponseEntity.ok(compoundConfigurationService.compoundsDto(
                projectConfigurationService.getConfigByProjectId(projectId).getCompounds()));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get (Old) Config initiated for Project - {{#projectId}} with reload option "
            + "{{#needReload}}")
    public ResponseEntity<FlowConfigDto> getConfig(UUID projectId, Boolean needReload) {
        return ResponseEntity.ok(projectConfigurationService.getOldConfig(projectId));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Configuration initiated for Project - {{#projectId}}")
    public ResponseEntity<ProjectConfigurationDto> getConfiguration(UUID projectId) {
        return ResponseEntity.ok(projectConfigurationService.toDto(
                projectConfigurationService.getConfigByProjectId(projectId)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Configuration with hard reload initiated for Project - {{#projectId}}")
    public ResponseEntity<ProjectConfigurationDto> getConfigurationWithHardReload(UUID projectId) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(
                        projectConfigurationService.toDto(
                                projectConfigurationService.hardReloadConfiguration(projectConfiguration)))
        );
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).DIRECTORY.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Directories initiated for Project - {{#projectId}}")
    public ResponseEntity<ProjectDirectoriesDto> getDirectories(UUID projectId) {
        log.info("Get directories");
        return ResponseEntity.ok(directoryConfigurationService.directoriesDto(
                projectConfigurationService.getConfigByProjectId(projectId)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Process initiated for Project - {{#projectId}} for process id - {{#processId}}")
    public ResponseEntity<ProcessDto> getProcess(UUID projectId, UUID processId) {
        log.info("Get process with ID '{}'", processId);
        return ResponseEntity.ok(processConfigurationService.toDto(
                processConfigurationService.getProcessById(
                        projectConfigurationService.getConfigByProjectId(projectId), processId)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Processes initiated for Project - {{#projectId}}")
    public ResponseEntity<List<ProcessShortDto>> getProcesses(UUID projectId) {
        return ResponseEntity.ok(processConfigurationService.processesDto(
                projectConfigurationService.getConfigByProjectId(projectId).getProcesses()));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).FILE.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Project File initiated for Project - {{#projectId}}"
            + " for File id - {{#projectFileId}}")
    public ResponseEntity<Resource> getProjectFile(UUID projectId, UUID projectFileId) {
        return fileConfigurationService.getProjectFile(projectId, projectFileId, servletContext);
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).SECTION.getName(), #projectId, 'READ')")
    @AuditAction(auditAction = "Get Sections initiated for Project - {{#projectId}}")
    public ResponseEntity<List<SectionsDto>> getSections(UUID projectId) {
        return ResponseEntity.ok(sectionConfigurationService.sectionsDto(
                projectConfigurationService.getConfigByProjectId(projectId).getRootSections()));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).DIRECTORY.getName(), #projectId, 'READ')")
    public ResponseEntity<List<UUID>> getUuidDirectories(UUID projectId) {
        return ResponseEntity.ok(projectConfigurationService.getConfiguration(projectId)
                .getDirectories()
                .stream().map(ProjectDirectory::getId).collect(Collectors.toList()));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).FILE.getName(), #projectId, 'READ')")
    public ResponseEntity<List<UUID>> getUuidFiles(UUID projectId) {
        return ResponseEntity.ok(projectConfigurationService.getConfiguration(projectId).getFiles()
                .stream().map(ProjectFile::getId).collect(Collectors.toList()));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).DIRECTORY.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Move directory initiated for Project - {{#projectId}} for"
            + " Directory id - {{#directoryId}}")
    public ResponseEntity<Void> moveDirectory(UUID projectId, UUID directoryId,
                                              MoveDirectoryRequestDto moveDirectoryRequestDto) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            directoryConfigurationService.moveDirectory(projectConfiguration, directoryId, moveDirectoryRequestDto);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).FILE.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Move directory initiated for Project - {{#projectId}} for"
            + " file id - {{#fileId}}")
    public ResponseEntity<Void> moveProjectFile(UUID projectId, UUID fileId,
                                                MoveProjectFileRequestDto moveProjectFileRequestDto) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            fileConfigurationService.moveProjectFile(projectConfiguration, fileId, moveProjectFileRequestDto);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).COMPOUND.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Update Compound initiated for compound with id - {{#compoundDto}} from Project - "
            + "{{#projectId}}")
    public ResponseEntity<CompoundDto> updateCompound(UUID projectId, CompoundDto compoundDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(compoundConfigurationService.updateCompound(projectConfiguration, compoundDto)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Update configuration initiated for Project - {{#projectId}}")
    public ResponseEntity<ProjectConfigurationDto> updateConfiguration(UUID projectId,
                                                                       ProjectConfigurationDto
                                                                               projectConfigurationRequestDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(projectConfigurationService.updateConfiguration(projectConfiguration,
                        projectConfigurationRequestDto)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).DIRECTORY.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Update directory initiated for directory with name - {{#projectDirectoryDto.name}} "
            + "from Project - {{#projectId}}")
    public ResponseEntity<Void> updateDirectory(UUID projectId, ProjectDirectoryDto projectDirectoryDto) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            directoryConfigurationService.updateDirectory(projectConfiguration, projectDirectoryDto);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).PROCESS.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Update Process initiated for process with name - {{#processDto.name}} from Project - "
            + "{{#projectId}}")
    public ResponseEntity<ProcessDto> updateProcess(UUID projectId, ProcessDto processDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(processConfigurationService.updateProcess(projectConfiguration, processDto)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).FILE.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Update Project File initiated for file with name - {{#projectFileDto.name}} from "
            + "Project - {{#projectId}}")
    public ResponseEntity<Void> updateProjectFile(UUID projectId, ProjectFileDto projectFileDto, MultipartFile file) {
        return doConfigurationChange(projectId, projectConfiguration -> {
            fileConfigurationService.updateProjectFile(projectConfiguration, file, projectFileDto);
            return ResponseEntity.ok().build();
        });
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).SECTION.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "Update Section initiated for section with name {{#sectionDto.name}}  from Project - "
            + "{{#projectId}}")
    public ResponseEntity<List<SectionsDto>> updateSection(UUID projectId, SectionDto sectionDto) {
        return doConfigurationChange(projectId, projectConfiguration ->
                ResponseEntity.ok(sectionConfigurationService.updateSection(projectConfiguration, sectionDto)));
    }

    @Override
    @PreAuthorize("@entityAccess.checkAccess("
            + "T(org.qubership.atp.mia.model.UserManagementEntities).CONFIGURATION.getName(), #projectId, 'UPDATE')")
    @AuditAction(auditAction = "UploadZipConfig initiated for Project - {{#projectId}}")
    public ResponseEntity<Boolean> uploadZipConfig(UUID projectId, MultipartFile file) {
        projectConfigurationService.loadConfigFromZip(projectId, file);
        return ResponseEntity.ok(true);
    }

    private ResponseEntity doConfigurationChange(UUID projectId,
                                                 Function<ProjectConfiguration, ResponseEntity<?>> action) {
        return lockManager.executeWithLock("controller_update_config_" + projectId,
                () -> action.apply(projectConfigurationService.getConfigByProjectId(projectId)),
                () -> ResponseEntity.internalServerError()
                        .body("Lock Manager failed 'controller_update_config' for project " + projectId));
    }
}
