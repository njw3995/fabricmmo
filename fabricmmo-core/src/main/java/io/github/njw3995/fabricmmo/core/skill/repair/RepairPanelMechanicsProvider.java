package io.github.njw3995.fabricmmo.core.skill.repair;

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

/** Exact mcMMO 2.3.000 RepairCommand statsDisplay ordering and values. */
public final class RepairPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final RepairSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public RepairPanelMechanicsProvider(
            MinecraftServer server,
            RepairSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.REPAIR_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();
        int arcaneRank = settings.arcaneForgingRank(level);
        if (arcaneRank > 0
                && (!inspect || allowed(player, PermissionNodes.REPAIR_ARCANE_FORGING, true))) {
            rows.add(MechanicRow.custom(
                    locale.text("Repair.SubSkill.ArcaneForging.Stat", arcaneRank, 8)));
            if (settings.arcaneMayLoseEnchants() || settings.arcaneDowngradesEnabled()) {
                boolean bypass = inspect && (allowed(player, PermissionNodes.ARCANE_BYPASS, false)
                        || allowed(player, PermissionNodes.REPAIR_ENCHANT_BYPASS, false));
                double keep = bypass ? 100.0D : settings.keepEnchantChance()[arcaneRank - 1];
                double downgrade = bypass ? 0.0D : settings.avoidDowngradeChance()[arcaneRank - 1];
                rows.add(MechanicRow.custom(locale.text(
                        "Repair.SubSkill.ArcaneForging.Stat.Extra",
                        number(keep), number(downgrade))));
            }
        }
        if (settings.repairMasteryUnlocked(level)
                && (!inspect || allowed(player, PermissionNodes.REPAIR_MASTERY, true))) {
            double bonus = Math.min(
                    settings.masteryMaximumBonusPercentage(),
                    settings.masteryMaximumBonusPercentage()
                            * Math.min(level, settings.masteryMaximumBonusLevel())
                            / settings.masteryMaximumBonusLevel());
            rows.add(MechanicRow.custom(
                    locale.text("Repair.SubSkill.RepairMastery.Stat", percent(bonus))));
        }
        if (settings.superRepairUnlocked(level)
                && (!inspect || allowed(player, PermissionNodes.REPAIR_SUPER_REPAIR, true))) {
            double normal = settings.superRepairChance(level, false);
            String value = percent(normal);
            if (lucky) {
                value += locale.text(
                        "Perks.Lucky.Bonus",
                        percent(settings.superRepairChance(level, true)));
            }
            rows.add(new MechanicRow(
                    locale.text("Repair.SubSkill.SuperRepair.Stat"), value));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private static String number(double value) {
        return Double.toString(value);
    }
}
