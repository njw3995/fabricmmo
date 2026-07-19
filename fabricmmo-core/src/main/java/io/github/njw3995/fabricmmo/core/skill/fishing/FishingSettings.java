package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** mcMMO 2.3.000 Fishing rank, treasure, wait-time, and exploit settings. */
public record FishingSettings(
        ProgressionMode progressionMode,
        boolean dropsEnabled,
        boolean overrideVanillaTreasures,
        boolean extraFish,
        double lureModifierPercent,
        boolean allowConflictingEnchants,
        boolean exploitFixEnabled,
        int exploitMoveRange,
        int overFishLimit,
        int[] treasureHunterStandard,
        int[] treasureHunterRetro,
        int[] masterAnglerStandard,
        int[] masterAnglerRetro,
        int[] shakeStandard,
        int[] shakeRetro,
        int[] fishermansDietStandard,
        int[] fishermansDietRetro,
        int magicHunterStandard,
        int magicHunterRetro,
        int iceFishingStandard,
        int iceFishingRetro,
        double[] shakeChanceByRank,
        int[] vanillaXpMultiplierByRank,
        int masterAnglerMinReductionPerRank,
        int masterAnglerMaxReductionPerRank,
        int boatMinReduction,
        int boatMaxReduction,
        int minWaitCap,
        int maxWaitCap,
        int shakeXp) {

    public FishingSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        treasureHunterStandard = ordered(treasureHunterStandard, 8, "Treasure Hunter");
        treasureHunterRetro = ordered(treasureHunterRetro, 8, "Treasure Hunter");
        masterAnglerStandard = ordered(masterAnglerStandard, 8, "Master Angler");
        masterAnglerRetro = ordered(masterAnglerRetro, 8, "Master Angler");
        shakeStandard = ordered(shakeStandard, 8, "Shake");
        shakeRetro = ordered(shakeRetro, 8, "Shake");
        fishermansDietStandard = ordered(fishermansDietStandard, 5, "Fisherman's Diet");
        fishermansDietRetro = ordered(fishermansDietRetro, 5, "Fisherman's Diet");
        shakeChanceByRank = copy(shakeChanceByRank, 8, "Shake chances");
        vanillaXpMultiplierByRank = copy(vanillaXpMultiplierByRank, 8, "Vanilla XP multipliers");
        if (exploitMoveRange < 1 || overFishLimit < 1) {
            throw new IllegalArgumentException("Fishing exploit limits must be positive");
        }
        if (minWaitCap < 0 || maxWaitCap < minWaitCap + 40) {
            throw new IllegalArgumentException("Fishing wait caps are inconsistent");
        }
    }

    public static FishingSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path experienceFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        ProgressionMode mode = config.bool("General.RetroMode.Enabled", true)
                ? ProgressionMode.RETRO : ProgressionMode.STANDARD;
        return new FishingSettings(
                mode,
                config.bool("Skills.Fishing.Drops_Enabled", true),
                config.bool("Skills.Fishing.Override_Vanilla_Treasures", true),
                config.bool("Skills.Fishing.Extra_Fish", false),
                config.decimal("Skills.Fishing.Lure_Modifier", 4.0D),
                config.bool("Skills.Fishing.Allow_Conflicting_Enchants", false),
                experience.bool("ExploitFix.Fishing", true),
                experience.integer("Fishing_ExploitFix_Options.MoveRange", 3),
                experience.integer("Fishing_ExploitFix_Options.OverFishLimit", 10),
                ranks(ranks, "Fishing.TreasureHunter.Standard.Rank_", 8,
                        new int[] {1, 25, 35, 50, 65, 75, 85, 100}),
                ranks(ranks, "Fishing.TreasureHunter.RetroMode.Rank_", 8,
                        new int[] {1, 250, 350, 500, 650, 750, 850, 1000}),
                ranks(ranks, "Fishing.MasterAngler.Standard.Rank_", 8,
                        new int[] {1, 20, 30, 40, 60, 70, 80, 90}),
                ranks(ranks, "Fishing.MasterAngler.RetroMode.Rank_", 8,
                        new int[] {1, 200, 300, 400, 600, 700, 800, 900}),
                ranks(ranks, "Fishing.Shake.Standard.Rank_", 8,
                        new int[] {15, 20, 25, 30, 40, 50, 60, 70}),
                ranks(ranks, "Fishing.Shake.RetroMode.Rank_", 8,
                        new int[] {150, 200, 250, 300, 400, 500, 600, 700}),
                ranks(ranks, "Fishing.FishermansDiet.Standard.Rank_", 5,
                        new int[] {20, 40, 60, 80, 100}),
                ranks(ranks, "Fishing.FishermansDiet.RetroMode.Rank_", 5,
                        new int[] {200, 400, 600, 800, 1000}),
                ranks.integer("Fishing.MagicHunter.Standard.Rank_1", 20),
                ranks.integer("Fishing.MagicHunter.RetroMode.Rank_1", 200),
                ranks.integer("Fishing.IceFishing.Standard.Rank_1", 5),
                ranks.integer("Fishing.IceFishing.RetroMode.Rank_1", 50),
                decimals(advanced, "Skills.Fishing.ShakeChance.Rank_", 8,
                        new double[] {15, 20, 25, 35, 45, 55, 65, 75}),
                ranks(advanced, "Skills.Fishing.VanillaXPMultiplier.Rank_", 8,
                        new int[] {1, 2, 3, 3, 4, 4, 5, 5}),
                advanced.integer("Skills.Fishing.MasterAngler.Tick_Reduction_Per_Rank.Min_Wait", 10),
                advanced.integer("Skills.Fishing.MasterAngler.Tick_Reduction_Per_Rank.Max_Wait", 30),
                advanced.integer("Skills.Fishing.MasterAngler.Boat_Tick_Reduction.Min_Wait", 10),
                advanced.integer("Skills.Fishing.MasterAngler.Boat_Tick_Reduction.Max_Wait", 30),
                Math.max(0, advanced.integer(
                        "Skills.Fishing.MasterAngler.Tick_Reduction_Caps.Min_Wait", 40)),
                advanced.integer(
                        "Skills.Fishing.MasterAngler.Tick_Reduction_Caps.Max_Wait", 100),
                experience.integer("Experience_Values.Fishing.Shake", 50));
    }

    public int treasureHunterRank(int level) {
        return rank(level, mode(treasureHunterStandard, treasureHunterRetro));
    }

    public int masterAnglerRank(int level) {
        return rank(level, mode(masterAnglerStandard, masterAnglerRetro));
    }

    public int shakeRank(int level) {
        return rank(level, mode(shakeStandard, shakeRetro));
    }

    public int fishermansDietRank(int level) {
        return rank(level, mode(fishermansDietStandard, fishermansDietRetro));
    }

    public int magicHunterUnlockLevel() {
        return mode(magicHunterStandard, magicHunterRetro);
    }

    public int iceFishingUnlockLevel() {
        return mode(iceFishingStandard, iceFishingRetro);
    }

    public double shakeChance(int level, boolean lucky) {
        int rank = shakeRank(level);
        return rank <= 0 ? 0.0D
                : FishingProbability.luckyPercent(shakeChanceByRank[rank - 1], lucky);
    }

    public int vanillaXpMultiplier(int level) {
        int rank = treasureHunterRank(level);
        return rank <= 0 ? 1 : vanillaXpMultiplierByRank[rank - 1];
    }

    public WaitBounds masterAnglerBounds(int level, boolean inBoat, int lureReductionTicks) {
        int rank = masterAnglerRank(level);
        int minReduction = rank * masterAnglerMinReductionPerRank
                + (inBoat ? boatMinReduction : 0);
        int maxReduction = rank * masterAnglerMaxReductionPerRank
                + (inBoat ? boatMaxReduction : 0)
                + Math.max(0, lureReductionTicks);
        int minimum = Math.max(minWaitCap, 100 - minReduction);
        int maximum = Math.max(Math.max(maxWaitCap, minWaitCap + 40), 600 - maxReduction);
        if (maximum < minimum) {
            maximum = minimum + 100;
        }
        return new WaitBounds(minimum, maximum, minReduction, maxReduction);
    }

    private int[] mode(int[] standard, int[] retro) {
        return progressionMode == ProgressionMode.RETRO ? retro : standard;
    }

    private int mode(int standard, int retro) {
        return progressionMode == ProgressionMode.RETRO ? retro : standard;
    }

    private static int rank(int level, int[] thresholds) {
        int result = 0;
        for (int index = 0; index < thresholds.length; index++) {
            if (level >= thresholds[index]) {
                result = index + 1;
            }
        }
        return result;
    }

    private static int[] ranks(
            FlatYamlConfig config,
            String prefix,
            int count,
            int[] defaults) {
        int[] result = new int[count];
        for (int index = 0; index < count; index++) {
            result[index] = config.integer(prefix + (index + 1), defaults[index]);
        }
        return result;
    }

    private static double[] decimals(
            FlatYamlConfig config,
            String prefix,
            int count,
            double[] defaults) {
        double[] result = new double[count];
        for (int index = 0; index < count; index++) {
            result[index] = config.decimal(prefix + (index + 1), defaults[index]);
        }
        return result;
    }

    private static int[] ordered(int[] values, int count, String name) {
        int[] result = copy(values, count, name);
        int previous = -1;
        for (int value : result) {
            if (value < 0 || value < previous) {
                throw new IllegalArgumentException(name + " ranks must be non-negative and ordered");
            }
            previous = value;
        }
        return result;
    }

    private static int[] copy(int[] values, int count, String name) {
        Objects.requireNonNull(values, name);
        if (values.length != count) {
            throw new IllegalArgumentException(name + " must have " + count + " values");
        }
        return values.clone();
    }

    private static double[] copy(double[] values, int count, String name) {
        Objects.requireNonNull(values, name);
        if (values.length != count) {
            throw new IllegalArgumentException(name + " must have " + count + " values");
        }
        return values.clone();
    }

    public record WaitBounds(
            int minimumTicks,
            int maximumTicks,
            int minimumReductionTicks,
            int maximumReductionTicks) {
    }
}
