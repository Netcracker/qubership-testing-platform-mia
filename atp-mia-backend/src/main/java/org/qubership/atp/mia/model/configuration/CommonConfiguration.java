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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.impl.VariableFormat;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "project_common_configuration")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Value
public class CommonConfiguration implements Serializable {

    private static final long serialVersionUID = -1842137845749660674L;
    @Id
    @Column(name = "project_id")
    private UUID projectId;
    @Column(name = "default_system")
    @Builder.Default
    @DiffInclude
    private String defaultSystem = Constants.DEFAULT_SYSTEM_NAME;
    @Column(name = "use_variables_inside_variable")
    @DiffInclude
    private boolean useVariablesInsideVariable = false;
    @Column(name = "variable_format")
    @Builder.Default
    @DiffInclude
    private String variableFormat = VariableFormat.DEFAULT_FORMAT;
    @Column(name = "save_files_to_working_dir")
    @DiffInclude
    private boolean saveFilesToWorkingDir;
    @Column(name = "save_sql_tables_to_file")
    @Builder.Default
    @DiffInclude
    private boolean saveSqlTablesToFile = true;
    @Column(name = "common_variables", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    @Builder.Default
    @DiffInclude
    private HashMap<String, String> commonVariables = new HashMap<>();
    @Column(name = "next_bill_date_sql")
    @Builder.Default
    @DiffInclude
    private String nextBillDateSql = "SELECT NEXT_BILL_DTM FROM ACCOUNT WHERE ACCOUNT_NUM =':accountNumber'";
    @Column(name = "reset_cache_sql")
    @Builder.Default
    @DiffInclude
    private String resetCacheSql = "{call gnvsessiongparams.clearcache()}";
    @Column(name = "ethalon_files_path")
    @Builder.Default
    @DiffInclude
    private String ethalonFilesPath = "etalon_files";
    @Column(name = "external_environment_prefix")
    @Builder.Default
    @DiffInclude
    private String externalEnvironmentPrefix = "";
    @Column(name = "command_shell_separator")
    @Builder.Default
    @DiffInclude
    private String commandShellSeparator = "\n";
    @OneToMany(mappedBy = "commonConfiguration", targetEntity = CommandPrefix.class, cascade = CascadeType.MERGE,
            orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Builder.Default
    @LazyCollection(LazyCollectionOption.FALSE)
    @DiffInclude
    private List<CommandPrefix> commandShellPrefixes = new ArrayList<>();
    @Column(name = "geneva_date_mask")
    @Builder.Default
    @DiffInclude
    private String genevaDateMask = "YYYYMMDD HHMMSSSS"; //for FE usages
    @Column(name = "ssh_rsa_file_path")
    @DiffInclude
    private String sshRsaFilePath;
    @Column(name = "lines_amount")
    @Builder.Default
    @DiffInclude
    private int linesAmount = 3;
    @OneToOne(targetEntity = ProjectConfiguration.class, cascade = CascadeType.MERGE)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    /**
     * Correct set ethalon files.
     *
     * @param ethalonFilesPath ethalonFilesPath
     */
    public void setEthalonFilesPath(String ethalonFilesPath) {
        this.ethalonFilesPath = ethalonFilesPath.trim().replaceAll("\\./", "").replaceAll("/", "");
    }

    /**
     * Update shell prefixes.
     */
    public void updateShellPrefixes() {
        commandShellPrefixes.forEach(prefix -> {
            prefix.setProjectId(projectConfiguration != null ? projectConfiguration.getProjectId() : projectId);
            prefix.setCommonConfiguration(this);
        });
    }
}
