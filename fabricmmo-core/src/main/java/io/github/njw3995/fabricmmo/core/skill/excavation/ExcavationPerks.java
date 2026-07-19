package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.core.permission.CommandPermissionService;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningPerks;
import net.minecraft.server.command.ServerCommandSource;

/** Shared upstream super-ability activation and cooldown perk resolution. */
public final class ExcavationPerks {
    private ExcavationPerks() {
    }

    public static int activationBonusSeconds(
            ServerCommandSource source,
            CommandPermissionService permissions) {
        return MiningPerks.activationBonusSeconds(source, permissions);
    }

    public static int cooldownSeconds(
            int baseSeconds,
            ServerCommandSource source,
            CommandPermissionService permissions) {
        return MiningPerks.cooldownSeconds(baseSeconds, source, permissions);
    }
}
