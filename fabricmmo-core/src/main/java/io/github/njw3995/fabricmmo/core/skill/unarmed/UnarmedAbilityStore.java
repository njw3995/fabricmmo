package io.github.njw3995.fabricmmo.core.skill.unarmed;

import java.io.IOException;
import java.util.UUID;

public interface UnarmedAbilityStore extends AutoCloseable {
    UnarmedAbilityData load(UUID playerId) throws IOException;
    void save(UUID playerId, UnarmedAbilityData data) throws IOException;
    @Override void close() throws IOException;
}
