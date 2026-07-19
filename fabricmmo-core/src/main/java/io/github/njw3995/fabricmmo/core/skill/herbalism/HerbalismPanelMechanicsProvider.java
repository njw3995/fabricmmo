package io.github.njw3995.fabricmmo.core.skill.herbalism;

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

/** Exact mcMMO 2.3.000 HerbalismCommand.statsDisplay row ordering. */
public final class HerbalismPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final HerbalismSettings settings;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions =
            new FabricCommandPermissionService();

    public HerbalismPanelMechanicsProvider(
            MinecraftServer server,
            HerbalismSettings settings,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspectPermissions = player != null;
        boolean lucky = inspectPermissions && allowed(player, PermissionNodes.HERBALISM_LUCKY, false);
        int activationBonus = inspectPermissions
                ? HerbalismPerks.activationBonusSeconds(player.getCommandSource(), permissions)
                : 0;
        ArrayList<MechanicRow> rows = new ArrayList<>();

        if (level >= settings.doubleDropsUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.HERBALISM_DOUBLE_DROPS, true))) {
            rows.add(new MechanicRow(
                    locale.text("Herbalism.SubSkill.DoubleDrops.Stat"),
                    chance(settings.doubleDropsChance(level, false),
                            settings.doubleDropsChance(level, true), lucky)));
        }
        if (level >= settings.verdantBountyUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.HERBALISM_VERDANT_BOUNTY, true))) {
            rows.add(new MechanicRow(
                    locale.text("Herbalism.SubSkill.VerdantBounty.Stat"),
                    chance(settings.verdantBountyChance(level, false),
                            settings.verdantBountyChance(level, true), lucky)));
        }
        int dietRank = settings.farmersDietRank(level);
        if (dietRank > 0
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.HERBALISM_FARMERS_DIET, true))) {
            rows.add(MechanicRow.custom(locale.text(
                    "Herbalism.SubSkill.FarmersDiet.Stat", dietRank)));
        }
        if (level >= settings.greenTerraUnlockLevel()
                && (!inspectPermissions || allowed(
                        player, PermissionNodes.HERBALISM_GREEN_TERRA, true))) {
            int base = settings.greenTerraDurationSeconds(level);
            String value = Integer.toString(base);
            if (activationBonus > 0) {
                value += locale.text("Perks.ActivationTime.Bonus", base + activationBonus);
            }
            rows.add(new MechanicRow(
                    locale.text("Herbalism.SubSkill.GreenTerra.Stat"), value));
        }
        int greenThumbRank = settings.greenThumbRank(level);
        boolean canGreenThumbBlocks = !inspectPermissions || canGreenThumbBlock(player);
        boolean canGreenThumbPlants = !inspectPermissions || canGreenThumbPlant(player);
        if (greenThumbRank > 0 && (canGreenThumbBlocks || canGreenThumbPlants)) {
            rows.add(new MechanicRow(
                    locale.text("Herbalism.SubSkill.GreenThumb.Stat"),
                    chance(settings.greenThumbChance(level, false),
                            settings.greenThumbChance(level, true), lucky)));
            if (canGreenThumbPlants) {
                rows.add(MechanicRow.custom(locale.text(
                        "Herbalism.SubSkill.GreenThumb.Stat.Extra", greenThumbRank)));
            }
        }
        if (!inspectPermissions || allowed(player, PermissionNodes.HERBALISM_HYLIAN_LUCK, true)) {
            rows.add(new MechanicRow(
                    locale.text("Herbalism.SubSkill.HylianLuck.Stat"),
                    chance(settings.hylianLuckChance(level, false),
                            settings.hylianLuckChance(level, true), lucky)));
        }
        if (!inspectPermissions || allowed(player, PermissionNodes.HERBALISM_SHROOM_THUMB, true)) {
            rows.add(new MechanicRow(
                    locale.text("Herbalism.SubSkill.ShroomThumb.Stat"),
                    chance(settings.shroomThumbChance(level, false),
                            settings.shroomThumbChance(level, true), lucky)));
        }
        return List.copyOf(rows);
    }


    private boolean canGreenThumbBlock(ServerPlayerEntity player) {
        return List.of("dirt", "cobblestone", "cobblestone_wall", "dirt_path", "stone_bricks")
                .stream()
                .anyMatch(path -> allowed(
                        player, PermissionNodes.herbalismGreenThumbBlock(path), true));
    }

    private boolean canGreenThumbPlant(ServerPlayerEntity player) {
        return List.of("wheat", "carrots", "potatoes", "beetroots", "nether_wart", "cocoa")
                .stream()
                .anyMatch(path -> allowed(
                        player, PermissionNodes.herbalismGreenThumbPlant(path), true));
    }

    private String chance(double base, double boosted, boolean lucky) {
        String value = formatPercent(base);
        if (lucky) {
            value += locale.text("Perks.Lucky.Bonus", formatPercent(boosted));
        }
        return value;
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
