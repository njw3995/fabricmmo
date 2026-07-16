package io.github.njw3995.fabricmmo.core.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateView;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityPipeline implements AbilityStateView {
    private final DefaultAbilityRegistry registry;
    private final FabricMmoEventBus eventBus;
    private final Clock clock;
    private final Map<Key, State> states = new ConcurrentHashMap<>();

    public AbilityPipeline(DefaultAbilityRegistry registry, FabricMmoEventBus eventBus, Clock clock) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.clock = clock;
    }

    public boolean prepare(UUID playerId, NamespacedId abilityId, int skillLevel) {
        ActiveAbilityDefinition ability = registry.active(abilityId).orElseThrow(
                () -> new IllegalArgumentException("Unknown active ability: " + abilityId));
        if (skillLevel < ability.unlockLevel()) {
            return false;
        }
        Key key = new Key(playerId, abilityId);
        Instant now = clock.instant();
        State existing = states.get(key);
        if (existing != null && existing.cooldownUntil().isAfter(now)) {
            return false;
        }
        states.put(key, new State(now.plus(ability.readyTimeout()), Instant.EPOCH, Instant.EPOCH));
        eventBus.publish(new AbilityStateEvent(playerId, abilityId,
                AbilityStateEvent.State.PREPARED));
        return true;
    }

    public boolean activate(UUID playerId, NamespacedId abilityId) {
        ActiveAbilityDefinition ability = registry.active(abilityId).orElseThrow(
                () -> new IllegalArgumentException("Unknown active ability: " + abilityId));
        Key key = new Key(playerId, abilityId);
        Instant now = clock.instant();
        State state = states.get(key);
        if (state == null || state.readyUntil().isBefore(now) || state.activeUntil().isAfter(now)) {
            return false;
        }
        states.put(key, new State(
                Instant.EPOCH,
                now.plus(ability.baseDuration()),
                now.plus(ability.baseCooldown())));
        eventBus.publish(new AbilityStateEvent(playerId, abilityId,
                AbilityStateEvent.State.ACTIVATED));
        return true;
    }

    public void expire(UUID playerId, NamespacedId abilityId) {
        Key key = new Key(playerId, abilityId);
        states.computeIfPresent(key, (ignored, state) ->
                new State(Instant.EPOCH, Instant.EPOCH, state.cooldownUntil()));
        eventBus.publish(new AbilityStateEvent(playerId, abilityId,
                AbilityStateEvent.State.EXPIRED));
    }

    public boolean onCooldown(UUID playerId, NamespacedId abilityId) {
        return !cooldownRemaining(playerId, abilityId).isZero();
    }

    @Override
    public boolean isActive(UUID playerId, NamespacedId abilityId) {
        return !activeRemaining(playerId, abilityId).isZero();
    }

    @Override
    public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
        return remaining(states.get(new Key(playerId, abilityId)), true);
    }

    @Override
    public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
        return remaining(states.get(new Key(playerId, abilityId)), false);
    }

    private Duration remaining(State state, boolean active) {
        if (state == null) {
            return Duration.ZERO;
        }
        Instant deadline = active ? state.activeUntil() : state.cooldownUntil();
        Instant now = clock.instant();
        return deadline.isAfter(now) ? Duration.between(now, deadline) : Duration.ZERO;
    }

    private record Key(UUID playerId, NamespacedId abilityId) {
    }

    private record State(Instant readyUntil, Instant activeUntil, Instant cooldownUntil) {
    }
}
