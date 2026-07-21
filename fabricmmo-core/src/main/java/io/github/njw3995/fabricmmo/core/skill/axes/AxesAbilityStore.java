package io.github.njw3995.fabricmmo.core.skill.axes;

import java.io.IOException;
import java.util.UUID;

public interface AxesAbilityStore extends AutoCloseable {
    AxesAbilityData load(UUID playerId) throws IOException;
    void save(UUID playerId, AxesAbilityData data) throws IOException;
    @Override void close() throws IOException;
}
