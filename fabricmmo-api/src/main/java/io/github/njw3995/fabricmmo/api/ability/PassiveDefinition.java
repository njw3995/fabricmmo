package io.github.njw3995.fabricmmo.api.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

public record PassiveDefinition(NamespacedId id, NamespacedId skillId, int unlockLevel, Map<String, String> metadata) {
    public PassiveDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(skillId, "skillId");
        if (unlockLevel < 0) {
            throw new IllegalArgumentException("unlockLevel must be non-negative");
        }
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }
}
