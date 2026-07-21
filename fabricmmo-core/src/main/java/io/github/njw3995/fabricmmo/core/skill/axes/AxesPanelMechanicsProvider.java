package io.github.njw3995.fabricmmo.core.skill.axes;

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

/** Exact mcMMO 2.3.000 AxesCommand statsDisplay ordering and wording. */
public final class AxesPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final AxesSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public AxesPanelMechanicsProvider(
            MinecraftServer server,
            AxesSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.AXES_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (settings.armorImpactRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.AXES_ARMOR_IMPACT, true))) {
            rows.add(new MechanicRow(
                    locale.text("Axes.Ability.Bonus.2"),
                    locale.text("Axes.Ability.Bonus.3", settings.armorImpactRawDamage(level))));
        }
        if (settings.axeMasteryRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.AXES_AXE_MASTERY, true))) {
            rows.add(new MechanicRow(
                    locale.text("Axes.Ability.Bonus.0"),
                    locale.text("Axes.Ability.Bonus.1", settings.axeMasteryDamage(level))));
        }
        if (settings.criticalRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.AXES_CRITICAL_STRIKES, true))) {
            rows.add(new MechanicRow(
                    locale.text("Axes.SubSkill.CriticalStrikes.Stat"),
                    chance(settings.criticalChancePercent(level, false),
                            settings.criticalChancePercent(level, true), lucky)));
        }
        if (settings.greaterImpactRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.AXES_GREATER_IMPACT, true))) {
            rows.add(new MechanicRow(
                    locale.text("Axes.Ability.Bonus.4"),
                    locale.text("Axes.Ability.Bonus.5", settings.greaterImpactBonusDamage())));
        }
        if (settings.skullSplitterRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.AXES_SKULL_SPLITTER, true))) {
            int baseDuration = settings.skullSplitterDurationSeconds(level);
            int enduranceBonus = player == null ? 0 : AxesPerks.activationBonusSeconds(
                    player.getCommandSource(), permissions);
            String duration = Integer.toString(baseDuration);
            if (enduranceBonus > 0) {
                duration += locale.text(
                        "Perks.ActivationTime.Bonus", baseDuration + enduranceBonus);
            }
            rows.add(new MechanicRow(
                    locale.text("Axes.SubSkill.SkullSplitter.Stat"), duration));
        }
        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.AXES_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Axes.SubSkill.AxesLimitBreak.Stat"),
                    Integer.toString(AxesDamage.limitBreakDamage(
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
