package io.github.njw3995.fabricmmo.core.persistence;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Administrative capabilities shared by supported FabricMMO persistence backends. */
public interface ManagedProgressionStore extends ProgressionStore {
    String backendName();

    Set<UUID> playerIds() throws IOException;

    boolean delete(UUID playerId) throws IOException;

    Instant lastSeen(UUID playerId) throws IOException;

    default void touch(UUID playerId, Instant instant) throws IOException {
        // Backends without an independent login timestamp may use their file modification time.
    }
}
