package io.github.njw3995.fabricmmo.core.skill.excavation;

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

/** Bridges Giga Drill Breaker state into the public API view. */
public final class ExcavationAbilityStateView implements AbilityStateView {
    private final MinecraftServer server;
    private final ExcavationAbilityController controller;
    private final ExcavationSettings settings;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public ExcavationAbilityStateView(
            MinecraftServer server,
            ExcavationAbilityController controller,
            ExcavationSettings settings) {
        this.server = Objects.requireNonNull(server, "server");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public boolean isActive(UUID playerId, NamespacedId abilityId) {
        return !activeRemaining(playerId, abilityId).isZero();
    }

    @Override
    public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
        if (!CoreExcavationAbilities.GIGA_DRILL_BREAKER.equals(abilityId)) {
            return Duration.ZERO;
        }
        try {
            return Duration.ofSeconds(controller.activeSecondsRemaining(playerId));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Giga Drill Breaker state", exception);
        }
    }

    @Override
    public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
        if (!CoreExcavationAbilities.GIGA_DRILL_BREAKER.equals(abilityId)) {
            return Duration.ZERO;
        }
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        int cooldown = player == null
                ? settings.gigaDrillCooldownSeconds()
                : ExcavationPerks.cooldownSeconds(
                        settings.gigaDrillCooldownSeconds(),
                        player.getCommandSource(),
                        permissions);
        try {
            return Duration.ofSeconds(controller.cooldownRemaining(playerId, cooldown));
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Giga Drill Breaker cooldown", exception);
        }
    }
}
