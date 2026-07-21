package io.github.njw3995.fabricmmo.api.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.UUID;

/**
 * Core-owned preparation, activation, duration, and cooldown state for active abilities.
 *
 * <p>An addon remains responsible for listening to the Minecraft or source-mod action that should
 * prepare or activate the ability and for applying the ability's gameplay effect. FabricMMO owns
 * the timing state and publishes the normal ability-state events.</p>
 */
public interface AbilityService extends AbilityStateView {
    /**
     * Preferred gameplay entry point. Core resolves the registered skill level and validates that
     * the player is online, eligible, and permitted to use both the skill and ability.
     */
    boolean prepareOnline(UUID playerId, NamespacedId abilityId);

    /**
     * Preferred activation entry point. Core revalidates online presence, game mode, and skill or
     * ability permissions at activation time so a prepared ability cannot bypass a later change.
     */
    boolean activateOnline(UUID playerId, NamespacedId abilityId);

    /**
     * Low-level preparation entry point for controlled tests, migration, and non-player contexts.
     * Normal gameplay listeners should use {@link #prepareOnline(UUID, NamespacedId)}.
     */
    boolean prepare(UUID playerId, NamespacedId abilityId, int skillLevel);

    boolean activate(UUID playerId, NamespacedId abilityId);

    boolean cancel(UUID playerId, NamespacedId abilityId);

    void expire(UUID playerId, NamespacedId abilityId);

    boolean onCooldown(UUID playerId, NamespacedId abilityId);

    static AbilityService unsupported() {
        return new AbilityService() {
            private UnsupportedOperationException unsupported() {
                return new UnsupportedOperationException(
                        "This FabricMMO API implementation does not provide core-owned ability state");
            }

            @Override
            public boolean prepareOnline(UUID playerId, NamespacedId abilityId) {
                throw unsupported();
            }

            @Override
            public boolean activateOnline(UUID playerId, NamespacedId abilityId) {
                throw unsupported();
            }

            @Override
            public boolean prepare(UUID playerId, NamespacedId abilityId, int skillLevel) {
                throw unsupported();
            }

            @Override
            public boolean activate(UUID playerId, NamespacedId abilityId) {
                throw unsupported();
            }

            @Override
            public boolean cancel(UUID playerId, NamespacedId abilityId) {
                throw unsupported();
            }

            @Override
            public void expire(UUID playerId, NamespacedId abilityId) {
                throw unsupported();
            }

            @Override
            public boolean onCooldown(UUID playerId, NamespacedId abilityId) {
                return !cooldownRemaining(playerId, abilityId).isZero();
            }

            @Override
            public boolean isActive(UUID playerId, NamespacedId abilityId) {
                return false;
            }

            @Override
            public java.time.Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
                return java.time.Duration.ZERO;
            }

            @Override
            public java.time.Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
                return java.time.Duration.ZERO;
            }
        };
    }
}
