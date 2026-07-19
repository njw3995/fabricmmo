package io.github.njw3995.fabricmmo.core.skill.excavation;

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

/** Exact mcMMO 2.3.000 Excavation statsDisplay rows. */
public final class ExcavationPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final ExcavationSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions =
            new FabricCommandPermissionService();

    public ExcavationPanelMechanicsProvider(
            MinecraftServer server,
            ExcavationAbilityController abilities,
            ExcavationSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        Objects.requireNonNull(abilities, "abilities");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspectPermissions = player != null;
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (level >= settings.gigaDrillUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.EXCAVATION_GIGA_DRILL_BREAKER, true))) {
            int baseDuration = settings.gigaDrillDurationSeconds(level);
            int bonus = player == null ? 0 : ExcavationPerks.activationBonusSeconds(
                    player.getCommandSource(), permissions);
            String value = Integer.toString(baseDuration);
            if (bonus > 0) {
                value += locale.text("Perks.ActivationTime.Bonus", baseDuration + bonus);
            }
            rows.add(new MechanicRow(
                    locale.text("Excavation.SubSkill.GigaDrillBreaker.Stat"),
                    value));
        }

        int archaeologyRank = settings.archaeologyRank(level);
        if (archaeologyRank > 0
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.EXCAVATION_ARCHAEOLOGY, true))) {
            rows.add(new MechanicRow(
                    locale.text("Excavation.SubSkill.Archaeology.Stat"),
                    formatPercent(settings.archaeologyOrbChancePercent(level))));
            rows.add(new MechanicRow(
                    locale.text("Excavation.SubSkill.Archaeology.Stat.Extra"),
                    Integer.toString(settings.archaeologyOrbAmount(level))));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String permission, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static String formatPercent(double percentage) {
        return String.format(Locale.US, "%.2f%%", percentage);
    }
}
