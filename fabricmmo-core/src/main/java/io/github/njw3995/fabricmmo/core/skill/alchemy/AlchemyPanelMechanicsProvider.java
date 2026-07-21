package io.github.njw3995.fabricmmo.core.skill.alchemy;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.locale.LocaleService;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Exact mcMMO 2.3.000 Alchemy statsDisplay rows. */
public final class AlchemyPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final AlchemySettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions =
            new FabricCommandPermissionService();

    public AlchemyPanelMechanicsProvider(
            MinecraftServer server,
            AlchemySettings settings,
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
        if (!inspect || allowed(player, PermissionNodes.ALCHEMY_CATALYSIS, true)) {
            double normal = AlchemyFormula.catalysisSpeed(
                    level,
                    settings.catalysisUnlock(),
                    settings.catalysisMaxBonusLevel(),
                    settings.catalysisMinSpeed(),
                    settings.catalysisMaxSpeed(),
                    false);
            String value = multiplier(normal);
            if (inspect && allowed(player, PermissionNodes.ALCHEMY_LUCKY, false)) {
                double lucky = AlchemyFormula.catalysisSpeed(
                        level,
                        settings.catalysisUnlock(),
                        settings.catalysisMaxBonusLevel(),
                        settings.catalysisMinSpeed(),
                        settings.catalysisMaxSpeed(),
                        true);
                value += locale.text("Perks.Lucky.Bonus", multiplier(lucky));
            }
            rows.add(new MechanicRow(locale.text("Alchemy.SubSkill.Catalysis.Stat"), value));
        }
        if (!inspect || allowed(player, PermissionNodes.ALCHEMY_CONCOCTIONS, true)) {
            int tier = settings.concoctionsTier(level);
            List<Identifier> ingredients = List.copyOf(
                    FabricMmoFabricRuntime.alchemyPotionConfig().ingredientsForTier(tier));
            rows.add(MechanicRow.custom(locale.text(
                    "Alchemy.SubSkill.Concoctions.Stat", tier, 8)));
            rows.add(MechanicRow.custom(locale.text(
                    "Alchemy.SubSkill.Concoctions.Stat.Extra",
                    ingredients.size(),
                    ingredients.stream().map(AlchemyPanelMechanicsProvider::ingredientName)
                            .collect(Collectors.joining(", ")))));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static String multiplier(double value) {
        return String.format(Locale.US, "%.2fx", value);
    }

    private static String ingredientName(Identifier id) {
        if (!Registries.ITEM.containsId(id)) return id.toString();
        return Registries.ITEM.get(id).getName().getString();
    }
}
