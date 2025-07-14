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

package org.qubership.atp.mia.model.configuration;

import java.util.UUID;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.exceptions.externalsystemintegrations.ProjectNotFoundException;

public class ProjectsJsonTest {

    @Test
    public void ProjectsJsonConstructorTest() {
        // For Code coverage. Can be removed later.
        ProjectsJson projectsJson = new ProjectsJson();
        projectsJson.setId("1");
        projectsJson.setName("defaultProject");
        projectsJson.setException(new ProjectNotFoundException(UUID.randomUUID()));
        projectsJson.setSuccessfulLoad(true);
        projectsJson.setConfigUrl("https://git.somedomain.com/Personal/Custom_projects/PROJ/project_mia");
        Assert.assertEquals("1", projectsJson.getId());
        Assert.assertEquals("defaultProject", projectsJson.getName());
        Assert.assertTrue(projectsJson.isSuccessfulLoad());
        Assert.assertEquals("https://git.somedomain.com/Personal/Custom_projects/PROJ/project_mia",
                projectsJson.getConfigUrl());
    }
}
