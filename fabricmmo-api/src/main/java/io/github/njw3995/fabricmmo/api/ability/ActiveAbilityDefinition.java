package io.github.njw3995.fabricmmo.api.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record ActiveAbilityDefinition(
        NamespacedId id,
        NamespacedId skillId,
        int unlockLevel,
        Duration readyTimeout,
        Duration baseDuration,
        Duration baseCooldown,
        Map<String, String> metadata) {

    public ActiveAbilityDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(readyTimeout, "readyTimeout");
        Objects.requireNonNull(baseDuration, "baseDuration");
        Objects.requireNonNull(baseCooldown, "baseCooldown");
        if (unlockLevel < 0 || readyTimeout.isNegative() || readyTimeout.isZero()
                || baseDuration.isNegative() || baseDuration.isZero()
                || baseCooldown.isNegative()) {
            throw new IllegalArgumentException("Invalid active ability timing or level");
        }
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }
}
