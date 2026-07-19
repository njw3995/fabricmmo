package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.info.SkillPanelMechanicsProvider;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Upstream-shaped Woodcutting mechanic rows for the shared skill command panel. */
public final class WoodcuttingPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final WoodcuttingSettings settings;
    private final WoodcuttingDropSettings drops;
    private final FabricCommandPermissionService permissions =
            new FabricCommandPermissionService();

    public WoodcuttingPanelMechanicsProvider(
            WoodcuttingSettings settings,
            WoodcuttingDropSettings drops) {
        this(null, settings, drops);
    }

    public WoodcuttingPanelMechanicsProvider(
            MinecraftServer server,
            WoodcuttingSettings settings,
            WoodcuttingDropSettings drops) {
        this.server = server;
        this.settings = Objects.requireNonNull(settings, "settings");
        this.drops = Objects.requireNonNull(drops, "drops");
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

        if (!inspectPermissions || allowed(
                player, PermissionNodes.WOODCUTTING_TREE_FELLER, true)) {
            int baseDuration = settings.treeFellerDurationSeconds(level);
            rows.add(new MechanicRow(
                    "Tree Feller",
                    level >= settings.treeFellerUnlockLevel()
                            ? enduranceBonus > 0
                                    ? baseDuration + " seconds ("
                                            + (baseDuration + enduranceBonus)
                                            + "s with Endurance Perk)"
                                    : baseDuration + " seconds"
                            : "Locked until level " + settings.treeFellerUnlockLevel()));
        }
        if (!inspectPermissions || allowed(
                player, PermissionNodes.WOODCUTTING_HARVEST_LUMBER, true)) {
            rows.add(new MechanicRow(
                    "Harvest Lumber",
                    drops.harvestLumberUnlocked(level, settings.progressionMode())
                            ? chance(
                                    level,
                                    drops.harvestLumberMaxLevel(settings.progressionMode()),
                                    drops.harvestLumberChanceMaxPercent(),
                                    lucky)
                            : "Locked until level " + unlock(
                                    settings.progressionMode(),
                                    drops.harvestLumberUnlockStandard(),
                                    drops.harvestLumberUnlockRetro())));
        }
        if (!inspectPermissions || allowed(
                player, PermissionNodes.WOODCUTTING_CLEAN_CUTS, true)) {
            rows.add(new MechanicRow(
                    "Clean Cuts",
                    drops.cleanCutsUnlocked(level, settings.progressionMode())
                            ? chance(
                                    level,
                                    drops.cleanCutsMaxLevel(settings.progressionMode()),
                                    drops.cleanCutsChanceMaxPercent(),
                                    lucky)
                            : "Locked until level " + unlock(
                                    settings.progressionMode(),
                                    drops.cleanCutsUnlockStandard(),
                                    drops.cleanCutsUnlockRetro())));
        }
        if (!inspectPermissions || allowed(
                player, PermissionNodes.WOODCUTTING_KNOCK_ON_WOOD, true)) {
            rows.add(new MechanicRow(
                    "Knock on Wood",
                    level >= settings.knockOnWoodRankTwoLevel()
                            ? "Rank 2: standard tree loot and experience orbs"
                            : level >= settings.knockOnWoodRankOneLevel()
                                    ? "Rank 1: standard tree loot"
                                    : "Locked until level "
                                            + settings.knockOnWoodRankOneLevel()));
        }
        if (!inspectPermissions || allowed(
                player, PermissionNodes.WOODCUTTING_LEAF_BLOWER, true)) {
            rows.add(new MechanicRow(
                    "Leaf Blower",
                    level >= settings.leafBlowerUnlockLevel()
                            ? "Blow away leaves"
                            : "Locked until level " + settings.leafBlowerUnlockLevel()));
        }
        return List.copyOf(rows);
    }

    private boolean allowed(
            ServerPlayerEntity player,
            String permission,
            boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static int unlock(
            ProgressionMode mode,
            int standard,
            int retro) {
        return mode == ProgressionMode.RETRO ? retro : standard;
    }

    private static String chance(
            int level,
            int maxLevel,
            double maximum,
            boolean lucky) {
        double base = WoodcuttingProbability.chance(level, maxLevel, maximum, false);
        if (!lucky) {
            return percent(base);
        }
        double luckyChance = WoodcuttingProbability.chance(level, maxLevel, maximum, true);
        return percent(base) + " (" + percent(luckyChance) + " with Lucky Perk)";
    }

    private static String percent(double probability) {
        return String.format(Locale.ROOT, "%.2f%%", probability * 100.0D);
    }
}
