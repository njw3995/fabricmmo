package io.github.njw3995.fabricmmo.core.skill.archery;

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

/** Exact mcMMO 2.3.000 ArcheryCommand statsDisplay ordering and wording. */
public final class ArcheryPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final ArcherySettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public ArcheryPanelMechanicsProvider(
            MinecraftServer server,
            ArcherySettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.ARCHERY_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (settings.retrievalRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.ARCHERY_ARROW_RETRIEVAL, true))) {
            rows.add(new MechanicRow(
                    locale.text("Archery.SubSkill.ArrowRetrieval.Stat"),
                    chance(settings.retrievalChancePercent(level, false),
                            settings.retrievalChancePercent(level, true), lucky)));
        }
        if (!inspect || allowed(player, PermissionNodes.ARCHERY_DAZE, true)) {
            rows.add(new MechanicRow(
                    locale.text("Archery.SubSkill.Daze.Stat"),
                    chance(settings.dazeChancePercent(level, false),
                            settings.dazeChancePercent(level, true), lucky)));
        }
        if (settings.skillShotRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.ARCHERY_SKILL_SHOT, true))) {
            rows.add(new MechanicRow(
                    locale.text("Archery.SubSkill.SkillShot.Stat"),
                    percent(settings.skillShotBonusPercent(level) * 100.0D)));
        }
        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.ARCHERY_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Archery.SubSkill.ArcheryLimitBreak.Stat"),
                    Integer.toString(RangedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), 1000))));
        }
        return List.copyOf(rows);
    }

    private String chance(double normal, double luckyValue, boolean lucky) {
        String value = percent(normal);
        return lucky ? value + locale.text("Perks.Lucky.Bonus", percent(luckyValue)) : value;
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
