package io.github.njw3995.fabricmmo.core.skill.axes;

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

/** Bridges Skull Splitter state into the public API view. */
public final class AxesAbilityStateView implements AbilityStateView {
    private final MinecraftServer server;
    private final AxesAbilityController controller;
    private final AxesSettings settings;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public AxesAbilityStateView(MinecraftServer server, AxesAbilityController controller, AxesSettings settings) {
        this.server = Objects.requireNonNull(server, "server");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.settings = Objects.requireNonNull(settings, "settings");
    }
    @Override public boolean isActive(UUID playerId, NamespacedId abilityId) {
        return !activeRemaining(playerId, abilityId).isZero();
    }
    @Override public Duration activeRemaining(UUID playerId, NamespacedId abilityId) {
        if (!CoreAxesAbilities.SKULL_SPLITTER.equals(abilityId)) return Duration.ZERO;
        try { return Duration.ofSeconds(controller.activeSecondsRemaining(playerId)); }
        catch (IOException e) { throw new UncheckedIOException("Unable to read Skull Splitter state", e); }
    }
    @Override public Duration cooldownRemaining(UUID playerId, NamespacedId abilityId) {
        if (!CoreAxesAbilities.SKULL_SPLITTER.equals(abilityId)) return Duration.ZERO;
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        int cooldown = player == null ? settings.skullSplitterCooldownSeconds()
                : AxesPerks.cooldownSeconds(settings.skullSplitterCooldownSeconds(), player.getCommandSource(), permissions);
        try { return Duration.ofSeconds(controller.cooldownRemaining(playerId, cooldown)); }
        catch (IOException e) { throw new UncheckedIOException("Unable to read Skull Splitter cooldown", e); }
    }
}
