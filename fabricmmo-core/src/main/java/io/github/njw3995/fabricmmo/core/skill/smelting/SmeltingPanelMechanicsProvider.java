package io.github.njw3995.fabricmmo.core.skill.smelting;

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

/** Exact mcMMO 2.3.000 SmeltingCommand statsDisplay ordering and values. */
public final class SmeltingPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final SmeltingSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public SmeltingPanelMechanicsProvider(
            MinecraftServer server,
            SmeltingSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.SMELTING_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>(3);
        int fuelRank = settings.fuelEfficiencyRank(level);
        if (fuelRank > 0
                && (!inspect || allowed(player, PermissionNodes.SMELTING_FUEL_EFFICIENCY, true))) {
            rows.add(MechanicRow.custom(locale.text(
                    "Smelting.SubSkill.FuelEfficiency.Stat",
                    SmeltingFormula.fuelMultiplier(fuelRank))));
        }
        if (!inspect || allowed(player, PermissionNodes.SMELTING_SECOND_SMELT, true)) {
            double normal = settings.secondSmeltChance(level, false);
            String chance = percent(normal);
            if (lucky) {
                chance += locale.text(
                        "Perks.Lucky.Bonus", percent(settings.secondSmeltChance(level, true)));
            }
            rows.add(new MechanicRow(
                    locale.text("Smelting.SubSkill.SecondSmelt.Stat"), chance));
        }
        if (settings.understandingRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.SMELTING_VANILLA_XP, true))) {
            rows.add(MechanicRow.custom(locale.text(
                    "Smelting.SubSkill.UnderstandingTheArt.Stat",
                    settings.vanillaXpMultiplier(level))));
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
