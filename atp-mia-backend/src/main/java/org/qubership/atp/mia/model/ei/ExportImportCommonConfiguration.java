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

package org.qubership.atp.mia.model.ei;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.configuration.CommonConfiguration;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
public class ExportImportCommonConfiguration extends ExportImportIdentifier {

    private static final long serialVersionUID = -429059651116173040L;

    private String defaultSystem;
    private boolean useVariablesInsideVariable;
    private String variableFormat;
    private boolean saveFilesToWorkingDir;
    private boolean saveSqlTablesToFile;
    private HashMap<String, String> commonVariables;
    private String nextBillDateSql;
    private String resetCacheSql;
    private String ethalonFilesPath;
    private String externalEnvironmentPrefix;
    private String commandShellSeparator;
    private List<ExportImportCommandPrefix> commandShellPrefixes;
    private String genevaDateMask;
    private String sshRsaFilePath;
    private int linesAmount;

    /**
     * Create new one from CommonConfiguration.
     *
     * @param c CommonConfiguration
     */
    public ExportImportCommonConfiguration(CommonConfiguration c) {
        super(c.getProjectId(), "CommonConfiguration", c.getProjectId(), c.getProjectId());
        this.defaultSystem = c.getDefaultSystem();
        this.useVariablesInsideVariable = c.isUseVariablesInsideVariable();
        this.variableFormat = c.getVariableFormat();
        this.saveFilesToWorkingDir = c.isSaveFilesToWorkingDir();
        this.saveSqlTablesToFile = c.isSaveSqlTablesToFile();
        this.commonVariables = c.getCommonVariables();
        this.nextBillDateSql = c.getNextBillDateSql();
        this.resetCacheSql = c.getResetCacheSql();
        this.ethalonFilesPath = c.getEthalonFilesPath();
        this.externalEnvironmentPrefix = c.getExternalEnvironmentPrefix();
        this.commandShellSeparator = c.getCommandShellSeparator();
        this.commandShellPrefixes = c.getCommandShellPrefixes().stream()
                .map(ExportImportCommandPrefix::new)
                .collect(Collectors.toList());
        this.genevaDateMask = c.getGenevaDateMask();
        this.sshRsaFilePath = c.getSshRsaFilePath();
        this.linesAmount = c.getLinesAmount();
    }
}
