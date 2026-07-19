package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import java.io.IOException;
import java.util.UUID;

public interface WoodcuttingAbilityStore extends AutoCloseable {
    WoodcuttingAbilityData load(UUID playerId) throws IOException;

    void save(UUID playerId, WoodcuttingAbilityData data) throws IOException;

    @Override
    void close() throws IOException;
}
