package org.qubership.atp.mia.service.configuration.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.Switcher;

/**
 * Immutable copy of {@link HeaderConfiguration} for cached access.
 */
public final class HeaderConfigurationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID projectId;
    private final boolean showGenevaDateBlock;
    private final boolean showWorkingDirectory;
    private final boolean showResetDbCache;
    private final boolean showUpdateConfig;
    private final boolean showTestData;
    private final boolean showTimeShifting;
    private final boolean exportToggleDefaultPosition;
    private final boolean enableUpdateFlowJsonConfig;
    private final String workingDirectory;
    private final List<Switcher> switchers;
    private final List<Switcher> systemSwitchers;

    private HeaderConfigurationSnapshot(UUID projectId,
                                        boolean showGenevaDateBlock,
                                        boolean showWorkingDirectory,
                                        boolean showResetDbCache,
                                        boolean showUpdateConfig,
                                        boolean showTestData,
                                        boolean showTimeShifting,
                                        boolean exportToggleDefaultPosition,
                                        boolean enableUpdateFlowJsonConfig,
                                        String workingDirectory,
                                        List<Switcher> switchers,
                                        List<Switcher> systemSwitchers) {
        this.projectId = projectId;
        this.showGenevaDateBlock = showGenevaDateBlock;
        this.showWorkingDirectory = showWorkingDirectory;
        this.showResetDbCache = showResetDbCache;
        this.showUpdateConfig = showUpdateConfig;
        this.showTestData = showTestData;
        this.showTimeShifting = showTimeShifting;
        this.exportToggleDefaultPosition = exportToggleDefaultPosition;
        this.enableUpdateFlowJsonConfig = enableUpdateFlowJsonConfig;
        this.workingDirectory = workingDirectory;
        this.switchers = switchers;
        this.systemSwitchers = systemSwitchers;
    }

    public static HeaderConfigurationSnapshot from(HeaderConfiguration headerConfiguration) {
        if (headerConfiguration == null) {
            return null;
        }
        return new HeaderConfigurationSnapshot(
                headerConfiguration.getProjectId(),
                headerConfiguration.isShowGenevaDateBlock(),
                headerConfiguration.isShowWorkingDirectory(),
                headerConfiguration.isShowResetDbCache(),
                headerConfiguration.isShowUpdateConfig(),
                headerConfiguration.isShowTestData(),
                headerConfiguration.isShowTimeShifting(),
                headerConfiguration.isExportToggleDefaultPosition(),
                headerConfiguration.isEnableUpdateFlowJsonConfig(),
                headerConfiguration.getWorkingDirectory(),
                copySwitchers(headerConfiguration.getSwitchers()),
                copySwitchers(headerConfiguration.getSystemSwitchers())
        );
    }

    private static List<Switcher> copySwitchers(List<Switcher> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(source.stream()
                .filter(Objects::nonNull)
                .map(s -> Switcher.builder()
                        .name(s.getName())
                        .display(s.getDisplay())
                        .value(s.isValue())
                        .actionType(s.getActionType())
                        .actionTrue(s.getActionTrue())
                        .actionFalse(s.getActionFalse())
                        .build())
                .collect(Collectors.toList()));
    }

    public UUID getProjectId() {
        return projectId;
    }

    public boolean isShowGenevaDateBlock() {
        return showGenevaDateBlock;
    }

    public boolean isShowWorkingDirectory() {
        return showWorkingDirectory;
    }

    public boolean isShowResetDbCache() {
        return showResetDbCache;
    }

    public boolean isShowUpdateConfig() {
        return showUpdateConfig;
    }

    public boolean isShowTestData() {
        return showTestData;
    }

    public boolean isShowTimeShifting() {
        return showTimeShifting;
    }

    public boolean isExportToggleDefaultPosition() {
        return exportToggleDefaultPosition;
    }

    public boolean isEnableUpdateFlowJsonConfig() {
        return enableUpdateFlowJsonConfig;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public List<Switcher> getSwitchers() {
        return switchers;
    }

    public List<Switcher> getSystemSwitchers() {
        return systemSwitchers;
    }
}


