package org.qubership.atp.mia.service.configuration.snapshot;

import java.io.Serializable;
import java.util.UUID;

import org.qubership.atp.mia.model.configuration.ProjectConfiguration;

/**
 * Aggregates lightweight projections of general project configuration parts.
 */
public final class ProjectGeneralConfigurationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID projectId;
    private final CommonConfigurationSnapshot commonConfiguration;
    private final HeaderConfigurationSnapshot headerConfiguration;
    private final PotHeaderConfigurationSnapshot potHeaderConfiguration;

    private ProjectGeneralConfigurationSnapshot(UUID projectId,
                                                CommonConfigurationSnapshot commonConfiguration,
                                                HeaderConfigurationSnapshot headerConfiguration,
                                                PotHeaderConfigurationSnapshot potHeaderConfiguration) {
        this.projectId = projectId;
        this.commonConfiguration = commonConfiguration;
        this.headerConfiguration = headerConfiguration;
        this.potHeaderConfiguration = potHeaderConfiguration;
    }

    public static ProjectGeneralConfigurationSnapshot from(ProjectConfiguration projectConfiguration) {
        return new ProjectGeneralConfigurationSnapshot(
                projectConfiguration.getProjectId(),
                CommonConfigurationSnapshot.from(projectConfiguration.getCommonConfiguration()),
                HeaderConfigurationSnapshot.from(projectConfiguration.getHeaderConfiguration()),
                PotHeaderConfigurationSnapshot.from(projectConfiguration.getPotHeaderConfiguration())
        );
    }

    public UUID getProjectId() {
        return projectId;
    }

    public CommonConfigurationSnapshot getCommonConfiguration() {
        return commonConfiguration;
    }

    public HeaderConfigurationSnapshot getHeaderConfiguration() {
        return headerConfiguration;
    }

    public PotHeaderConfigurationSnapshot getPotHeaderConfiguration() {
        return potHeaderConfiguration;
    }
}


