package io.github.njw3995.fabricmmo.core.skill.woodcutting;

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

/** Exact mcMMO 2.3.000 Woodcutting statsDisplay rows. */
public final class WoodcuttingPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final WoodcuttingSettings settings;
    private final WoodcuttingDropSettings drops;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions =
            new FabricCommandPermissionService();

    public WoodcuttingPanelMechanicsProvider(
            WoodcuttingSettings settings,
            WoodcuttingDropSettings drops,
            LocaleService locale) {
        this(null, settings, drops, locale);
    }

    public WoodcuttingPanelMechanicsProvider(
            MinecraftServer server,
            WoodcuttingSettings settings,
            WoodcuttingDropSettings drops,
            LocaleService locale) {
        this.server = server;
        this.settings = Objects.requireNonNull(settings, "settings");
        this.drops = Objects.requireNonNull(drops, "drops");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server == null
                ? null : server.getPlayerManager().getPlayer(playerId);
        boolean inspectPermissions = player != null;
        boolean lucky = inspectPermissions && permissions.hasPermission(
                player.getCommandSource(), PermissionNodes.WOODCUTTING_LUCKY, false);
        int enduranceBonus = inspectPermissions
                ? WoodcuttingPerks.activationBonusSeconds(
                        player.getCommandSource(), permissions)
                : 0;
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (drops.harvestLumberUnlocked(level, settings.progressionMode())
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.WOODCUTTING_HARVEST_LUMBER, true))) {
            rows.add(new MechanicRow(
                    locale.text("Woodcutting.SubSkill.HarvestLumber.Stat"),
                    chance(
                            level,
                            drops.harvestLumberMaxLevel(settings.progressionMode()),
                            drops.harvestLumberChanceMaxPercent(),
                            lucky)));
        }
        if (drops.cleanCutsUnlocked(level, settings.progressionMode())
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.WOODCUTTING_CLEAN_CUTS, true))) {
            rows.add(new MechanicRow(
                    locale.text("Woodcutting.SubSkill.CleanCuts.Stat"),
                    chance(
                            level,
                            drops.cleanCutsMaxLevel(settings.progressionMode()),
                            drops.cleanCutsChanceMaxPercent(),
                            lucky)));
        }
        boolean treeFellerUnlocked = level >= settings.treeFellerUnlockLevel();
        if (treeFellerUnlocked
                && level >= settings.knockOnWoodRankOneLevel()
                && (!inspectPermissions || (allowed(
                        player, PermissionNodes.WOODCUTTING_TREE_FELLER, true)
                        && allowed(player, PermissionNodes.WOODCUTTING_KNOCK_ON_WOOD, true)))) {
            String lootKey = level >= settings.knockOnWoodRankTwoLevel()
                    ? "Woodcutting.SubSkill.KnockOnWood.Loot.Rank2"
                    : "Woodcutting.SubSkill.KnockOnWood.Loot.Normal";
            rows.add(new MechanicRow(
                    locale.text("Woodcutting.SubSkill.KnockOnWood.Stat"),
                    locale.text(lootKey)));
        }
        if (level >= settings.leafBlowerUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.WOODCUTTING_LEAF_BLOWER, true))) {
            rows.add(new MechanicRow(
                    locale.text("Woodcutting.Ability.0"),
                    locale.text("Woodcutting.Ability.1")));
        }
        if (treeFellerUnlocked
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.WOODCUTTING_TREE_FELLER, true))) {
            int baseDuration = settings.treeFellerDurationSeconds(level);
            String value = Integer.toString(baseDuration);
            if (enduranceBonus > 0) {
                value += locale.text("Perks.ActivationTime.Bonus", baseDuration + enduranceBonus);
            }
            rows.add(new MechanicRow(
                    locale.text("Woodcutting.SubSkill.TreeFeller.Stat"),
                    value));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(
            ServerPlayerEntity player,
            String permission,
            boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private String chance(
            int level,
            int maxLevel,
            double maximum,
            boolean lucky) {
        double base = WoodcuttingProbability.chance(level, maxLevel, maximum, false);
        String value = formatProbability(base);
        if (lucky) {
            double luckyChance = WoodcuttingProbability.chance(level, maxLevel, maximum, true);
            value += locale.text("Perks.Lucky.Bonus", formatProbability(luckyChance));
        }
        return value;
    }

    private static String formatProbability(double probability) {
        return String.format(Locale.US, "%.2f%%", probability * 100.0D);
    }
}
