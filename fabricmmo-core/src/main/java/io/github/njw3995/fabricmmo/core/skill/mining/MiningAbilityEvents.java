package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.UUID;

/** Publishes runtime Mining ability transitions through the public FabricMMO event bus. */
public final class MiningAbilityEvents {
    private MiningAbilityEvents() {
    }

    public static void prepared(UUID playerId, NamespacedId abilityId) {
        publish(playerId, abilityId, AbilityStateEvent.State.PREPARED);
    }

    public static void activated(UUID playerId, NamespacedId abilityId) {
        publish(playerId, abilityId, AbilityStateEvent.State.ACTIVATED);
    }

    public static void expired(UUID playerId, NamespacedId abilityId) {
        publish(playerId, abilityId, AbilityStateEvent.State.EXPIRED);
    }

    public static void cancelled(UUID playerId, NamespacedId abilityId) {
        publish(playerId, abilityId, AbilityStateEvent.State.CANCELLED);
    }

    private static void publish(UUID playerId, NamespacedId abilityId, AbilityStateEvent.State state) {
        if (FabricMmoFabricRuntime.running()) {
            FabricMmoFabricRuntime.requireApi().events()
                    .publish(new AbilityStateEvent(playerId, abilityId, state));
        }
    }
}
