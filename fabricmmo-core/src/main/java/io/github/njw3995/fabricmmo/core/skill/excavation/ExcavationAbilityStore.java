package io.github.njw3995.fabricmmo.core.skill.excavation;

import java.io.IOException;
import java.util.UUID;

public interface ExcavationAbilityStore extends AutoCloseable {
    ExcavationAbilityData load(UUID playerId) throws IOException;

    void save(UUID playerId, ExcavationAbilityData data) throws IOException;

    @Override
    void close() throws IOException;
}
