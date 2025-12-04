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

import static org.junit.jupiter.api.Assertions.*;

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
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ConfigurationReference;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;

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
}

