package io.github.njw3995.fabricmmo.api.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Objects;
import java.util.UUID;

public record ProgressionSnapshot(UUID playerId, NamespacedId skillId, int level, int xp, int xpToNextLevel) {
    public ProgressionSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");
        if (level < 0 || xp < 0 || xpToNextLevel <= 0) {
            throw new IllegalArgumentException("Invalid progression values");
        }
    }
}
