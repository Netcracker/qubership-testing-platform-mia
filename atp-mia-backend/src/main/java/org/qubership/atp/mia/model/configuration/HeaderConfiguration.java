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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.Value;
import org.qubership.atp.mia.model.Constants;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "project_header_configuration")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Value
public class HeaderConfiguration implements Serializable {

    private static final long serialVersionUID = -1708851849749487504L;
    @Id
    @Column(name = "project_id")
    private UUID projectId;
    @Column(name = "show_geneva_date_block")
    @Builder.Default
    @DiffInclude
    private boolean showGenevaDateBlock = true;
    @Column(name = "show_working_directory")
    @Builder.Default
    @DiffInclude
    private boolean showWorkingDirectory = true;
    @Column(name = "show_reset_db_cache")
    @Builder.Default
    @DiffInclude
    private boolean showResetDbCache = false;
    @Column(name = "show_update_config")
    @Builder.Default
    @DiffInclude
    private boolean showUpdateConfig = true;
    @Column(name = "show_test_data")
    @Builder.Default
    @DiffInclude
    private boolean showTestData = false;
    @Column(name = "show_time_shifting")
    @Builder.Default
    @DiffInclude
    private boolean showTimeShifting = false;
    @Column(name = "export_toggle_default_position")
    @Builder.Default
    @DiffInclude
    private boolean exportToggleDefaultPosition = true;
    @Column(name = "enable_update_flow_json_config")
    @Builder.Default
    @DiffInclude
    private boolean enableUpdateFlowJsonConfig = true;
    @Column(name = "working_directory")
    @Builder.Default
    @DiffInclude
    private String workingDirectory = Constants.DEFAULT_WORKING_DIRECTORY;
    @Column(name = "switchers", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    @Builder.Default
    @DiffInclude
    private List<Switcher> switchers = new ArrayList<>();
    @Column(name = "system_switchers", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    @Builder.Default
    @DiffIgnore
    private List<Switcher> systemSwitchers = HeaderConfiguration.defaultSystemSwitchers();
    @OneToOne(targetEntity = ProjectConfiguration.class, cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @DiffIgnore
    private ProjectConfiguration projectConfiguration;

    /**
     * Default System Switchers.
     *
     * @return List of Switcher
     */
    public static List<Switcher> defaultSystemSwitchers() {
        Switcher sw1 = new Switcher();
        sw1.setName("fullTraceMode");
        sw1.setDisplay("Full Trace On/Off");
        sw1.setActionType("SSH");
        sw1.setValue(false);
        sw1.setActionTrue("export TRACE_LEVEL=FULL\nexport TRACE_ALL=ON");
        sw1.setActionFalse(null);
        Switcher sw2 = new Switcher();
        sw2.setName("needDos2Unix");
        sw2.setDisplay("needDos2Unix");
        sw2.setValue(false);
        Switcher sw3 = new Switcher();
        sw3.setName("replaceFileOnUploadAttachment");
        sw3.setDisplay("Replace/Add timestamp on upload attachment");
        sw3.setValue(true);
        sw3.setActionTrue("replace");
        sw3.setActionFalse("add timestamp");
        Switcher sw4 = new Switcher();
        sw4.setName("stopOnFail");
        sw4.setDisplay("Stop compound if one of processes is fail");
        sw4.setValue(true);
        return Arrays.asList(sw1, sw2, sw3, sw4);
    }

    /**
     * Sets system switchers.
     *
     * @param systemSwitchers systemSwitchers.
     */
    public void setSystemSwitchers(List<Switcher> systemSwitchers) {
        this.systemSwitchers.forEach(sw -> {
            Optional<Switcher> swConfig = systemSwitchers.stream()
                    .filter(swC -> swC.getName().equals(sw.getName())).findAny();
            if (swConfig.isPresent()) {
                sw.setValue(swConfig.get().isValue());
            }
        });
    }
}
