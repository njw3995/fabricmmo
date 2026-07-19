package io.github.njw3995.fabricmmo.core.skill.herbalism;

import java.io.IOException;
import java.util.UUID;

public interface HerbalismAbilityStore extends AutoCloseable {
    HerbalismAbilityData load(UUID playerId) throws IOException;

    void save(UUID playerId, HerbalismAbilityData data) throws IOException;

    @Override
    void close() throws IOException;
}
