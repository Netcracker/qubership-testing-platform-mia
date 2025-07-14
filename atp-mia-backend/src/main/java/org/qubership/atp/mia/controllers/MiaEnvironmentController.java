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

import org.qubership.atp.integration.configuration.configuration.AuditAction;
import org.qubership.atp.mia.clients.api.environments.dto.projects.SystemEnvironmentsViewDto;
import org.qubership.atp.mia.model.environment.AbstractConfiguratorModel;
import org.qubership.atp.mia.model.environment.Environment;
import org.qubership.atp.mia.model.environment.Project;
import org.qubership.atp.mia.service.configuration.EnvironmentsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/rest")
@RestController()
public class MiaEnvironmentController /*implements ProjectControllerApi*/ {

    private final EnvironmentsService service;

    public MiaEnvironmentController(EnvironmentsService service) {
        this.service = service;
    }

    /**
     * Get project.
     *
     * @return Project from atp-environment
     */
    @GetMapping("/project")
    @AuditAction(auditAction = "Get Project details for projectId - {{#projectId}}")
    public Project getProject(@RequestParam(value = "projectId") UUID projectId) {
        return service.getProject(projectId);
    }

    /**
     * Get list environments.
     *
     * @return List environment from atp-environment
     */
    @GetMapping("/environments")
    @AuditAction(auditAction = "Get Environments for projectId - {{#projectId}}")
    public List<AbstractConfiguratorModel> getEnvironments(@RequestParam(value = "projectId") UUID projectId) {
        return service.getEnvironmentsByProject(projectId);
    }

    /**
     * Get full information about environment..
     *
     * @return Environment or RuntimeException if environment not found
     */
    @GetMapping("/environments/full")
    @AuditAction(auditAction = "Get Full Environment details for projectId - {{#projectId}} and environmentId - "
            + "{{#environmentId}}")
    public Environment getEnvironmentsFull(@RequestParam(value = "projectId") UUID projectId,
                                           @RequestParam(value = "environmentId") UUID environmentId) {
        return service.getEnvironmentsFull(environmentId, projectId);
    }

    /**
     * Get all systems for selected project.
     *
     * @param projectId of project.
     * @return list of systems for selected project.
     */
    @GetMapping("/environments/systems")
    @AuditAction(auditAction = "Get all systems for projectId - {{#projectId}}")
    public List<SystemEnvironmentsViewDto> getSystemsForProject(@RequestParam(value = "projectId") UUID projectId) {
        return service.getSystemsForProject(projectId);
    }

    /**
     * Get project.
     *
     * @return Project from environment
     */
    @GetMapping("/projects")
    @AuditAction(auditAction = "Get all projects")
    public List<AbstractConfiguratorModel> getProjects() {
        return service.getProjects();
    }
}
