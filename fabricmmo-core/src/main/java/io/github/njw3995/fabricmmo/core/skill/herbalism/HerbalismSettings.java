package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** mcMMO 2.3.000 Herbalism ability, rank, probability, and replant settings. */
public record HerbalismSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int greenTerraCooldownSeconds,
        int greenTerraMaximumSeconds,
        int greenTerraUnlockStandard,
        int greenTerraUnlockRetro,
        int[] greenThumbStandard,
        int[] greenThumbRetro,
        int[] farmersDietStandard,
        int[] farmersDietRetro,
        int doubleDropsUnlockStandard,
        int doubleDropsUnlockRetro,
        int verdantBountyUnlockStandard,
        int verdantBountyUnlockRetro,
        double greenThumbChanceMax,
        int greenThumbMaxStandard,
        int greenThumbMaxRetro,
        double doubleDropsChanceMax,
        int doubleDropsMaxStandard,
        int doubleDropsMaxRetro,
        double verdantBountyChanceMax,
        int verdantBountyMaxStandard,
        int verdantBountyMaxRetro,
        double hylianLuckChanceMax,
        int hylianLuckMaxStandard,
        int hylianLuckMaxRetro,
        double shroomThumbChanceMax,
        int shroomThumbMaxStandard,
        int shroomThumbMaxRetro,
        boolean replantCarrots,
        boolean replantWheat,
        boolean replantNetherWart,
        boolean replantPotatoes,
        boolean replantBeetroots,
        boolean replantCocoa,
        boolean replantSweetBerryBush,
        boolean preventAfkLeveling,
        boolean limitXpOnTallPlants) {

    public HerbalismSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        greenThumbStandard = copyRanks(greenThumbStandard, 4, "Green Thumb");
        greenThumbRetro = copyRanks(greenThumbRetro, 4, "Green Thumb");
        farmersDietStandard = copyRanks(farmersDietStandard, 5, "Farmer's Diet");
        farmersDietRetro = copyRanks(farmersDietRetro, 5, "Farmer's Diet");
    }

    public static HerbalismSettings load(
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
        return new HerbalismSettings(
                mode,
                config.bool("Abilities.Enabled", true),
                config.bool("Abilities.Messages", true),
                advanced.bool("Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.bool("Abilities.Activation.Only_Activate_When_Sneaking", false),
                advanced.integer("Skills.General.Ability.Length.Standard.CapLevel", 100),
                advanced.integer("Skills.General.Ability.Length.RetroMode.CapLevel", 1000),
                advanced.integer("Skills.General.Ability.Length.Standard.IncreaseLevel", 5),
                advanced.integer("Skills.General.Ability.Length.RetroMode.IncreaseLevel", 50),
                config.integer("Abilities.Cooldowns.Green_Terra", 240),
                config.integer("Abilities.Max_Seconds.Green_Terra", 0),
                ranks.integer("Herbalism.GreenTerra.Standard.Rank_1", 5),
                ranks.integer("Herbalism.GreenTerra.RetroMode.Rank_1", 50),
                rankArray(ranks, "Herbalism.GreenThumb.Standard.Rank_", new int[] {25, 50, 75, 100}),
                rankArray(ranks, "Herbalism.GreenThumb.RetroMode.Rank_", new int[] {250, 500, 750, 1000}),
                rankArray(ranks, "Herbalism.FarmersDiet.Standard.Rank_", new int[] {20, 40, 60, 80, 100}),
                rankArray(ranks, "Herbalism.FarmersDiet.RetroMode.Rank_", new int[] {200, 400, 600, 800, 1000}),
                ranks.integer("Herbalism.DoubleDrops.Standard.Rank_1", 1),
                ranks.integer("Herbalism.DoubleDrops.RetroMode.Rank_1", 1),
                ranks.integer("Herbalism.VerdantBounty.Standard.Rank_1", 100),
                ranks.integer("Herbalism.VerdantBounty.RetroMode.Rank_1", 1000),
                advanced.decimal("Skills.Herbalism.GreenThumb.ChanceMax", 100.0D),
                advanced.integer("Skills.Herbalism.GreenThumb.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Herbalism.GreenThumb.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Herbalism.DoubleDrops.ChanceMax", 100.0D),
                advanced.integer("Skills.Herbalism.DoubleDrops.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Herbalism.DoubleDrops.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Herbalism.VerdantBounty.ChanceMax", 50.0D),
                advanced.integer("Skills.Herbalism.VerdantBounty.MaxBonusLevel.Standard", 1000),
                advanced.integer("Skills.Herbalism.VerdantBounty.MaxBonusLevel.RetroMode", 10000),
                advanced.decimal("Skills.Herbalism.HylianLuck.ChanceMax", 10.0D),
                advanced.integer("Skills.Herbalism.HylianLuck.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Herbalism.HylianLuck.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Herbalism.ShroomThumb.ChanceMax", 50.0D),
                advanced.integer("Skills.Herbalism.ShroomThumb.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Herbalism.ShroomThumb.MaxBonusLevel.RetroMode", 1000),
                config.bool("Green_Thumb_Replanting_Crops.Carrots", true),
                config.bool("Green_Thumb_Replanting_Crops.Wheat", true),
                config.bool("Green_Thumb_Replanting_Crops.Nether_Wart", true),
                config.bool("Green_Thumb_Replanting_Crops.Potatoes", true),
                config.bool("Green_Thumb_Replanting_Crops.Beetroots", true),
                config.bool("Green_Thumb_Replanting_Crops.Cocoa", true),
                config.bool("Green_Thumb_Replanting_Crops.Sweet_Berry_Bush", true),
                config.bool("Skills.Herbalism.Prevent_AFK_Leveling", true),
                experience.bool("ExploitFix.LimitTallPlantXP", true));
    }

    public int greenTerraUnlockLevel() {
        return modeValue(greenTerraUnlockStandard, greenTerraUnlockRetro);
    }

    public int greenTerraDurationSeconds(int level) {
        return (int) AbilityDurationFormula.baseDuration(
                level,
                modeValue(abilityLengthCapStandard, abilityLengthCapRetro),
                modeValue(abilityLengthIncreaseStandard, abilityLengthIncreaseRetro),
                greenTerraMaximumSeconds).toSeconds();
    }

    public int greenThumbRank(int level) {
        return rank(level, progressionMode == ProgressionMode.RETRO ? greenThumbRetro : greenThumbStandard);
    }

    public int farmersDietRank(int level) {
        return rank(level, progressionMode == ProgressionMode.RETRO ? farmersDietRetro : farmersDietStandard);
    }

    public int greenThumbStage(int level, boolean greenTerraActive) {
        int rank = greenThumbRank(level);
        return greenTerraActive ? Math.min(4, rank + 1) : rank;
    }

    public int doubleDropsUnlockLevel() {
        return modeValue(doubleDropsUnlockStandard, doubleDropsUnlockRetro);
    }

    public int verdantBountyUnlockLevel() {
        return modeValue(verdantBountyUnlockStandard, verdantBountyUnlockRetro);
    }

    public double greenThumbChance(int level, boolean lucky) {
        return chance(level, greenThumbChanceMax, greenThumbMaxStandard, greenThumbMaxRetro, lucky);
    }

    public double doubleDropsChance(int level, boolean lucky) {
        return chance(level, doubleDropsChanceMax, doubleDropsMaxStandard, doubleDropsMaxRetro, lucky);
    }

    public double verdantBountyChance(int level, boolean lucky) {
        return chance(level, verdantBountyChanceMax, verdantBountyMaxStandard, verdantBountyMaxRetro, lucky);
    }

    public double hylianLuckChance(int level, boolean lucky) {
        return chance(level, hylianLuckChanceMax, hylianLuckMaxStandard, hylianLuckMaxRetro, lucky);
    }

    public double shroomThumbChance(int level, boolean lucky) {
        return chance(level, shroomThumbChanceMax, shroomThumbMaxStandard, shroomThumbMaxRetro, lucky);
    }

    public boolean replantEnabled(String blockPath) {
        return switch (blockPath) {
            case "carrots" -> replantCarrots;
            case "wheat" -> replantWheat;
            case "nether_wart" -> replantNetherWart;
            case "potatoes" -> replantPotatoes;
            case "beetroots" -> replantBeetroots;
            case "cocoa" -> replantCocoa;
            case "sweet_berry_bush" -> replantSweetBerryBush;
            default -> false;
        };
    }

    private double chance(int level, double max, int standard, int retro, boolean lucky) {
        return HerbalismProbability.chancePercent(level, progressionMode, max, standard, retro, lucky);
    }

    private int modeValue(int standard, int retro) {
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

    private static int[] rankArray(FlatYamlConfig config, String prefix, int[] defaults) {
        int[] values = new int[defaults.length];
        for (int index = 0; index < defaults.length; index++) {
            values[index] = config.integer(prefix + (index + 1), defaults[index]);
        }
        return values;
    }

    private static int[] copyRanks(int[] values, int size, String name) {
        Objects.requireNonNull(values, "values");
        if (values.length != size) {
            throw new IllegalArgumentException(name + " must have " + size + " ranks");
        }
        int[] copy = values.clone();
        int previous = -1;
        for (int value : copy) {
            if (value < previous || value < 0) {
                throw new IllegalArgumentException(name + " ranks must be non-negative and ordered");
            }
            previous = value;
        }
        return copy;
    }
}
