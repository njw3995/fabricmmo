package io.github.njw3995.fabricmmo.core.skill.swords;

import java.io.IOException;
import java.util.UUID;

public interface SwordsAbilityStore extends AutoCloseable {
    SwordsAbilityData load(UUID playerId) throws IOException;
    void save(UUID playerId, SwordsAbilityData data) throws IOException;
    @Override void close() throws IOException;
}
