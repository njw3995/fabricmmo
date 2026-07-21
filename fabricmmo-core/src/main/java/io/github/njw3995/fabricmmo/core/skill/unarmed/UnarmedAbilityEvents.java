package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.UUID;

/** Publishes Berserk state transitions through the public API event bus. */
public final class UnarmedAbilityEvents {
    private UnarmedAbilityEvents() { }
    public static void prepared(UUID id) { publish(id, AbilityStateEvent.State.PREPARED); }
    public static void activated(UUID id) { publish(id, AbilityStateEvent.State.ACTIVATED); }
    public static void expired(UUID id) { publish(id, AbilityStateEvent.State.EXPIRED); }
    public static void cancelled(UUID id) { publish(id, AbilityStateEvent.State.CANCELLED); }
    private static void publish(UUID id, AbilityStateEvent.State state) {
        if (FabricMmoFabricRuntime.running()) {
            FabricMmoFabricRuntime.requireApi().events().publish(
                    new AbilityStateEvent(id, CoreUnarmedAbilities.BERSERK, state));
        }
    }
}
