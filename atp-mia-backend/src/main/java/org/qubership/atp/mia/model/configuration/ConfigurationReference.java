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
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lightweight reference to Process or Compound configuration.
 * Used for caching to avoid serializing full objects to Hazelcast.
 * Contains only ID and name for quick lookups.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigurationReference implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String name;
    
    /**
     * Create reference from ProcessConfiguration.
     * 
     * @param process process configuration
     * @return configuration reference
     */
    public static ConfigurationReference fromProcess(ProcessConfiguration process) {
        if (process == null) {
            return null;
        }
        return new ConfigurationReference(process.getId(), process.getName());
    }
    
    /**
     * Create reference from CompoundConfiguration.
     * 
     * @param compound compound configuration
     * @return configuration reference
     */
    public static ConfigurationReference fromCompound(CompoundConfiguration compound) {
        if (compound == null) {
            return null;
        }
        return new ConfigurationReference(compound.getId(), compound.getName());
    }
    
    /**
     * Convert list of refs to lightweight ProcessConfiguration objects.
     * 
     * @param refs list of references
     * @return list of lightweight ProcessConfiguration with only id and name
     */
    public static List<ProcessConfiguration> toProcessConfigurations(List<ConfigurationReference> refs) {
        return refs.stream()
                .map(ref -> ProcessConfiguration.builder()
                        .id(ref.getId())
                        .name(ref.getName())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Convert list of refs to lightweight CompoundConfiguration objects.
     * 
     * @param refs list of references
     * @return list of lightweight CompoundConfiguration with only id and name
     */
    public static List<CompoundConfiguration> toCompoundConfigurations(List<ConfigurationReference> refs) {
        return refs.stream()
                .map(ref -> CompoundConfiguration.builder()
                        .id(ref.getId())
                        .name(ref.getName())
                        .build())
                .collect(Collectors.toList());
    }
}

