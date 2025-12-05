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

package org.qubership.atp.mia.service.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.model.configuration.CommonConfiguration;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ConfigurationReference;
import org.qubership.atp.mia.model.configuration.HeaderConfiguration;
import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.qubership.atp.mia.model.file.ProjectFile;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Input;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.impl.executable.Prerequisite;

import java.util.Arrays;
import java.util.HashMap;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

/**
 * Test to verify that ProjectConfiguration serialization works correctly with Hazelcast.
 * 
 * Key verification points:
 * 1. processRefs and compoundRefs ARE serialized (lightweight data in cache)
 * 2. processes and compounds are NOT serialized (transient, loaded on demand)
 * 3. sections ARE serialized with their refs
 */
public class HazelcastSerializationTest {
    
    // Use System.out for guaranteed console output in tests
    private static void log(String format, Object... args) {
        String message = format;
        for (Object arg : args) {
            message = message.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        System.out.println("[TEST] " + message);
    }

    private static HazelcastInstance hazelcastInstance;
    private static final String CACHE_NAME = "test-configuration-cache";

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.setInstanceName("test-hazelcast-instance");
        // Disable network for embedded testing
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        hazelcastInstance = Hazelcast.newHazelcastInstance(config);
        log("Hazelcast instance started for testing");
    }

    @AfterAll
    static void tearDown() {
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            log("Hazelcast instance stopped");
        }
    }

    @Test
    @DisplayName("Test Java Serialization of ProjectConfiguration - processes/compounds should be null after deserialization")
    void testJavaSerializationTransientFields() throws Exception {
        // Given: Create a ProjectConfiguration with FULL processes/compounds AND refs
        ProjectConfiguration config = createTestConfiguration();
        
        // Verify that we actually have full objects BEFORE serialization
        List<ProcessConfiguration> processesBefore = getProcessesDirectly(config);
        List<CompoundConfiguration> compoundsBefore = getCompoundsDirectly(config);
        
        log("=== BEFORE SERIALIZATION ===");
        log("ProjectId: {}", config.getProjectId());
        log("ProcessRefs count: {}", config.getProcessRefs().size());
        log("CompoundRefs count: {}", config.getCompoundRefs().size());
        log("Processes (FULL objects, transient): {} items", processesBefore != null ? processesBefore.size() : "null");
        log("Compounds (FULL objects, transient): {} items", compoundsBefore != null ? compoundsBefore.size() : "null");
        log("Sections count: {}", config.getSections().size());
        
        // Verify processes/compounds exist before serialization
        assertNotNull(processesBefore, "Processes should exist before serialization");
        assertNotNull(compoundsBefore, "Compounds should exist before serialization");
        assertEquals(3, processesBefore.size(), "Should have 3 full processes before serialization");
        assertEquals(2, compoundsBefore.size(), "Should have 2 full compounds before serialization");
        
        if (!config.getSections().isEmpty()) {
            SectionConfiguration section = config.getSections().get(0);
            log("Section[0] processRefs: {}", section.getProcessRefs().size());
            log("Section[0] compoundRefs: {}", section.getCompoundRefs().size());
            log("Section[0] processes (FULL, transient): {} items", section.getProcesses().size());
            log("Section[0] compounds (FULL, transient): {} items", section.getCompounds().size());
        }

        // When: Serialize and deserialize using Java ObjectOutputStream (same as Hazelcast)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(config);
        }
        
        byte[] serializedBytes = baos.toByteArray();
        log("Serialized size: {} bytes", serializedBytes.length);
        
        ProjectConfiguration deserializedConfig;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedBytes))) {
            deserializedConfig = (ProjectConfiguration) ois.readObject();
        }

        // Then: Verify using reflection to check actual field values
        List<ProcessConfiguration> processesAfter = getProcessesDirectly(deserializedConfig);
        List<CompoundConfiguration> compoundsAfter = getCompoundsDirectly(deserializedConfig);
        
        log("=== AFTER DESERIALIZATION ===");
        log("ProjectId: {}", deserializedConfig.getProjectId());
        log("ProcessRefs count: {}", deserializedConfig.getProcessRefs().size());
        log("CompoundRefs count: {}", deserializedConfig.getCompoundRefs().size());
        log("Processes (transient field): {}", processesAfter);
        log("Compounds (transient field): {}", compoundsAfter);
        
        // CRITICAL ASSERTIONS: transient fields should be NULL after deserialization!
        assertNull(processesAfter, "Processes field should be NULL after deserialization (transient worked!)");
        assertNull(compoundsAfter, "Compounds field should be NULL after deserialization (transient worked!)");
        
        // Non-transient fields should be preserved
        assertNotNull(deserializedConfig.getProjectId(), "ProjectId should be preserved");
        assertEquals(config.getProjectId(), deserializedConfig.getProjectId());
        
        // Refs should be preserved (they are NOT transient)
        assertEquals(3, deserializedConfig.getProcessRefs().size(), "ProcessRefs should be serialized");
        assertEquals(2, deserializedConfig.getCompoundRefs().size(), "CompoundRefs should be serialized");
        
        // Verify ref content
        log("ProcessRefs after deserialization (PRESERVED):");
        for (ConfigurationReference ref : deserializedConfig.getProcessRefs()) {
            log("  - ID: {}, Name: {}", ref.getId(), ref.getName());
        }
        
        log("CompoundRefs after deserialization (PRESERVED):");
        for (ConfigurationReference ref : deserializedConfig.getCompoundRefs()) {
            log("  - ID: {}, Name: {}", ref.getId(), ref.getName());
        }
        
        // Sections should be preserved with their refs, but NOT their processes/compounds
        assertFalse(deserializedConfig.getSections().isEmpty(), "Sections should be serialized");
        SectionConfiguration section = deserializedConfig.getSections().get(0);
        log("Section[0] after deserialization:");
        log("  - ID: {}, Name: {}", section.getId(), section.getName());
        log("  - ProcessRefs count (PRESERVED): {}", section.getProcessRefs().size());
        log("  - CompoundRefs count (PRESERVED): {}", section.getCompoundRefs().size());
        
        // Section refs should be preserved
        assertEquals(2, section.getProcessRefs().size(), "Section processRefs should be serialized");
        assertEquals(1, section.getCompoundRefs().size(), "Section compoundRefs should be serialized");
        
        log("=== TEST PASSED: transient fields (processes, compounds) were NOT serialized! ===");
        log("=== TEST PASSED: refs were serialized correctly! ===");
    }

    @Test
    @DisplayName("Test Hazelcast IMap caching of ProjectConfiguration")
    void testHazelcastMapCaching() {
        // Given
        ProjectConfiguration config = createTestConfiguration();
        UUID projectId = config.getProjectId();
        
        IMap<UUID, ProjectConfiguration> cache = hazelcastInstance.getMap(CACHE_NAME);
        
        log("=== PUTTING TO HAZELCAST CACHE ===");
        log("Configuration with {} processRefs, {} compoundRefs", 
                config.getProcessRefs().size(), config.getCompoundRefs().size());
        
        // When: Put to cache
        cache.put(projectId, config);
        
        // And: Get from cache
        ProjectConfiguration cachedConfig = cache.get(projectId);
        
        // Then
        log("=== RETRIEVED FROM HAZELCAST CACHE ===");
        assertNotNull(cachedConfig, "Should retrieve from cache");
        assertEquals(projectId, cachedConfig.getProjectId());
        
        // Refs should be present
        assertEquals(3, cachedConfig.getProcessRefs().size(), "ProcessRefs should be in cache");
        assertEquals(2, cachedConfig.getCompoundRefs().size(), "CompoundRefs should be in cache");
        
        log("ProcessRefs from cache:");
        cachedConfig.getProcessRefs().forEach(ref -> 
            log("  - {} : {}", ref.getId(), ref.getName()));
        
        log("CompoundRefs from cache:");
        cachedConfig.getCompoundRefs().forEach(ref -> 
            log("  - {} : {}", ref.getId(), ref.getName()));
        
        // Verify sections and their refs
        assertFalse(cachedConfig.getSections().isEmpty());
        SectionConfiguration section = cachedConfig.getSections().get(0);
        assertEquals(2, section.getProcessRefs().size(), "Section processRefs should be in cache");
        assertEquals(1, section.getCompoundRefs().size(), "Section compoundRefs should be in cache");
        
        log("Section '{}' from cache:", section.getName());
        log("  ProcessRefs: {}", section.getProcessRefs().size());
        log("  CompoundRefs: {}", section.getCompoundRefs().size());
        
        // Cleanup
        cache.remove(projectId);
        
        log("=== HAZELCAST CACHING TEST PASSED! ===");
    }

    @Test
    @DisplayName("Compare serialized size with and without refs optimization")
    void testSerializedSizeComparison() throws Exception {
        // Create configuration with refs only (optimized)
        ProjectConfiguration optimizedConfig = createTestConfiguration();
        
        // Simulate old approach: set full processes/compounds (not transient)
        // We can't really do this since fields are transient, but we can measure refs
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(optimizedConfig);
        }
        
        int optimizedSize = baos.toByteArray().length;
        log("=== SIZE COMPARISON ===");
        log("Optimized configuration size (with refs, without full objects): {} bytes", optimizedSize);
        log("This is the size that goes to Hazelcast cache");
        
        // For comparison, let's see what a single ProcessConfiguration would add
        ProcessConfiguration singleProcess = createFullProcess("LargeProcess", UUID.randomUUID());
        ByteArrayOutputStream processBytes = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(processBytes)) {
            oos.writeObject(singleProcess);
        }
        log("Single ProcessConfiguration size: {} bytes", processBytes.toByteArray().length);
        log("With 100 processes, we would save approximately: {} KB", 
                (processBytes.toByteArray().length * 100) / 1024);
        
        log("=== SIZE COMPARISON COMPLETE ===");
    }

    /**
     * Create a test ProjectConfiguration with:
     * - refs populated (lightweight, will be serialized)
     * - full processes and compounds populated (transient, should NOT be serialized)
     */
    private ProjectConfiguration createTestConfiguration() {
        UUID projectId = UUID.randomUUID();
        
        // Create FULL processes (should be filtered out by transient)
        List<ProcessConfiguration> processes = new ArrayList<>();
        UUID processId1 = UUID.randomUUID();
        UUID processId2 = UUID.randomUUID();
        UUID processId3 = UUID.randomUUID();
        processes.add(createFullProcess("Process_SSH_BG", processId1));
        processes.add(createFullProcess("Process_SQL_Query", processId2));
        processes.add(createFullProcess("Process_REST_Call", processId3));
        
        // Create FULL compounds (should be filtered out by transient)
        List<CompoundConfiguration> compounds = new ArrayList<>();
        UUID compoundId1 = UUID.randomUUID();
        UUID compoundId2 = UUID.randomUUID();
        compounds.add(createFullCompound("Compound_Billing", compoundId1));
        compounds.add(createFullCompound("Compound_Payment", compoundId2));
        
        // Create refs for processes (lightweight, will be serialized)
        List<ConfigurationReference> processRefs = new ArrayList<>();
        processRefs.add(new ConfigurationReference(processId1, "Process_SSH_BG"));
        processRefs.add(new ConfigurationReference(processId2, "Process_SQL_Query"));
        processRefs.add(new ConfigurationReference(processId3, "Process_REST_Call"));
        
        // Create refs for compounds (lightweight, will be serialized)
        List<ConfigurationReference> compoundRefs = new ArrayList<>();
        compoundRefs.add(new ConfigurationReference(compoundId1, "Compound_Billing"));
        compoundRefs.add(new ConfigurationReference(compoundId2, "Compound_Payment"));
        
        // Create section with its own processes/compounds
        SectionConfiguration section = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("TestSection")
                .place(0)
                .sections(new ArrayList<>())
                .build();
        
        // Set section's FULL processes and compounds (transient, should NOT be serialized)
        UUID sectionProcessId1 = UUID.randomUUID();
        UUID sectionProcessId2 = UUID.randomUUID();
        UUID sectionCompoundId1 = UUID.randomUUID();
        section.getProcesses().add(createFullProcess("Section_Process_1", sectionProcessId1));
        section.getProcesses().add(createFullProcess("Section_Process_2", sectionProcessId2));
        section.getCompounds().add(createFullCompound("Section_Compound_1", sectionCompoundId1));
        
        // Set section refs (lightweight, will be serialized)
        section.getProcessRefs().add(new ConfigurationReference(sectionProcessId1, "Section_Process_1"));
        section.getProcessRefs().add(new ConfigurationReference(sectionProcessId2, "Section_Process_2"));
        section.getCompoundRefs().add(new ConfigurationReference(sectionCompoundId1, "Section_Compound_1"));
        
        List<SectionConfiguration> sections = new ArrayList<>();
        sections.add(section);
        
        // Build configuration
        ProjectConfiguration config = ProjectConfiguration.builder()
                .projectId(projectId)
                .projectName("Test Project")
                .sections(sections)
                .directories(new ArrayList<>())
                .files(new ArrayList<>())
                .build();
        
        // Set refs (lightweight)
        config.getProcessRefs().addAll(processRefs);
        config.getCompoundRefs().addAll(compoundRefs);
        
        // Set FULL processes and compounds (these should be filtered out by transient!)
        // We need to use reflection or direct field access since getter may create new ArrayList
        setProcessesDirectly(config, processes);
        setCompoundsDirectly(config, compounds);
        
        log("Created test configuration:");
        log("  - Processes count (full objects): {}", processes.size());
        log("  - Compounds count (full objects): {}", compounds.size());
        log("  - ProcessRefs count (lightweight): {}", processRefs.size());
        log("  - CompoundRefs count (lightweight): {}", compoundRefs.size());
        
        return config;
    }
    
    /**
     * Set processes directly using reflection to bypass getter logic.
     */
    private void setProcessesDirectly(ProjectConfiguration config, List<ProcessConfiguration> processes) {
        try {
            java.lang.reflect.Field field = ProjectConfiguration.class.getDeclaredField("processes");
            field.setAccessible(true);
            field.set(config, processes);
        } catch (Exception e) {
            log("Could not set processes via reflection: {}", e.getMessage());
        }
    }
    
    /**
     * Set compounds directly using reflection to bypass getter logic.
     */
    private void setCompoundsDirectly(ProjectConfiguration config, List<CompoundConfiguration> compounds) {
        try {
            java.lang.reflect.Field field = ProjectConfiguration.class.getDeclaredField("compounds");
            field.setAccessible(true);
            field.set(config, compounds);
        } catch (Exception e) {
            log("Could not set compounds via reflection: {}", e.getMessage());
        }
    }
    
    /**
     * Get processes directly using reflection to check actual field value (not getter).
     */
    private List<ProcessConfiguration> getProcessesDirectly(ProjectConfiguration config) {
        try {
            java.lang.reflect.Field field = ProjectConfiguration.class.getDeclaredField("processes");
            field.setAccessible(true);
            return (List<ProcessConfiguration>) field.get(config);
        } catch (Exception e) {
            log("Could not get processes via reflection: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get compounds directly using reflection to check actual field value (not getter).
     */
    private List<CompoundConfiguration> getCompoundsDirectly(ProjectConfiguration config) {
        try {
            java.lang.reflect.Field field = ProjectConfiguration.class.getDeclaredField("compounds");
            field.setAccessible(true);
            return (List<CompoundConfiguration>) field.get(config);
        } catch (Exception e) {
            log("Could not get compounds via reflection: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create a full ProcessConfiguration with some data.
     */
    private ProcessConfiguration createFullProcess(String name, UUID id) {
        return ProcessConfiguration.builder()
                .id(id)
                .name(name)
                .sourceId(UUID.randomUUID())
                .build();
    }
    
    /**
     * Create a full CompoundConfiguration with some data.
     */
    private CompoundConfiguration createFullCompound(String name, UUID id) {
        return CompoundConfiguration.builder()
                .id(id)
                .name(name)
                .sourceId(UUID.randomUUID())
                .build();
    }

    // ==================== BACK-REFERENCE RESTORATION TESTS ====================

    @Test
    @DisplayName("Test back-references are null after deserialization (transient fields)")
    void testBackReferencesNullAfterDeserialization() throws Exception {
        // Given: Create configuration with all child objects and back-references set
        ProjectConfiguration config = createConfigurationWithBackReferences();
        
        log("=== BEFORE SERIALIZATION ===");
        verifyBackReferencesSet(config, "before serialization");
        
        // When: Serialize and deserialize (simulating Hazelcast cache)
        byte[] serialized = serializeObject(config);
        ProjectConfiguration deserialized = deserializeObject(serialized);
        
        log("=== AFTER DESERIALIZATION ===");
        
        // Then: Back-references should be NULL (they are transient)
        assertNotNull(deserialized, "Deserialized config should not be null");
        assertNotNull(deserialized.getSections(), "Sections should be deserialized");
        assertFalse(deserialized.getSections().isEmpty(), "Sections should not be empty");
        
        // Verify CommonConfiguration back-reference is null
        assertNotNull(deserialized.getCommonConfiguration(), "CommonConfiguration should be deserialized");
        assertNull(getBackReference(deserialized.getCommonConfiguration(), "projectConfiguration"),
                "CommonConfiguration.projectConfiguration should be null after deserialization");
        
        // Verify HeaderConfiguration back-reference is null
        assertNotNull(deserialized.getHeaderConfiguration(), "HeaderConfiguration should be deserialized");
        assertNull(getBackReference(deserialized.getHeaderConfiguration(), "projectConfiguration"),
                "HeaderConfiguration.projectConfiguration should be null after deserialization");
        
        // Verify Section back-references are null
        SectionConfiguration section = deserialized.getSections().get(0);
        assertNull(getBackReference(section, "projectConfiguration"),
                "SectionConfiguration.projectConfiguration should be null after deserialization");
        assertNull(getBackReference(section, "parentSection"),
                "SectionConfiguration.parentSection should be null (root section)");
        
        // Verify nested section parent is null
        if (!section.getSections().isEmpty()) {
            SectionConfiguration childSection = section.getSections().get(0);
            assertNull(getBackReference(childSection, "parentSection"),
                    "Child section parentSection should be null after deserialization");
        }
        
        // Verify lazyLoader is null
        assertNull(getBackReference(deserialized, "lazyLoader"),
                "ProjectConfiguration.lazyLoader should be null after deserialization");
        assertNull(getBackReference(section, "lazyLoader"),
                "SectionConfiguration.lazyLoader should be null after deserialization");
        
        log("All back-references are correctly null after deserialization (transient)");
    }

    @Test
    @DisplayName("Test back-references are restored after calling restoreBackReferences simulation")
    void testBackReferencesRestoredCorrectly() throws Exception {
        // Given: Create configuration, serialize, deserialize
        ProjectConfiguration config = createConfigurationWithBackReferences();
        byte[] serialized = serializeObject(config);
        ProjectConfiguration deserialized = deserializeObject(serialized);
        
        log("=== SIMULATING restoreBackReferences ===");
        
        // When: Simulate restoreBackReferences logic
        simulateRestoreBackReferences(deserialized);
        
        // Then: All back-references should be restored
        log("=== AFTER RESTORATION ===");
        
        // Verify CommonConfiguration back-reference
        assertNotNull(deserialized.getCommonConfiguration().getProjectConfiguration(),
                "CommonConfiguration.projectConfiguration should be restored");
        assertEquals(deserialized, deserialized.getCommonConfiguration().getProjectConfiguration(),
                "CommonConfiguration should reference parent ProjectConfiguration");
        
        // Verify HeaderConfiguration back-reference
        assertNotNull(deserialized.getHeaderConfiguration().getProjectConfiguration(),
                "HeaderConfiguration.projectConfiguration should be restored");
        assertEquals(deserialized, deserialized.getHeaderConfiguration().getProjectConfiguration(),
                "HeaderConfiguration should reference parent ProjectConfiguration");
        
        // Verify Section back-references
        SectionConfiguration section = deserialized.getSections().get(0);
        assertNotNull(section.getProjectConfiguration(),
                "Section.projectConfiguration should be restored");
        assertEquals(deserialized, section.getProjectConfiguration(),
                "Section should reference parent ProjectConfiguration");
        
        // Verify nested section parent is restored
        if (!section.getSections().isEmpty()) {
            SectionConfiguration childSection = section.getSections().get(0);
            assertNotNull(childSection.getParentSection(),
                    "Child section parentSection should be restored");
            assertEquals(section, childSection.getParentSection(),
                    "Child section should reference parent section");
            assertNotNull(childSection.getProjectConfiguration(),
                    "Child section projectConfiguration should be restored");
        }
        
        // Verify directory back-references
        if (!deserialized.getDirectories().isEmpty()) {
            ProjectDirectory dir = deserialized.getDirectories().get(0);
            assertNotNull(dir.getProjectConfiguration(),
                    "Directory.projectConfiguration should be restored");
            assertEquals(deserialized, dir.getProjectConfiguration(),
                    "Directory should reference parent ProjectConfiguration");
        }
        
        log("All back-references successfully restored!");
        verifyBackReferencesSet(deserialized, "after restoration");
    }

    /**
     * Create a full configuration with all child objects and back-references.
     */
    private ProjectConfiguration createConfigurationWithBackReferences() {
        UUID projectId = UUID.randomUUID();
        
        // Create CommonConfiguration
        CommonConfiguration commonConfig = CommonConfiguration.builder()
                .projectId(projectId)
                .build();
        
        // Create HeaderConfiguration
        HeaderConfiguration headerConfig = HeaderConfiguration.builder()
                .projectId(projectId)
                .build();
        
        // Create PotHeaderConfiguration
        PotHeaderConfiguration potHeaderConfig = PotHeaderConfiguration.builder()
                .projectId(projectId)
                .build();
        
        // Create nested sections
        SectionConfiguration childSection = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("ChildSection")
                .place(0)
                .sections(new ArrayList<>())
                .build();
        
        SectionConfiguration parentSection = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("ParentSection")
                .place(0)
                .sections(new ArrayList<>())
                .build();
        parentSection.getSections().add(childSection);
        
        // Create directory
        ProjectDirectory directory = ProjectDirectory.builder()
                .id(UUID.randomUUID())
                .name("TestDir")
                .directories(new ArrayList<>())
                .files(new ArrayList<>())
                .build();
        
        // Create file
        ProjectFile file = ProjectFile.builder()
                .id(UUID.randomUUID())
                .name("TestFile")
                .build();
        
        List<SectionConfiguration> sections = new ArrayList<>();
        sections.add(parentSection);
        
        List<ProjectDirectory> directories = new ArrayList<>();
        directories.add(directory);
        
        List<ProjectFile> files = new ArrayList<>();
        files.add(file);
        
        // Build project configuration
        ProjectConfiguration config = ProjectConfiguration.builder()
                .projectId(projectId)
                .projectName("BackRefTest Project")
                .commonConfiguration(commonConfig)
                .headerConfiguration(headerConfig)
                .potHeaderConfiguration(potHeaderConfig)
                .sections(sections)
                .directories(directories)
                .files(files)
                .build();
        
        // Set all back-references (simulating what Hibernate would do)
        commonConfig.setProjectConfiguration(config);
        headerConfig.setProjectConfiguration(config);
        potHeaderConfig.setProjectConfiguration(config);
        parentSection.setProjectConfiguration(config);
        childSection.setProjectConfiguration(config);
        childSection.setParentSection(parentSection);
        directory.setProjectConfiguration(config);
        file.setProjectConfiguration(config);
        
        return config;
    }

    /**
     * Simulate the restoreBackReferences logic from ProjectConfigurationService.
     */
    private void simulateRestoreBackReferences(ProjectConfiguration config) {
        if (config == null) {
            return;
        }
        
        // Restore back-references on child configurations
        if (config.getCommonConfiguration() != null) {
            config.getCommonConfiguration().setProjectConfiguration(config);
        }
        if (config.getHeaderConfiguration() != null) {
            config.getHeaderConfiguration().setProjectConfiguration(config);
        }
        if (config.getPotHeaderConfiguration() != null) {
            config.getPotHeaderConfiguration().setProjectConfiguration(config);
        }
        
        // Restore section back-references
        if (config.getSections() != null) {
            for (SectionConfiguration section : config.getSections()) {
                restoreSectionBackRefs(section, config, null);
            }
        }
        
        // Restore directory back-references
        if (config.getDirectories() != null) {
            for (ProjectDirectory dir : config.getDirectories()) {
                restoreDirectoryBackRefs(dir, config, null);
            }
        }
        
        // Restore file back-references
        if (config.getFiles() != null) {
            for (ProjectFile file : config.getFiles()) {
                file.setProjectConfiguration(config);
            }
        }
    }

    private void restoreSectionBackRefs(SectionConfiguration section, 
                                         ProjectConfiguration projectConfig,
                                         SectionConfiguration parentSection) {
        if (section == null) {
            return;
        }
        section.setProjectConfiguration(projectConfig);
        section.setParentSection(parentSection);
        
        if (section.getSections() != null) {
            for (SectionConfiguration child : section.getSections()) {
                restoreSectionBackRefs(child, projectConfig, section);
            }
        }
    }

    private void restoreDirectoryBackRefs(ProjectDirectory directory,
                                           ProjectConfiguration projectConfig,
                                           ProjectDirectory parentDirectory) {
        if (directory == null) {
            return;
        }
        directory.setProjectConfiguration(projectConfig);
        directory.setParentDirectory(parentDirectory);
        
        if (directory.getDirectories() != null) {
            for (ProjectDirectory child : directory.getDirectories()) {
                restoreDirectoryBackRefs(child, projectConfig, directory);
            }
        }
        if (directory.getFiles() != null) {
            for (ProjectFile file : directory.getFiles()) {
                file.setProjectConfiguration(projectConfig);
                file.setDirectory(directory);
            }
        }
    }

    /**
     * Get back-reference field value via reflection.
     */
    private Object getBackReference(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            log("Could not get field {} via reflection: {}", fieldName, e.getMessage());
        }
        return null;
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Verify that back-references are set (for logging).
     */
    private void verifyBackReferencesSet(ProjectConfiguration config, String phase) {
        log("Verifying back-references {}:", phase);
        
        if (config.getCommonConfiguration() != null) {
            Object ref = getBackReference(config.getCommonConfiguration(), "projectConfiguration");
            log("  CommonConfiguration.projectConfiguration: {}", ref != null ? "SET" : "NULL");
        }
        if (config.getHeaderConfiguration() != null) {
            Object ref = getBackReference(config.getHeaderConfiguration(), "projectConfiguration");
            log("  HeaderConfiguration.projectConfiguration: {}", ref != null ? "SET" : "NULL");
        }
        if (!config.getSections().isEmpty()) {
            SectionConfiguration section = config.getSections().get(0);
            Object ref = getBackReference(section, "projectConfiguration");
            log("  Section.projectConfiguration: {}", ref != null ? "SET" : "NULL");
            
            if (!section.getSections().isEmpty()) {
                SectionConfiguration child = section.getSections().get(0);
                Object parentRef = getBackReference(child, "parentSection");
                log("  ChildSection.parentSection: {}", parentRef != null ? "SET" : "NULL");
            }
        }
    }

    /**
     * Serialize object to byte array.
     */
    private byte[] serializeObject(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return baos.toByteArray();
    }

    /**
     * Deserialize object from byte array.
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializeObject(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        T result = (T) ois.readObject();
        ois.close();
        return result;
    }

    // ==================== COMPREHENSIVE FIELD VERIFICATION TESTS ====================

    @Test
    @DisplayName("Test ProcessConfiguration all fields preserved after serialization")
    void testProcessConfigurationFieldsPreserved() throws Exception {
        log("=== TEST: ProcessConfiguration Fields Preservation ===");
        
        // Given: Create a ProcessConfiguration with ALL fields populated
        ProcessConfiguration original = createFullProcessConfiguration();
        
        log("ORIGINAL ProcessConfiguration:");
        logProcessConfigurationFields(original);
        
        // When: Serialize and deserialize
        byte[] serialized = serializeObject(original);
        ProcessConfiguration restored = deserializeObject(serialized);
        
        log("RESTORED ProcessConfiguration:");
        logProcessConfigurationFields(restored);
        
        // Then: Verify all fields match
        log("=== VERIFICATION ===");
        
        // Basic fields
        assertEquals(original.getId(), restored.getId(), "ID should match");
        assertEquals(original.getSourceId(), restored.getSourceId(), "SourceId should match");
        assertEquals(original.getName(), restored.getName(), "Name should match");
        assertEquals(original.getPathToFile(), restored.getPathToFile(), "PathToFile should match");
        
        // ProcessSettings (important - JSON field)
        assertNotNull(restored.getProcessSettings(), "ProcessSettings should not be null");
        ProcessSettings origSettings = original.getProcessSettings();
        ProcessSettings restoredSettings = restored.getProcessSettings();
        
        assertEquals(origSettings.getName(), restoredSettings.getName(), 
                "ProcessSettings.name should match");
        assertEquals(origSettings.getReferToInput(), restoredSettings.getReferToInput(), 
                "ProcessSettings.referToInput should match");
        
        // ProcessSettings.command
        assertNotNull(restoredSettings.getCommand(), "Command should not be null");
        assertEquals(origSettings.getCommand().getName(), restoredSettings.getCommand().getName(),
                "Command.name should match");
        assertEquals(origSettings.getCommand().getValue(), restoredSettings.getCommand().getValue(),
                "Command.value should match");
        
        // ProcessSettings.inputs
        assertNotNull(restoredSettings.getInputs(), "Inputs should not be null");
        assertEquals(origSettings.getInputs().size(), restoredSettings.getInputs().size(),
                "Inputs size should match");
        for (int i = 0; i < origSettings.getInputs().size(); i++) {
            assertEquals(origSettings.getInputs().get(i).getName(), 
                    restoredSettings.getInputs().get(i).getName(),
                    "Input[" + i + "].name should match");
            assertEquals(origSettings.getInputs().get(i).getValue(), 
                    restoredSettings.getInputs().get(i).getValue(),
                    "Input[" + i + "].value should match");
            assertEquals(origSettings.getInputs().get(i).getLabel(), 
                    restoredSettings.getInputs().get(i).getLabel(),
                    "Input[" + i + "].label should match");
        }
        
        // ProcessSettings.validations
        assertNotNull(restoredSettings.getValidations(), "Validations should not be null");
        assertEquals(origSettings.getValidations().size(), restoredSettings.getValidations().size(),
                "Validations size should match");
        
        // ProcessSettings.prerequisites
        assertNotNull(restoredSettings.getPrerequisites(), "Prerequisites should not be null");
        assertEquals(origSettings.getPrerequisites().size(), restoredSettings.getPrerequisites().size(),
                "Prerequisites size should match");
        
        // ProcessSettings.globalVariables
        assertNotNull(restoredSettings.getGlobalVariables(), "GlobalVariables should not be null");
        assertEquals(origSettings.getGlobalVariables(), restoredSettings.getGlobalVariables(),
                "GlobalVariables should match");
        
        // String lists (compounds, sections names)
        assertEquals(original.getCompounds(), restored.getCompounds(), 
                "Compounds list should match");
        assertEquals(original.getSections(), restored.getSections(), 
                "Sections list should match");
        
        // Back-reference should be null (transient)
        assertNull(getBackReference(restored, "projectConfiguration"),
                "projectConfiguration should be null (transient)");
        
        log("✓ All ProcessConfiguration fields verified successfully!");
    }

    @Test
    @DisplayName("Test CompoundConfiguration all fields preserved after serialization")
    void testCompoundConfigurationFieldsPreserved() throws Exception {
        log("=== TEST: CompoundConfiguration Fields Preservation ===");
        
        // Given: Create a CompoundConfiguration with ALL fields populated
        CompoundConfiguration original = createFullCompoundConfiguration();
        
        log("ORIGINAL CompoundConfiguration:");
        logCompoundConfigurationFields(original);
        
        // When: Serialize and deserialize
        byte[] serialized = serializeObject(original);
        CompoundConfiguration restored = deserializeObject(serialized);
        
        log("RESTORED CompoundConfiguration:");
        logCompoundConfigurationFields(restored);
        
        // Then: Verify all fields match
        log("=== VERIFICATION ===");
        
        assertEquals(original.getId(), restored.getId(), "ID should match");
        assertEquals(original.getSourceId(), restored.getSourceId(), "SourceId should match");
        assertEquals(original.getName(), restored.getName(), "Name should match");
        assertEquals(original.getReferToInput(), restored.getReferToInput(), "ReferToInput should match");
        
        // Back-reference should be null (transient)
        assertNull(getBackReference(restored, "projectConfiguration"),
                "projectConfiguration should be null (transient)");
        
        log("✓ All CompoundConfiguration fields verified successfully!");
    }

    @Test
    @DisplayName("Test SectionConfiguration all fields preserved after serialization")
    void testSectionConfigurationFieldsPreserved() throws Exception {
        log("=== TEST: SectionConfiguration Fields Preservation ===");
        
        // Given: Create nested sections
        SectionConfiguration childSection = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("ChildSection")
                .place(0)
                .sections(new ArrayList<>())
                .build();
        childSection.getProcessRefs().add(new ConfigurationReference(UUID.randomUUID(), "ChildProcess1"));
        childSection.getCompoundRefs().add(new ConfigurationReference(UUID.randomUUID(), "ChildCompound1"));
        
        SectionConfiguration original = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .sourceId(UUID.randomUUID())
                .name("ParentSection")
                .place(1)
                .sections(new ArrayList<>())
                .build();
        original.getSections().add(childSection);
        original.getProcessRefs().add(new ConfigurationReference(UUID.randomUUID(), "Process1"));
        original.getProcessRefs().add(new ConfigurationReference(UUID.randomUUID(), "Process2"));
        original.getCompoundRefs().add(new ConfigurationReference(UUID.randomUUID(), "Compound1"));
        
        // Set back-references (will be null after deserialization)
        childSection.setParentSection(original);
        
        log("ORIGINAL SectionConfiguration:");
        logSectionConfigurationFields(original);
        
        // When: Serialize and deserialize
        byte[] serialized = serializeObject(original);
        SectionConfiguration restored = deserializeObject(serialized);
        
        log("RESTORED SectionConfiguration:");
        logSectionConfigurationFields(restored);
        
        // Then: Verify all fields match
        log("=== VERIFICATION ===");
        
        assertEquals(original.getId(), restored.getId(), "ID should match");
        assertEquals(original.getSourceId(), restored.getSourceId(), "SourceId should match");
        assertEquals(original.getName(), restored.getName(), "Name should match");
        assertEquals(original.getPlace(), restored.getPlace(), "Place should match");
        
        // ProcessRefs should be preserved
        assertEquals(original.getProcessRefs().size(), restored.getProcessRefs().size(),
                "ProcessRefs size should match");
        for (int i = 0; i < original.getProcessRefs().size(); i++) {
            assertEquals(original.getProcessRefs().get(i).getId(), 
                    restored.getProcessRefs().get(i).getId(),
                    "ProcessRefs[" + i + "].id should match");
            assertEquals(original.getProcessRefs().get(i).getName(), 
                    restored.getProcessRefs().get(i).getName(),
                    "ProcessRefs[" + i + "].name should match");
        }
        
        // CompoundRefs should be preserved
        assertEquals(original.getCompoundRefs().size(), restored.getCompoundRefs().size(),
                "CompoundRefs size should match");
        
        // Nested sections should be preserved
        assertEquals(original.getSections().size(), restored.getSections().size(),
                "Nested sections size should match");
        SectionConfiguration restoredChild = restored.getSections().get(0);
        assertEquals(childSection.getId(), restoredChild.getId(), "Child section ID should match");
        assertEquals(childSection.getName(), restoredChild.getName(), "Child section name should match");
        
        // Back-references should be null (transient)
        assertNull(getBackReference(restored, "projectConfiguration"),
                "projectConfiguration should be null (transient)");
        assertNull(getBackReference(restored, "parentSection"),
                "parentSection should be null (root section)");
        assertNull(getBackReference(restoredChild, "parentSection"),
                "Child parentSection should be null after deserialization (transient)");
        
        log("✓ All SectionConfiguration fields verified successfully!");
    }

    @Test
    @DisplayName("Test full ProjectConfiguration with all nested objects preserved")
    void testFullProjectConfigurationPreserved() throws Exception {
        log("=== TEST: Full ProjectConfiguration Preservation ===");
        
        // Given: Create a complete ProjectConfiguration
        ProjectConfiguration original = createCompleteProjectConfiguration();
        
        log("ORIGINAL ProjectConfiguration:");
        log("  ProjectId: {}", original.getProjectId());
        log("  ProjectName: {}", original.getProjectName());
        log("  Sections count: {}", original.getSections().size());
        log("  ProcessRefs count: {}", original.getProcessRefs().size());
        log("  CompoundRefs count: {}", original.getCompoundRefs().size());
        
        // When: Serialize and deserialize
        byte[] serialized = serializeObject(original);
        log("Serialized size: {} bytes", serialized.length);
        
        ProjectConfiguration restored = deserializeObject(serialized);
        
        log("RESTORED ProjectConfiguration:");
        log("  ProjectId: {}", restored.getProjectId());
        log("  ProjectName: {}", restored.getProjectName());
        log("  Sections count: {}", restored.getSections() != null ? restored.getSections().size() : "null");
        log("  ProcessRefs count: {}", restored.getProcessRefs().size());
        log("  CompoundRefs count: {}", restored.getCompoundRefs().size());
        
        // Then: Verify all fields match
        log("=== VERIFICATION ===");
        
        // Basic fields
        assertEquals(original.getProjectId(), restored.getProjectId(), "ProjectId should match");
        assertEquals(original.getProjectName(), restored.getProjectName(), "ProjectName should match");
        
        // Refs should be preserved
        assertEquals(original.getProcessRefs().size(), restored.getProcessRefs().size(),
                "ProcessRefs size should match");
        assertEquals(original.getCompoundRefs().size(), restored.getCompoundRefs().size(),
                "CompoundRefs size should match");
        
        // Verify each ref
        for (int i = 0; i < original.getProcessRefs().size(); i++) {
            ConfigurationReference origRef = original.getProcessRefs().get(i);
            ConfigurationReference restoredRef = restored.getProcessRefs().get(i);
            assertEquals(origRef.getId(), restoredRef.getId(), "ProcessRef[" + i + "].id should match");
            assertEquals(origRef.getName(), restoredRef.getName(), "ProcessRef[" + i + "].name should match");
        }
        
        // Sections should be preserved
        assertNotNull(restored.getSections(), "Sections should not be null");
        assertEquals(original.getSections().size(), restored.getSections().size(),
                "Sections size should match");
        
        // Verify section fields
        for (int i = 0; i < original.getSections().size(); i++) {
            SectionConfiguration origSection = original.getSections().get(i);
            SectionConfiguration restoredSection = restored.getSections().get(i);
            assertEquals(origSection.getId(), restoredSection.getId(), 
                    "Section[" + i + "].id should match");
            assertEquals(origSection.getName(), restoredSection.getName(), 
                    "Section[" + i + "].name should match");
            assertEquals(origSection.getProcessRefs().size(), restoredSection.getProcessRefs().size(),
                    "Section[" + i + "].processRefs size should match");
        }
        
        // CommonConfiguration should be preserved
        assertNotNull(restored.getCommonConfiguration(), "CommonConfiguration should not be null");
        assertEquals(original.getCommonConfiguration().getProjectId(), 
                restored.getCommonConfiguration().getProjectId(),
                "CommonConfiguration.projectId should match");
        
        // HeaderConfiguration should be preserved
        assertNotNull(restored.getHeaderConfiguration(), "HeaderConfiguration should not be null");
        
        // Transient fields should be null
        assertNull(getBackReference(restored, "lazyLoader"), "lazyLoader should be null (transient)");
        assertNull(getBackReference(restored, "processes"), "processes should be null (transient)");
        assertNull(getBackReference(restored, "compounds"), "compounds should be null (transient)");
        
        log("✓ Full ProjectConfiguration verified successfully!");
    }

    // ==================== HELPER METHODS FOR COMPREHENSIVE TESTS ====================

    /**
     * Create a ProcessConfiguration with all fields populated.
     */
    private ProcessConfiguration createFullProcessConfiguration() {
        UUID processId = UUID.randomUUID();
        
        // Create Command
        Command command = Command.builder()
                .name("REST_COMMAND")
                .value("POST /api/v1/test")
                .build();
        
        // Create Inputs (label is @Nonnull required field)
        List<Input> inputs = new ArrayList<>();
        Input input1 = Input.builder().name("input1").value("value1").label("Label1").build();
        Input input2 = Input.builder().name("input2").value("value2").label("Label2").build();
        inputs.add(input1);
        inputs.add(input2);
        
        // Create Validations
        List<Validation> validations = new ArrayList<>();
        Validation validation = Validation.builder()
                .name("StatusValidation")
                .value("200")
                .build();
        validations.add(validation);
        
        // Create Prerequisites
        List<Prerequisite> prerequisites = new ArrayList<>();
        Prerequisite prereq = Prerequisite.builder()
                .name("AuthPrerequisite")
                .value("token123")
                .build();
        prerequisites.add(prereq);
        
        // Create GlobalVariables
        HashMap<String, String> globalVars = new HashMap<>();
        globalVars.put("ENV", "TEST");
        globalVars.put("VERSION", "1.0");
        
        // Create ProcessSettings
        ProcessSettings processSettings = ProcessSettings.builder()
                .name("TestProcessSettings")
                .command(command)
                .inputs(inputs)
                .validations(validations)
                .prerequisites(prerequisites)
                .globalVariables(globalVars)
                .referToInput("input1")
                .build();
        
        // Create ProcessConfiguration
        ProcessConfiguration process = ProcessConfiguration.builder()
                .id(processId)
                .sourceId(UUID.randomUUID())
                .name("FullTestProcess")
                .pathToFile("/path/to/process.json")
                .processSettings(processSettings)
                .build();
        
        // Set string lists
        process.setCompounds(Arrays.asList("Compound1", "Compound2"));
        process.setSections(Arrays.asList("Section1", "Section2"));
        
        return process;
    }

    /**
     * Create a CompoundConfiguration with all fields populated.
     */
    private CompoundConfiguration createFullCompoundConfiguration() {
        return CompoundConfiguration.builder()
                .id(UUID.randomUUID())
                .sourceId(UUID.randomUUID())
                .name("FullTestCompound")
                .referToInput("mainInput")
                .build();
    }

    /**
     * Create a complete ProjectConfiguration with all nested objects.
     */
    private ProjectConfiguration createCompleteProjectConfiguration() {
        UUID projectId = UUID.randomUUID();
        
        // Create sections with refs
        SectionConfiguration section1 = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Section1")
                .place(0)
                .sections(new ArrayList<>())
                .build();
        section1.getProcessRefs().add(new ConfigurationReference(UUID.randomUUID(), "Proc1"));
        section1.getProcessRefs().add(new ConfigurationReference(UUID.randomUUID(), "Proc2"));
        section1.getCompoundRefs().add(new ConfigurationReference(UUID.randomUUID(), "Comp1"));
        
        SectionConfiguration section2 = SectionConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Section2")
                .place(1)
                .sections(new ArrayList<>())
                .build();
        section2.getProcessRefs().add(new ConfigurationReference(UUID.randomUUID(), "Proc3"));
        
        List<SectionConfiguration> sections = new ArrayList<>();
        sections.add(section1);
        sections.add(section2);
        
        // Create configurations
        CommonConfiguration commonConfig = CommonConfiguration.builder()
                .projectId(projectId)
                .build();
        
        HeaderConfiguration headerConfig = HeaderConfiguration.builder()
                .projectId(projectId)
                .build();
        
        PotHeaderConfiguration potHeaderConfig = PotHeaderConfiguration.builder()
                .projectId(projectId)
                .build();
        
        // Create project refs
        List<ConfigurationReference> processRefs = new ArrayList<>();
        processRefs.add(new ConfigurationReference(UUID.randomUUID(), "GlobalProcess1"));
        processRefs.add(new ConfigurationReference(UUID.randomUUID(), "GlobalProcess2"));
        processRefs.add(new ConfigurationReference(UUID.randomUUID(), "GlobalProcess3"));
        
        List<ConfigurationReference> compoundRefs = new ArrayList<>();
        compoundRefs.add(new ConfigurationReference(UUID.randomUUID(), "GlobalCompound1"));
        compoundRefs.add(new ConfigurationReference(UUID.randomUUID(), "GlobalCompound2"));
        
        // Build complete configuration
        ProjectConfiguration config = ProjectConfiguration.builder()
                .projectId(projectId)
                .projectName("CompleteTestProject")
                .sections(sections)
                .commonConfiguration(commonConfig)
                .headerConfiguration(headerConfig)
                .potHeaderConfiguration(potHeaderConfig)
                .directories(new ArrayList<>())
                .files(new ArrayList<>())
                .build();
        
        config.getProcessRefs().addAll(processRefs);
        config.getCompoundRefs().addAll(compoundRefs);
        
        // Set back-references (these will be null after deserialization)
        commonConfig.setProjectConfiguration(config);
        headerConfig.setProjectConfiguration(config);
        potHeaderConfig.setProjectConfiguration(config);
        section1.setProjectConfiguration(config);
        section2.setProjectConfiguration(config);
        
        return config;
    }

    private void logProcessConfigurationFields(ProcessConfiguration p) {
        log("  ID: {}", p.getId());
        log("  SourceId: {}", p.getSourceId());
        log("  Name: {}", p.getName());
        log("  PathToFile: {}", p.getPathToFile());
        if (p.getProcessSettings() != null) {
            log("  ProcessSettings.name: {}", p.getProcessSettings().getName());
            log("  ProcessSettings.command: {}", 
                    p.getProcessSettings().getCommand() != null ? p.getProcessSettings().getCommand().getName() : "null");
            log("  ProcessSettings.inputs: {}", 
                    p.getProcessSettings().getInputs() != null ? p.getProcessSettings().getInputs().size() : "null");
            log("  ProcessSettings.validations: {}", 
                    p.getProcessSettings().getValidations() != null ? p.getProcessSettings().getValidations().size() : "null");
            log("  ProcessSettings.globalVariables: {}", 
                    p.getProcessSettings().getGlobalVariables());
        }
        log("  Compounds: {}", p.getCompounds());
        log("  Sections: {}", p.getSections());
    }

    private void logCompoundConfigurationFields(CompoundConfiguration c) {
        log("  ID: {}", c.getId());
        log("  SourceId: {}", c.getSourceId());
        log("  Name: {}", c.getName());
        log("  ReferToInput: {}", c.getReferToInput());
    }

    private void logSectionConfigurationFields(SectionConfiguration s) {
        log("  ID: {}", s.getId());
        log("  SourceId: {}", s.getSourceId());
        log("  Name: {}", s.getName());
        log("  Place: {}", s.getPlace());
        log("  ProcessRefs: {}", s.getProcessRefs().size());
        log("  CompoundRefs: {}", s.getCompoundRefs().size());
        log("  NestedSections: {}", s.getSections() != null ? s.getSections().size() : "null");
    }
}

