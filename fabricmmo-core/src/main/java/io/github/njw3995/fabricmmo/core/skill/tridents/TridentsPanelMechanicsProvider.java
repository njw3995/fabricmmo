package io.github.njw3995.fabricmmo.core.skill.tridents;

import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedDamage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Exact mcMMO 2.3.000 TridentsCommand statsDisplay ordering and wording. */
public final class TridentsPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final TridentsSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions = new FabricCommandPermissionService();

    public TridentsPanelMechanicsProvider(
            MinecraftServer server,
            TridentsSettings settings,
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

        if (settings.limitBreakRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.TRIDENTS_LIMIT_BREAK, true))) {
            rows.add(new MechanicRow(
                    locale.text("Tridents.SubSkill.TridentsLimitBreak.Stat"),
                    Integer.toString(RangedDamage.limitBreakDamage(
                            settings.limitBreakRank(level), 1000))));
        }
        if (settings.impaleRank(level) > 0
                && (!inspect || allowed(player, PermissionNodes.TRIDENTS_IMPALE, true))) {
            rows.add(new MechanicRow(
                    locale.text("Tridents.SubSkill.Impale.Stat"),
                    Double.toString(settings.impaleDamage(level))));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }
}
