package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Exact mcMMO 2.3.000 AcrobaticsCommand statsDisplay rows. */
public final class AcrobaticsPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final AcrobaticsSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public AcrobaticsPanelMechanicsProvider(
            MinecraftServer server,
            AcrobaticsSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.ACROBATICS_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>(2);
        if (level >= settings.dodgeUnlockLevel()
                && (!inspect || allowed(player, PermissionNodes.ACROBATICS_DODGE, true))) {
            rows.add(new MechanicRow(
                    locale.text("Acrobatics.SubSkill.Dodge.Stat"),
                    chance(settings.dodgeChancePercent(level, false),
                            settings.dodgeChancePercent(level, true), lucky)));
        }
        if (!inspect || allowed(player, PermissionNodes.ACROBATICS_ROLL, true)) {
            rows.add(new MechanicRow(
                    locale.text("Acrobatics.SubSkill.Roll.Stat"),
                    chance(settings.rollChancePercent(level, false),
                            settings.rollChancePercent(level, true), lucky)));
        }
        return List.copyOf(rows);
    }

    private String chance(double normal, double luckyValue, boolean lucky) {
        String result = percent(normal);
        if (lucky) {
            result += locale.text("Perks.Lucky.Bonus", percent(luckyValue));
        }
        return result;
    }

    private boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
