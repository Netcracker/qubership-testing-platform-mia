package org.qubership.atp.mia.service.configuration.snapshot;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.configuration.CommonConfiguration;

/**
 * Lightweight immutable copy of {@link CommonConfiguration} used inside cached snapshots.
 */
public final class CommonConfigurationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID projectId;
    private final String defaultSystem;
    private final boolean useVariablesInsideVariable;
    private final String variableFormat;
    private final boolean saveFilesToWorkingDir;
    private final boolean saveSqlTablesToFile;
    private final Map<String, String> commonVariables;
    private final String nextBillDateSql;
    private final String resetCacheSql;
    private final String ethalonFilesPath;
    private final String externalEnvironmentPrefix;
    private final String commandShellSeparator;
    private final List<CommandPrefixSnapshot> commandShellPrefixes;
    private final String genevaDateMask;
    private final String sshRsaFilePath;
    private final int linesAmount;

    private CommonConfigurationSnapshot(UUID projectId,
                                        String defaultSystem,
                                        boolean useVariablesInsideVariable,
                                        String variableFormat,
                                        boolean saveFilesToWorkingDir,
                                        boolean saveSqlTablesToFile,
                                        Map<String, String> commonVariables,
                                        String nextBillDateSql,
                                        String resetCacheSql,
                                        String ethalonFilesPath,
                                        String externalEnvironmentPrefix,
                                        String commandShellSeparator,
                                        List<CommandPrefixSnapshot> commandShellPrefixes,
                                        String genevaDateMask,
                                        String sshRsaFilePath,
                                        int linesAmount) {
        this.projectId = projectId;
        this.defaultSystem = defaultSystem;
        this.useVariablesInsideVariable = useVariablesInsideVariable;
        this.variableFormat = variableFormat;
        this.saveFilesToWorkingDir = saveFilesToWorkingDir;
        this.saveSqlTablesToFile = saveSqlTablesToFile;
        this.commonVariables = commonVariables;
        this.nextBillDateSql = nextBillDateSql;
        this.resetCacheSql = resetCacheSql;
        this.ethalonFilesPath = ethalonFilesPath;
        this.externalEnvironmentPrefix = externalEnvironmentPrefix;
        this.commandShellSeparator = commandShellSeparator;
        this.commandShellPrefixes = commandShellPrefixes;
        this.genevaDateMask = genevaDateMask;
        this.sshRsaFilePath = sshRsaFilePath;
        this.linesAmount = linesAmount;
    }

    public static CommonConfigurationSnapshot from(CommonConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        Map<String, String> commonVariablesCopy = configuration.getCommonVariables() == null
                ? Collections.emptyMap()
                : new HashMap<>(configuration.getCommonVariables());
        List<CommandPrefixSnapshot> commandPrefixes = configuration.getCommandShellPrefixes() == null
                ? Collections.emptyList()
                : configuration.getCommandShellPrefixes().stream()
                .filter(Objects::nonNull)
                .map(CommandPrefixSnapshot::from)
                .collect(Collectors.toList());
        return new CommonConfigurationSnapshot(
                configuration.getProjectId(),
                configuration.getDefaultSystem(),
                configuration.isUseVariablesInsideVariable(),
                configuration.getVariableFormat(),
                configuration.isSaveFilesToWorkingDir(),
                configuration.isSaveSqlTablesToFile(),
                Collections.unmodifiableMap(commonVariablesCopy),
                configuration.getNextBillDateSql(),
                configuration.getResetCacheSql(),
                configuration.getEthalonFilesPath(),
                configuration.getExternalEnvironmentPrefix(),
                configuration.getCommandShellSeparator(),
                commandPrefixes,
                configuration.getGenevaDateMask(),
                configuration.getSshRsaFilePath(),
                configuration.getLinesAmount()
        );
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getDefaultSystem() {
        return defaultSystem;
    }

    public boolean isUseVariablesInsideVariable() {
        return useVariablesInsideVariable;
    }

    public String getVariableFormat() {
        return variableFormat;
    }

    public boolean isSaveFilesToWorkingDir() {
        return saveFilesToWorkingDir;
    }

    public boolean isSaveSqlTablesToFile() {
        return saveSqlTablesToFile;
    }

    public Map<String, String> getCommonVariables() {
        return commonVariables;
    }

    public String getNextBillDateSql() {
        return nextBillDateSql;
    }

    public String getResetCacheSql() {
        return resetCacheSql;
    }

    public String getEthalonFilesPath() {
        return ethalonFilesPath;
    }

    public String getExternalEnvironmentPrefix() {
        return externalEnvironmentPrefix;
    }

    public String getCommandShellSeparator() {
        return commandShellSeparator;
    }

    public List<CommandPrefixSnapshot> getCommandShellPrefixes() {
        return commandShellPrefixes;
    }

    public String getGenevaDateMask() {
        return genevaDateMask;
    }

    public String getSshRsaFilePath() {
        return sshRsaFilePath;
    }

    public int getLinesAmount() {
        return linesAmount;
    }
}


