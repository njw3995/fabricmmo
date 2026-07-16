package io.github.njw3995.fabricmmo.core.persistence;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

public record PlayerProgressionData(UUID playerId, long revision, Map<NamespacedId, StoredSkillProgress> skills) {
    public PlayerProgressionData {
        Objects.requireNonNull(playerId, "playerId");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        skills = Map.copyOf(new TreeMap<>(Objects.requireNonNull(skills, "skills")));
    }

    public static PlayerProgressionData empty(UUID playerId) {
        return new PlayerProgressionData(playerId, 0L, Map.of());
    }
}
