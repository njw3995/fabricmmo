package io.github.njw3995.fabricmmo.core.skill.maces;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Exact mcMMO 2.3.000 MacesCommand statsDisplay ordering and wording. */
public final class MacesPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final MacesSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public MacesPanelMechanicsProvider(
            MinecraftServer server,
            MacesSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.MACES_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.MACES_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Maces.SubSkill.MacesLimitBreak.Stat"),
                    Integer.toString(MacesDamage.limitBreakDamage(
                            settings.limitBreakRank(level), 1000))));
        }

        int crippleRank = settings.crippleRank(level);
        if (crippleRank > 0
                && (!inspect || allowed(player, PermissionNodes.MACES_CRIPPLE, true))) {
            double normal = settings.crippleChances()[crippleRank - 1];
            String chance = Double.toString(normal) + "%";
            if (lucky) {
                chance += locale.text(
                        "Perks.Lucky.Bonus",
                        Double.toString(normal * MacesProbability.PANEL_LUCKY_MULTIPLIER));
            }
            rows.add(new MechanicRow(locale.text("Maces.SubSkill.Cripple.Stat"), chance));
            rows.add(MechanicRow.custom(locale.text(
                    "Maces.SubSkill.Cripple.Stat.Extra",
                    Double.toString(MacesSettings.crippleDurationTicks(true) / 20.0D),
                    Double.toString(MacesSettings.crippleDurationTicks(false) / 20.0D))));
        }

        if (settings.crushRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.MACES_CRUSH, true))) {
            rows.add(new MechanicRow(
                    locale.text("Maces.SubSkill.Crush.Stat"),
                    Double.toString(settings.crushDamage(level))));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }
}
