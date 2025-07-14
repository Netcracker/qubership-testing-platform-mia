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

package org.qubership.atp.mia.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled
@ExtendWith(SkipTestInJenkins.class)
@PowerMockIgnore(value = {"javax.management.*"})
@SpringBootTest(classes = {ProcessService.class}, properties = {"spring.cloud.vault.enabled=false"})
public class DeserializeNewConfigTest extends DeserializerConfigBaseTest {
/*
    public static void compareConfigs(ProjectConfiguration expectedConfig, ProjectConfiguration actualConfig) {
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().getFirst(),
                actualConfig.getSections().getLast().getSections().getFirst());
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(0),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(0));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(1),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(1));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(2),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(2));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(3),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(3));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(4),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(4));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(5),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(5));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1).getProcesses().get(6),
                actualConfig.getSections().getLast().getSections().get(1).getProcesses().get(6));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(1),
                actualConfig.getSections().getLast().getSections().get(1));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(2),
                actualConfig.getSections().getLast().getSections().get(2));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(3).getProcesses().get(0),
                actualConfig.getSections().getLast().getSections().get(3).getProcesses().get(0));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(4).getProcesses().get(0),
                actualConfig.getSections().getLast().getSections().get(4).getProcesses().get(0));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(4).getProcesses().get(1),
                actualConfig.getSections().getLast().getSections().get(4).getProcesses().get(1));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(4).getProcesses().get(2),
                actualConfig.getSections().getLast().getSections().get(4).getProcesses().get(2));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(4).getProcesses().get(3),
                actualConfig.getSections().getLast().getSections().get(4).getProcesses().get(3));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(5).getProcesses().get(0),
                actualConfig.getSections().getLast().getSections().get(5).getProcesses().get(0));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(5).getProcesses().get(1),
                actualConfig.getSections().getLast().getSections().get(5).getProcesses().get(1));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(5).getProcesses().get(2),
                actualConfig.getSections().getLast().getSections().get(5).getProcesses().get(2));
        Assert.assertEquals(expectedConfig.getSections().getLast().getSections().get(5).getProcesses().get(3),
                actualConfig.getSections().getLast().getSections().get(5).getProcesses().get(3));
        Assert.assertEquals(expectedConfig.getSections().getLast().getProcesses().get(0),
                actualConfig.getSections().getLast().getProcesses().get(0));
        Assert.assertEquals(expectedConfig.getSections().getLast(),
                actualConfig.getSections().getLast());
        Assert.assertEquals(expectedConfig.getSections().getFirst().getProcesses().get(0),
                actualConfig.getSections().getFirst().getProcesses().get(0));
        Assert.assertEquals(expectedConfig.getSections(), actualConfig.getSections());
    }

    @Test
    public void getConfig_useFlowConfig_deserializesProperly() {
        compareConfigs(getDefaultConfig(), service.getConfig());
    }

    @Test
    public void testValidationString_whenDuplicatesInProcess() {
        ProjectConfiguration config = getCustomConfig(true, false, false);
        StringJoiner actual = config.checkThisConfigForDuplicates();
        String expected = ErrorCodes.MIA_0050_DUPLICATE_CONFIG.getMessage(
                "1) process duplicate: \"SSH_BG\" in section \"sectionSQL\";\n"
                        + "2) process duplicate: \"SQL_GPARAMS\" in section \"sectionSQL\";\n");
        Assert.assertEquals(expected, actual.toString());
    }

    @Test
    public void testValidationString_whenDuplicatesInCompound() {
        ProjectConfiguration config = getCustomConfig(false, false, true);
        StringJoiner actual = config.checkThisConfigForDuplicates();
        String expected = ErrorCodes.MIA_0050_DUPLICATE_CONFIG.getMessage(
                "1) process: \"SQL_GPARAMS\" which links to filename: \"./ethalon_file/folder\"\n"
                        + "but found duplicate which links to other filename: \"./ethalon_file/\"\n"
                        + " in compound: \"compound2\" in section: \"sectionSQL\";\n");
        Assert.assertEquals(expected, actual.toString());
    }

    @Test
    public void testValidationString_whenDuplicatesInSection() {
        ProjectConfiguration config = getCustomConfig(false, true, false);
        StringJoiner actual = config.checkThisConfigForDuplicates();
        String expected = ErrorCodes.MIA_0050_DUPLICATE_CONFIG.getMessage(
                 "1) section: \"sectionSQL\" section;\n");
        Assert.assertEquals(expected, actual.toString());
    }

    @Test
    public void testValidationString_whenNoDuplicates() {
        ProjectConfiguration config = getCustomConfig(false, false, false);
        Assert.assertEquals("", config.checkThisConfigForDuplicates().toString());
    }

    @Test
    public void testValidationString_whenDefaultConfig() {
        ProjectConfiguration config = getCustomConfig(false, false, false);
        Assert.assertEquals("", config.checkThisConfigForDuplicates().toString());
    }

    private ProjectConfiguration getDefaultConfig() {
        final ProjectConfiguration config = new ProjectConfiguration();
        final LinkedList<FlowConfigSection> sections = new LinkedList<>();
        final LinkedList<Process> processes = new LinkedList<>();
        processes.add(getSql());
        processes.add(getBg());
        processes.add(getTransfer());
        processes.add(getMultiDownload());
        processes.add(getCheck());
        processes.add(getGenerationPp());
        processes.add(getDump());
        processes.add(getDefaultRest());
        processes.add(getSoap());
        processes.add(getEventS());
        processes.add(getFillInput());
        processes.add(getEventFileProjectThree());
        ArrayList<Input> inputs = getDefaultInputs();
        ArrayList<Validation> currStatement = getDefaultCurrentStatement();
        LinkedList<Executable> executables = new LinkedList<>();
        executables.add(new Compound().toBuilder().name("Compound Name").processList(processes)
                .inputs(inputs).currentStatement(currStatement).build());
        sections.add(new FlowConfigSection("CM", executables));
        final LinkedList<FlowConfigSection> billingSystemSections = new LinkedList<>();
        executables = new LinkedList<>();
        executables.add(getSql());
        billingSystemSections.add(new FlowConfigSection("SQL", executables));
        executables = new LinkedList<>();
        executables.add(getBg());
        executables.add(getTransfer());
        executables.add(getMultiDownload());
        executables.add(getCheck());
        executables.add(getGenerationPp());
        executables.add(getDump());
        executables.add(getBgWithMarker());
        executables.add(getUploadFile());
        billingSystemSections.add(new FlowConfigSection("SSH", executables));
        executables = new LinkedList<>();
        executables.add(getDefaultRest());
        executables.add(getRestBodyFromFile());
        executables.add(getRestFromFile());
        billingSystemSections.add(new FlowConfigSection("REST", executables));
        executables = new LinkedList<>();
        executables.add(getSoap());
        billingSystemSections.add(new FlowConfigSection("SOAP", executables));
        executables = new LinkedList<>();
        executables.add(getEventS());
        executables.add(getEventFileProjectThree());
        executables.add(getEventFileProjectOne());
        executables.add(getEventFileProjectTwo());
        billingSystemSections.add(new FlowConfigSection("EVENTS", executables));
        executables = new LinkedList<>();
        executables.add(getEventTestData());
        executables.add(getVerifyTestData());
        executables.add(getSshTestData());
        executables.add(getSoapTestData());
        executables.add(getSqlTestData());
        billingSystemSections.add(new FlowConfigSection("Processes from Test data", executables));
        executables = new LinkedList<>();
        executables.add(getFillInput());
        sections.add(new FlowConfigSection("Billing System", executables, billingSystemSections));
        config.setSections(sections);
        return config;
    }

    private ProjectConfiguration getCustomConfig(boolean allowDuplicatesInProcess,
                                                 boolean allowDuplicatesInSections,
                                                 boolean allowDuplicatesInCompound) {
        final ProjectConfiguration config = new ProjectConfiguration();
        final LinkedList<FlowConfigSection> sections = new LinkedList<>();
        FlowConfigSection section = new FlowConfigSection();
        if (allowDuplicatesInProcess) {
            section.addProcess(getSql());
            section.addProcess(getBg());
        }
        if (allowDuplicatesInCompound) {
            addCompoundDuplicates(section);
        }
        section.addProcess(getSql());
        section.addProcess(getBg());
        section.addProcess(getBgWithMarker());
        section.setName("sectionSQL");
        sections.add(section);
        if (allowDuplicatesInSections) {
            FlowConfigSection section1 = section.clone();
            FlowConfigSection section2 = section.clone();
            sections.add(section1);
            sections.add(section2);
        }
        config.setSections(sections);
        return config;
    }

    private void addCompoundDuplicates(FlowConfigSection section) {
        String defaultPath = "./ethalon_file/";
        Process process = getSql();
        process.setPathToFile(defaultPath);
        section.addProcess(new Compound()
                .toBuilder()
                .name("compound1")
                .processList(new LinkedList<Process>() {{
                    add(process);
                }}).build());
        section.addProcess(new Compound()
                .toBuilder()
                .name("compound2")
                .processList(new LinkedList<Process>() {{
                    add(process.clone()
                            .toBuilder()
                            .pathToFile(defaultPath + "folder")
                            .build());
                }}).build());
    }*/
}
