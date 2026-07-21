package io.github.njw3995.fabricmmo.core.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityService;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateRegistrar;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateView;
import io.github.njw3995.fabricmmo.api.ability.ActiveAbilityDefinition;
import io.github.njw3995.fabricmmo.api.event.AbilityStateEvent;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AbilityPipeline implements AbilityService, AbilityStateRegistrar {
    private final DefaultAbilityRegistry registry;
    private final SkillRegistryView skills;
    private final ProgressionService progression;
    private final FabricMmoEventBus eventBus;
    private final Clock clock;
    private final Map<Key, State> states = new ConcurrentHashMap<>();
    private final Map<NamespacedId, AbilityStateView> delegatedViews = new ConcurrentHashMap<>();
    private volatile PlayerResolver playerResolver = ignored -> Optional.empty();

    /** Low-level constructor retained for isolated timing tests. */
    public AbilityPipeline(
            DefaultAbilityRegistry registry,
            FabricMmoEventBus eventBus,
            Clock clock) {
        this(registry, null, null, eventBus, clock);
    }

    public AbilityPipeline(
            DefaultAbilityRegistry registry,
            SkillRegistryView skills,
            ProgressionService progression,
            FabricMmoEventBus eventBus,
            Clock clock) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.skills = skills;
        this.progression = progression;
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    void playerResolver(PlayerResolver resolver) {
        playerResolver = Objects.requireNonNull(resolver, "resolver");
    }

    public void bind(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        FabricCommandPermissionService permissions = new FabricCommandPermissionService();
        playerResolver = playerId -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                return Optional.empty();
            }
            if (player.isRemoved() || player.isCreative() || player.isSpectator()) {
                return Optional.of(PlayerContext.ineligible());
            }
            return Optional.of(PlayerContext.eligible(permission -> permissions.hasPermission(
                    player.getCommandSource(), permission, true)));
        };
    }

    @Override
    public void registerAbilityStateView(NamespacedId abilityId, AbilityStateView view) {
        NamespacedId checkedId = Objects.requireNonNull(abilityId, "abilityId");
        AbilityStateView checkedView = Objects.requireNonNull(view, "view");
        if (registry.active(checkedId).isEmpty()) {
            throw new IllegalArgumentException("Unknown active ability: " + checkedId);
        }
        AbilityStateView previous = delegatedViews.putIfAbsent(checkedId, checkedView);
        if (previous != null && previous != checkedView) {
            throw new IllegalStateException("Ability state view already registered: " + checkedId);
        }
    }

    /** @deprecated use the public API registrar method. */
    @Deprecated(forRemoval = false)
    public void registerStateView(NamespacedId abilityId, AbilityStateView view) {
        registerAbilityStateView(abilityId, view);
    }

    @Override
    public boolean prepareOnline(UUID playerId, NamespacedId abilityId) {
        Objects.requireNonNull(playerId, "playerId");
        ActiveAbilityDefinition ability = requireActive(abilityId);
        if (!eligibleOnline(playerId, ability)) {
            return false;
        }
        int skillLevel = progression.query(playerId, ability.skillId()).level();
        return prepare(playerId, abilityId, skillLevel);
    }

    @Override
    public boolean activateOnline(UUID playerId, NamespacedId abilityId) {
        Objects.requireNonNull(playerId, "playerId");
        ActiveAbilityDefinition ability = requireActive(abilityId);
        return eligibleOnline(playerId, ability) && activate(playerId, abilityId);
    }

    @Override
    public boolean prepare(UUID playerId, NamespacedId abilityId, int skillLevel) {
        ActiveAbilityDefinition ability = requireActive(abilityId);
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

    @Override
    public boolean activate(UUID playerId, NamespacedId abilityId) {
        ActiveAbilityDefinition ability = requireActive(abilityId);
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

    @Override
    public boolean cancel(UUID playerId, NamespacedId abilityId) {
        Key key = new Key(playerId, abilityId);
        State state = states.get(key);
        if (state == null) {
            return false;
        }
        Instant now = clock.instant();
        if (state.activeUntil().isAfter(now)) {
            states.put(key, new State(Instant.EPOCH, Instant.EPOCH, state.cooldownUntil()));
        } else {
            states.remove(key);
        }
        eventBus.publish(new AbilityStateEvent(playerId, abilityId,
                AbilityStateEvent.State.CANCELLED));
        return true;
    }

    @Override
    public void expire(UUID playerId, NamespacedId abilityId) {
        Key key = new Key(playerId, abilityId);
        states.computeIfPresent(key, (ignored, state) ->
                new State(Instant.EPOCH, Instant.EPOCH, state.cooldownUntil()));
        eventBus.publish(new AbilityStateEvent(playerId, abilityId,
                AbilityStateEvent.State.EXPIRED));
    }

    public void playerDisconnected(UUID playerId) {
        states.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    @Override
    public boolean onCooldown(UUID playerId, NamespacedId abilityId) {
        return !cooldownRemaining(playerId, abilityId).isZero();
    }

    @Override
    public boolean isActive(UUID playerId, NamespacedId abilityId) {
        AbilityStateView delegated = delegatedViews.get(abilityId);
        return delegated != null
                ? delegated.isActive(playerId, abilityId)
                : !activeRemaining(playerId, abilityId).isZero();
    }

    @Override
    public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
        AbilityStateView delegated = delegatedViews.get(abilityId);
        return delegated != null
                ? delegated.activeRemaining(playerId, abilityId)
                : remaining(states.get(new Key(playerId, abilityId)), true);
    }

    @Override
    public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
        AbilityStateView delegated = delegatedViews.get(abilityId);
        return delegated != null
                ? delegated.cooldownRemaining(playerId, abilityId)
                : remaining(states.get(new Key(playerId, abilityId)), false);
    }

    private Duration remaining(State state, boolean active) {
        if (state == null) {
            return Duration.ZERO;
        }
        Instant deadline = active ? state.activeUntil() : state.cooldownUntil();
        Instant now = clock.instant();
        return deadline.isAfter(now) ? Duration.between(now, deadline) : Duration.ZERO;
    }


    private ActiveAbilityDefinition requireActive(NamespacedId abilityId) {
        Objects.requireNonNull(abilityId, "abilityId");
        return registry.active(abilityId).orElseThrow(
                () -> new IllegalArgumentException("Unknown active ability: " + abilityId));
    }

    private boolean eligibleOnline(UUID playerId, ActiveAbilityDefinition ability) {
        if (skills == null || progression == null) {
            return false;
        }
        PlayerContext player = playerResolver.resolve(playerId).orElse(null);
        if (player == null || !player.eligible()) {
            return false;
        }
        SkillDefinition skill = skills.find(ability.skillId()).orElse(null);
        if (skill == null || !hasPermission(player, skillPermission(skill))) {
            return false;
        }
        String abilityPermission = ability.metadata().getOrDefault("permission", "").trim();
        return hasPermission(player, abilityPermission);
    }

    private static String skillPermission(SkillDefinition skill) {
        String explicit = skill.metadata().getOrDefault("permission", "").trim();
        if (!explicit.isBlank()) {
            return explicit;
        }
        if (skill.id().namespace().equals("fabricmmo")) {
            return "mcmmo.skills." + skill.id().path();
        }
        return "";
    }

    private static boolean hasPermission(PlayerContext player, String permission) {
        return permission.isBlank() || player.hasPermission().test(permission);
    }

    @FunctionalInterface
    interface PlayerResolver {
        Optional<PlayerContext> resolve(UUID playerId);
    }

    record PlayerContext(boolean eligible, Predicate<String> hasPermission) {
        PlayerContext {
            Objects.requireNonNull(hasPermission, "hasPermission");
        }

        static PlayerContext ineligible() {
            return new PlayerContext(false, ignored -> false);
        }

        static PlayerContext eligible(Predicate<String> hasPermission) {
            return new PlayerContext(true, hasPermission);
        }
    }

    private record Key(UUID playerId, NamespacedId abilityId) {
    }

    private record State(Instant readyUntil, Instant activeUntil, Instant cooldownUntil) {
    }
}
