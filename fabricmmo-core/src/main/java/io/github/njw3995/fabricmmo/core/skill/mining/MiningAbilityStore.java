package io.github.njw3995.fabricmmo.core.skill.mining;

import java.io.IOException;
import java.util.UUID;

public interface MiningAbilityStore extends AutoCloseable {
    MiningAbilityData load(UUID playerId) throws IOException;

    void save(UUID playerId, MiningAbilityData data) throws IOException;

    @Override
    void close() throws IOException;
}
