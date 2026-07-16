package io.github.njw3995.fabricmmo.api.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.time.Duration;
import java.util.UUID;

public interface AbilityStateView {
    boolean isActive(UUID playerId, NamespacedId abilityId);

    Duration activeRemaining(UUID playerId, NamespacedId abilityId);

    Duration cooldownRemaining(UUID playerId, NamespacedId abilityId);
}
