package io.github.njw3995.fabricmmo.core.party;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 party and party-teleport settings. */
public record PartySettings(
        int maxSize,
        double shareRange,
        double xpShareBonusBase,
        double xpShareBonusIncrease,
        double xpShareBonusCap,
        int levelCap,
        int xpCurveModifier,
        boolean nearMembersNeeded,
        boolean informAllMembersOnLevelUp,
        Map<PartyFeature, Integer> unlockLevels,
        Duration ptpCooldown,
        Duration ptpWarmup,
        Duration ptpRecentlyHurtCooldown,
        Duration ptpRequestTimeout,
        boolean ptpAcceptRequired,
        boolean ptpWorldPermissions,
        double inspectMaxDistance,
        boolean levelUpSounds) {
    public PartySettings {
        if (maxSize == 0 || maxSize < -1) {
            throw new IllegalArgumentException("maxSize must be -1 or positive");
        }
        requirePositiveFinite(shareRange, "shareRange");
        requirePositiveFinite(xpShareBonusBase, "xpShareBonusBase");
        if (!Double.isFinite(xpShareBonusIncrease) || xpShareBonusIncrease < 0.0D) {
            throw new IllegalArgumentException("xpShareBonusIncrease must be finite and non-negative");
        }
        requirePositiveFinite(xpShareBonusCap, "xpShareBonusCap");
        if (levelCap <= 0 || xpCurveModifier < 1) {
            throw new IllegalArgumentException("levelCap and xpCurveModifier must be positive");
        }
        EnumMap<PartyFeature, Integer> normalized = new EnumMap<>(PartyFeature.class);
        normalized.putAll(Objects.requireNonNull(unlockLevels, "unlockLevels"));
        for (PartyFeature feature : PartyFeature.values()) {
            int level = normalized.getOrDefault(feature, 0);
            if (level < 0) {
                throw new IllegalArgumentException("Unlock level must be non-negative for " + feature);
            }
            normalized.put(feature, level);
        }
        unlockLevels = Map.copyOf(normalized);
        ptpCooldown = positiveOrZero(ptpCooldown, "ptpCooldown");
        ptpWarmup = positiveOrZero(ptpWarmup, "ptpWarmup");
        ptpRecentlyHurtCooldown = positiveOrZero(ptpRecentlyHurtCooldown, "ptpRecentlyHurtCooldown");
        ptpRequestTimeout = positiveOrZero(ptpRequestTimeout, "ptpRequestTimeout");
        if (!Double.isFinite(inspectMaxDistance) || inspectMaxDistance < 0.0D) {
            throw new IllegalArgumentException("inspectMaxDistance must be finite and non-negative");
        }
    }

    public static PartySettings load(Path configFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        EnumMap<PartyFeature, Integer> unlocks = new EnumMap<>(PartyFeature.class);
        unlocks.put(PartyFeature.CHAT, config.integer("Party.Leveling.Chat_UnlockLevel", 1));
        unlocks.put(PartyFeature.TELEPORT, config.integer("Party.Leveling.Teleport_UnlockLevel", 2));
        unlocks.put(PartyFeature.ALLIANCE, config.integer("Party.Leveling.Alliance_UnlockLevel", 5));
        unlocks.put(PartyFeature.ITEM_SHARE, config.integer("Party.Leveling.ItemShare_UnlockLevel", 8));
        unlocks.put(PartyFeature.XP_SHARE, config.integer("Party.Leveling.XpShare_UnlockLevel", 10));
        return new PartySettings(
                config.integer("Party.MaxSize", -1),
                config.decimal("Party.Sharing.Range", 75.0D),
                config.decimal("Party.Sharing.ExpShare_bonus_base", 1.1D),
                config.decimal("Party.Sharing.ExpShare_bonus_increase", 1.05D),
                config.decimal("Party.Sharing.ExpShare_bonus_cap", 1.5D),
                normalizeCap(config.integer("Party.Leveling.Level_Cap", 10)),
                config.integer("Party.Leveling.Xp_Curve_Modifier", 3),
                config.bool("Party.Leveling.Near_Members_Needed", false),
                config.bool("Party.Leveling.Inform_All_Party_Members_On_LevelUp", false),
                unlocks,
                Duration.ofSeconds(config.integer("Commands.ptp.Cooldown", 120)),
                Duration.ofSeconds(config.integer("Commands.ptp.Warmup", 5)),
                Duration.ofSeconds(config.integer("Commands.ptp.RecentlyHurt_Cooldown", 60)),
                Duration.ofSeconds(config.integer("Commands.ptp.Request_Timeout", 300)),
                config.bool("Commands.ptp.Accept_Required", true),
                config.bool("Commands.ptp.World_Based_Permissions", false),
                config.decimal("Commands.inspect.Max_Distance", 30.0D),
                config.bool("General.LevelUp_Sounds", true));
    }

    public static PartySettings upstreamDefaults() {
        EnumMap<PartyFeature, Integer> unlocks = new EnumMap<>(PartyFeature.class);
        unlocks.put(PartyFeature.CHAT, 1);
        unlocks.put(PartyFeature.TELEPORT, 2);
        unlocks.put(PartyFeature.ALLIANCE, 5);
        unlocks.put(PartyFeature.ITEM_SHARE, 8);
        unlocks.put(PartyFeature.XP_SHARE, 10);
        return new PartySettings(
                -1, 75.0D, 1.1D, 1.05D, 1.5D, 10, 3, false, false, unlocks,
                Duration.ofSeconds(120), Duration.ofSeconds(5), Duration.ofSeconds(60),
                Duration.ofSeconds(300), true, false, 30.0D, true);
    }

    public int unlockLevel(PartyFeature feature) {
        return unlockLevels.getOrDefault(feature, 0);
    }

    public boolean unlocked(PartyState party, PartyFeature feature) {
        return party.level() >= unlockLevel(feature);
    }

    private static int normalizeCap(int configured) {
        return configured <= 0 ? Integer.MAX_VALUE : configured;
    }

    private static Duration positiveOrZero(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return duration;
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
