package org.qubership.atp.mia.service.configuration.snapshot;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.qubership.atp.mia.model.configuration.CommandPrefix;

/**
 * Immutable projection of {@link CommandPrefix} tailored for caching/general usage.
 */
public final class CommandPrefixSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID projectId;
    private final String system;
    private final LinkedHashMap<String, String> prefixes;

    private CommandPrefixSnapshot(UUID projectId, String system, LinkedHashMap<String, String> prefixes) {
        this.projectId = projectId;
        this.system = system;
        this.prefixes = prefixes;
    }

    public static CommandPrefixSnapshot from(CommandPrefix commandPrefix) {
        if (commandPrefix == null) {
            return null;
        }
        LinkedHashMap<String, String> prefixesCopy = commandPrefix.getPrefixes() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(commandPrefix.getPrefixes());
        return new CommandPrefixSnapshot(commandPrefix.getProjectId(), commandPrefix.getSystem(), prefixesCopy);
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getSystem() {
        return system;
    }

    public Map<String, String> getPrefixes() {
        return Collections.unmodifiableMap(prefixes);
    }
}


