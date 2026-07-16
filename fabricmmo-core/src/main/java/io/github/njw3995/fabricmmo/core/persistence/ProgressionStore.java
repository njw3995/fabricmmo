package io.github.njw3995.fabricmmo.core.persistence;

import java.io.IOException;
import java.util.UUID;

public interface ProgressionStore extends AutoCloseable {
    PlayerProgressionData load(UUID playerId) throws IOException;

    void save(PlayerProgressionData data) throws IOException;

    @Override
    default void close() throws IOException {
    }
}
