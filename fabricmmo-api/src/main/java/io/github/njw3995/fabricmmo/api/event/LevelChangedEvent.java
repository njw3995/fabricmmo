package io.github.njw3995.fabricmmo.api.event;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Objects;
import java.util.UUID;

public record LevelChangedEvent(UUID playerId, NamespacedId skillId, int oldLevel, int newLevel) {
    public LevelChangedEvent {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");
    }
}
