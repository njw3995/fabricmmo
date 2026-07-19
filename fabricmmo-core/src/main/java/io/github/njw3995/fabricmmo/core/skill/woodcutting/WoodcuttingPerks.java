package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import net.minecraft.server.command.ServerCommandSource;

/** Upstream activation-time and cooldown perk resolution for Tree Feller. */
public final class WoodcuttingPerks {
    private WoodcuttingPerks() {
    }

    public static int activationBonusSeconds(
            ServerCommandSource source,
            CommandPermissionService permissions) {
        if (permissions.hasPermission(source, PermissionNodes.ACTIVATION_TIME_TWELVE_SECONDS, false)) {
            return 12;
        }
        if (permissions.hasPermission(source, PermissionNodes.ACTIVATION_TIME_EIGHT_SECONDS, false)) {
            return 8;
        }
        if (permissions.hasPermission(source, PermissionNodes.ACTIVATION_TIME_FOUR_SECONDS, false)) {
            return 4;
        }
        return 0;
    }

    public static int cooldownSeconds(
            int baseSeconds,
            ServerCommandSource source,
            CommandPermissionService permissions) {
        if (baseSeconds <= 0) {
            return 0;
        }
        if (permissions.hasPermission(source, PermissionNodes.COOLDOWN_HALVED, false)) {
            return (int) (baseSeconds * 0.5D);
        }
        if (permissions.hasPermission(source, PermissionNodes.COOLDOWN_THIRDED, false)) {
            return (int) (baseSeconds * (2.0D / 3.0D));
        }
        if (permissions.hasPermission(source, PermissionNodes.COOLDOWN_QUARTERED, false)) {
            return (int) (baseSeconds * 0.75D);
        }
        return baseSeconds;
    }
}
