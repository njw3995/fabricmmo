package io.github.njw3995.fabricmmo.api.event;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Objects;
import java.util.UUID;

public record AbilityStateEvent(UUID playerId, NamespacedId abilityId, State state) {
    public enum State { PREPARED, ACTIVATED, EXPIRED, CANCELLED }

    public AbilityStateEvent {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(state, "state");
    }
}
