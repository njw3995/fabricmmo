package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.ability.AbilityStateView;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Bridges persisted Mining ability state into the public API view. */
public final class MiningAbilityStateView implements AbilityStateView {
    private final MinecraftServer server;
    private final MiningAbilityController controller;
    private final MiningSettings settings;
    private final FabricCommandPermissionService permissions;

    public MiningAbilityStateView(
            MinecraftServer server,
            MiningAbilityController controller,
            MiningSettings settings) {
        this.server = Objects.requireNonNull(server, "server");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.permissions = new FabricCommandPermissionService();
    }

    @Override
    public boolean isActive(UUID playerId, NamespacedId abilityId) {
        return !activeRemaining(playerId, abilityId).isZero();
    }

    @Override
    public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
        if (!CoreMiningAbilities.SUPER_BREAKER.equals(abilityId)) {
            return Duration.ZERO;
        }
        try {
            return Duration.ofSeconds(controller.superBreakerSecondsRemaining(playerId));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Super Breaker state", exception);
        }
    }

    @Override
    public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
        try {
            if (CoreMiningAbilities.SUPER_BREAKER.equals(abilityId)) {
                return Duration.ofSeconds(controller.superBreakerCooldownRemaining(
                        playerId, cooldownSeconds(
                                playerId, settings.superBreakerCooldownSeconds())));
            }
            if (CoreMiningAbilities.BLAST_MINING.equals(abilityId)) {
                return Duration.ofSeconds(controller.blastCooldownRemaining(
                        playerId, cooldownSeconds(
                                playerId, settings.blastMiningCooldownSeconds())));
            }
            return Duration.ZERO;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Mining ability cooldown", exception);
        }
    }

    private int cooldownSeconds(UUID playerId, int baseSeconds) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        return player == null
                ? baseSeconds
                : MiningPerks.cooldownSeconds(
                        baseSeconds, player.getCommandSource(), permissions);
    }
}
