package org.qubership.atp.mia.service.configuration.snapshot;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.mia.model.configuration.PotHeaderConfiguration;
import org.qubership.atp.mia.model.impl.executable.PotHeader;

/**
 * Immutable projection of {@link PotHeaderConfiguration}.
 */
public final class PotHeaderConfigurationSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID projectId;
    private final List<PotHeader> headers;

    private PotHeaderConfigurationSnapshot(UUID projectId, List<PotHeader> headers) {
        this.projectId = projectId;
        this.headers = headers;
    }

    public static PotHeaderConfigurationSnapshot from(PotHeaderConfiguration potHeaderConfiguration) {
        if (potHeaderConfiguration == null) {
            return null;
        }
        List<PotHeader> headersCopy = potHeaderConfiguration.getHeaders() == null
                ? Collections.emptyList()
                : potHeaderConfiguration.getHeaders().stream()
                .filter(Objects::nonNull)
                .map(header -> header.toBuilder().build())
                .collect(Collectors.toList());
        return new PotHeaderConfigurationSnapshot(potHeaderConfiguration.getProjectId(), headersCopy);
    }

    public UUID getProjectId() {
        return projectId;
    }

    public List<PotHeader> getHeaders() {
        return headers == null ? Collections.emptyList() : Collections.unmodifiableList(headers);
    }
}


