package io.github.njw3995.fabricmmo.core.skill.fishing;

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

/** Exact mcMMO 2.3.000 Fishing statsDisplay rows. */
public final class FishingPanelMechanicsProvider implements SkillPanelMechanicsProvider {
    private final MinecraftServer server;
    private final FishingSettings settings;
    private final FishingTreasureRoller roller;
    private final FishingTreasureTable treasures;
    private final LocaleService locale;
    private final FabricCommandPermissionService permissions =
            new FabricCommandPermissionService();

    public FishingPanelMechanicsProvider(
            MinecraftServer server,
            FishingSettings settings,
            FishingTreasureTable treasures,
            LocaleService locale) {
        this.server = Objects.requireNonNull(server, "server");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.treasures = Objects.requireNonNull(treasures, "treasures");
        this.roller = new FishingTreasureRoller(treasures);
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    @Override
    public List<MechanicRow> rows(UUID playerId, int level) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        boolean inspect = player != null;
        boolean lucky = inspect && allowed(player, PermissionNodes.FISHING_LUCKY, false);
        ArrayList<MechanicRow> rows = new ArrayList<>();

        int dietRank = settings.fishermansDietRank(level);
        if (dietRank > 0 && (!inspect || allowed(
                player, PermissionNodes.FISHING_FISHERMANS_DIET, true))) {
            rows.add(MechanicRow.custom(locale.text(
                    "Fishing.SubSkill.FishermansDiet.Stat", dietRank)));
        }
        if (level >= settings.iceFishingUnlockLevel()
                && (!inspect || allowed(player, PermissionNodes.FISHING_ICE_FISHING, true))) {
            rows.add(new MechanicRow(
                    locale.text("Fishing.SubSkill.IceFishing.Stat"),
                    locale.text("Fishing.SubSkill.IceFishing.Stat")));
        }

        int tier = settings.treasureHunterRank(level);
        boolean treasureAllowed = tier > 0
                && (!inspect || allowed(player, PermissionNodes.FISHING_TREASURE_HUNTER, true));
        if (treasureAllowed
                && level >= settings.magicHunterUnlockLevel()
                && (!inspect || allowed(player, PermissionNodes.FISHING_MAGIC_HUNTER, true))) {
            rows.add(new MechanicRow(
                    locale.text("Fishing.SubSkill.MagicHunter.Stat"),
                    percent(roller.totalEnchantmentChance(tier))));
        }

        int masterRank = settings.masterAnglerRank(level);
        if (masterRank > 0
                && (!inspect || allowed(player, PermissionNodes.FISHING_MASTER_ANGLER, true))) {
            FishingSettings.WaitBounds bounds = settings.masterAnglerBounds(level, false, 0);
            rows.addAll(masterAnglerRows(locale, bounds));
        }

        int shakeRank = settings.shakeRank(level);
        if (shakeRank > 0
                && (!inspect || allowed(player, PermissionNodes.FISHING_SHAKE, true))) {
            String value = percent(settings.shakeChance(level, false));
            if (lucky) {
                value += locale.text("Perks.Lucky.Bonus",
                        percent(settings.shakeChance(level, true)));
            }
            rows.add(new MechanicRow(locale.text("Fishing.SubSkill.Shake.Stat"), value));
        }

        if (treasureAllowed) {
            rows.add(MechanicRow.custom(locale.text(
                    "Fishing.SubSkill.TreasureHunter.Stat", tier, 8)));
            rows.add(MechanicRow.custom(locale.text(
                    "Fishing.SubSkill.TreasureHunter.Stat.Extra",
                    percent(treasures.itemRate(tier, FishingRarity.COMMON)),
                    percent(treasures.itemRate(tier, FishingRarity.UNCOMMON)),
                    percent(treasures.itemRate(tier, FishingRarity.RARE)),
                    percent(treasures.itemRate(tier, FishingRarity.EPIC)),
                    percent(treasures.itemRate(tier, FishingRarity.LEGENDARY)),
                    percent(treasures.itemRate(tier, FishingRarity.MYTHIC)))));
        }
        return List.copyOf(rows);
    }

    static List<MechanicRow> masterAnglerRows(
            LocaleService locale,
            FishingSettings.WaitBounds bounds) {
        return List.of(
                MechanicRow.custom(locale.text(
                        "Fishing.SubSkill.MasterAngler.Stat",
                        seconds(bounds.minimumReductionTicks()))),
                MechanicRow.custom(locale.text(
                        "Fishing.SubSkill.MasterAngler.Stat.Extra",
                        seconds(bounds.maximumReductionTicks()))));
    }

    private boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return permissions.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static String percent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.US, "%.1f", ticks / 20.0D);
    }
}
