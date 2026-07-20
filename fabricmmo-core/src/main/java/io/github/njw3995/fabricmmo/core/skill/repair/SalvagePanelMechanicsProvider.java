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

/** Exact mcMMO 2.3.000 SalvageCommand statsDisplay ordering and values. */
public final class SalvagePanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final SalvageSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public SalvagePanelMechanicsProvider(
            MinecraftServer server,
            SalvageSettings settings,
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
        int scrapRank = settings.scrapCollectorRank(level);
        if (scrapRank > 0
                && (!inspect || allowed(player, PermissionNodes.SALVAGE_SCRAP_COLLECTOR, true))) {
            rows.add(MechanicRow.custom(locale.text(
                    "Salvage.SubSkill.ScrapCollector.Stat",
                    SalvageFormula.scrapCollectorLimit(scrapRank))));
        }
        int arcaneRank = settings.arcaneSalvageRank(level);
        if (arcaneRank > 0
                && (!inspect || allowed(player, PermissionNodes.SALVAGE_ARCANE, true))) {
            rows.add(MechanicRow.custom(
                    locale.text("Salvage.SubSkill.ArcaneSalvage.Stat", arcaneRank, 8)));
            boolean bypass = inspect && allowed(
                    player, PermissionNodes.SALVAGE_ENCHANT_BYPASS, false);
            if (settings.enchantLossEnabled()) {
                double full = bypass ? 100.0D
                        : settings.fullExtractionChance(arcaneRank, false);
                rows.add(new MechanicRow(
                        locale.text("Salvage.Arcane.ExtractFull"), percent(full)));
            }
            if (settings.enchantDowngradeEnabled()) {
                rows.add(new MechanicRow(
                        locale.text("Salvage.Arcane.ExtractPartial"),
                        percent(settings.partialExtractionChance(arcaneRank, false))));
            }
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
