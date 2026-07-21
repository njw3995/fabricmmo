package io.github.njw3995.fabricmmo.core.ability;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.PassiveDefinition;
import io.github.njw3995.fabricmmo.api.ability.PassiveResult;
import io.github.njw3995.fabricmmo.api.ability.PassiveService;
import io.github.njw3995.fabricmmo.api.event.FabricMmoEventBus;
import io.github.njw3995.fabricmmo.api.event.PassiveActivationEvent;
import io.github.njw3995.fabricmmo.api.progression.ProgressionService;
import io.github.njw3995.fabricmmo.api.random.RandomSource;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Default passive resolver shared by core and external addons. */
public final class PassivePipeline implements PassiveService {
    private final DefaultAbilityRegistry registry;
    private final SkillRegistryView skills;
    private final ProgressionService progression;
    private final FabricMmoEventBus events;
    private volatile PlayerResolver playerResolver = ignored -> Optional.empty();

    public PassivePipeline(
            DefaultAbilityRegistry registry,
            SkillRegistryView skills,
            ProgressionService progression,
            FabricMmoEventBus events) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.skills = Objects.requireNonNull(skills, "skills");
        this.progression = Objects.requireNonNull(progression, "progression");
        this.events = Objects.requireNonNull(events, "events");
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
    public PassiveResult rollOnline(
            UUID playerId,
            NamespacedId passiveId,
            double baseProbability,
            RandomSource random) {
        PassiveDefinition passive = requirePassive(passiveId);
        PlayerContext player = playerResolver.resolve(playerId).orElse(null);
        if (player == null || !player.eligible()) {
            return new PassiveResult(PassiveResult.Status.INELIGIBLE, 0.0D);
        }
        SkillDefinition skill = skills.find(passive.skillId()).orElse(null);
        if (skill == null || !hasPermission(player, skillPermission(skill))) {
            return new PassiveResult(PassiveResult.Status.INELIGIBLE, 0.0D);
        }
        String passivePermission = passive.metadata().getOrDefault("permission", "").trim();
        if (!hasPermission(player, passivePermission)) {
            return new PassiveResult(PassiveResult.Status.INELIGIBLE, 0.0D);
        }
        return roll(playerId, passiveId, baseProbability, random);
    }

    @Override
    public PassiveResult roll(
            UUID playerId,
            NamespacedId passiveId,
            double baseProbability,
            RandomSource random) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(random, "random");
        if (!Double.isFinite(baseProbability)
                || baseProbability < 0.0D
                || baseProbability > 1.0D) {
            throw new IllegalArgumentException("baseProbability must be between 0 and 1");
        }

        PassiveDefinition passive = requirePassive(passiveId);
        int skillLevel = progression.query(playerId, passive.skillId()).level();
        if (skillLevel < passive.unlockLevel()) {
            return new PassiveResult(PassiveResult.Status.LOCKED, 0.0D);
        }

        PassiveActivationEvent event = new PassiveActivationEvent(
                playerId, passiveId, baseProbability);
        events.publish(event);
        if (event.cancelled()) {
            return new PassiveResult(PassiveResult.Status.CANCELLED, 0.0D);
        }
        double probability = event.resultingProbability();
        return new PassiveResult(
                random.roll(probability)
                        ? PassiveResult.Status.ACTIVATED
                        : PassiveResult.Status.FAILED,
                probability);
    }

    private PassiveDefinition requirePassive(NamespacedId passiveId) {
        Objects.requireNonNull(passiveId, "passiveId");
        return registry.passive(passiveId).orElseThrow(
                () -> new IllegalArgumentException("Unknown passive ability: " + passiveId));
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
}
