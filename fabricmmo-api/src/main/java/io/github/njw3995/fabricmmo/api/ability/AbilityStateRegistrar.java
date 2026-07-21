package io.github.njw3995.fabricmmo.api.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;

/**
 * Connects addon-owned active-ability state to FabricMMO scoreboards, XP bars, debug output,
 * and other state readers without transferring authority to core.
 */
@FunctionalInterface
public interface AbilityStateRegistrar {
    void registerAbilityStateView(NamespacedId abilityId, AbilityStateView view);

    static AbilityStateRegistrar unsupported() {
        return (abilityId, view) -> {
            throw new UnsupportedOperationException(
                    "This FabricMMO API implementation does not support addon ability state views");
        };
    }
}
