package io.github.njw3995.fabricmmo.core.skill.crossbows;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedDamage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Exact mcMMO 2.3.000 CrossbowsCommand statsDisplay ordering and wording. */
public final class CrossbowsPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final CrossbowsSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public CrossbowsPanelMechanicsProvider(
            MinecraftServer server,
            CrossbowsSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (settings.poweredShotRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.CROSSBOWS_POWERED_SHOT, true))) {
            rows.add(new MechanicRow(
                    locale.text("Crossbows.SubSkill.PoweredShot.Stat"),
                    percent(settings.poweredShotBonusPercent(level) * 100.0D)));
        }
        int trickShotRank = settings.trickShotRank(level);
        if (trickShotRank > 0
                && (!inspect || allowed(player, PermissionNodes.CROSSBOWS_TRICK_SHOT, true))) {
            rows.add(new MechanicRow(
                    locale.text("Crossbows.SubSkill.TrickShot.Stat"),
                    Integer.toString(trickShotRank)));
        }
        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.CROSSBOWS_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Crossbows.SubSkill.CrossbowsLimitBreak.Stat"),
                    Integer.toString(RangedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), 1000))));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
