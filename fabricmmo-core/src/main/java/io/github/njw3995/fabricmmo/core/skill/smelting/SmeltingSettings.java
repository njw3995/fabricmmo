package io.github.njw3995.fabricmmo.core.skill.smelting;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Exact mcMMO 2.3.000 Smelting child-skill settings for Minecraft 1.21.1. */
public record SmeltingSettings(
        ProgressionMode progressionMode,
        int[] fuelEfficiencyStandard,
        int[] fuelEfficiencyRetro,
        int[] understandingStandard,
        int[] understandingRetro,
        double secondSmeltChanceMaximum,
        int secondSmeltMaximumBonusStandard,
        int secondSmeltMaximumBonusRetro,
        int[] vanillaXpMultiplier,
        Map<String, Integer> xpByInputName,
        Map<String, Boolean> secondSmeltByOutputName) {

    public SmeltingSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        fuelEfficiencyStandard = ordered(fuelEfficiencyStandard, "Fuel Efficiency Standard");
        fuelEfficiencyRetro = ordered(fuelEfficiencyRetro, "Fuel Efficiency Retro");
        understandingStandard = ordered(understandingStandard, "Understanding Standard");
        understandingRetro = ordered(understandingRetro, "Understanding Retro");
        vanillaXpMultiplier = vanillaXpMultiplier.clone();
        xpByInputName = Map.copyOf(xpByInputName);
        secondSmeltByOutputName = Map.copyOf(secondSmeltByOutputName);
        if (secondSmeltChanceMaximum < 0.0D || secondSmeltMaximumBonusStandard <= 0
                || secondSmeltMaximumBonusRetro <= 0 || vanillaXpMultiplier.length != 8) {
            throw new IllegalArgumentException("Invalid Smelting settings");
        }
    }

    public static SmeltingSettings load(
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
        Map<String, Integer> xp = new LinkedHashMap<>();
        experience.valuesWithPrefix("Experience_Values.Smelting.").forEach((key, value) ->
                xp.put(key.substring("Experience_Values.Smelting.".length()).toUpperCase(Locale.ROOT),
                        Integer.parseInt(value)));
        Map<String, Boolean> outputs = new LinkedHashMap<>();
        config.valuesWithPrefix("Bonus_Drops.Smelting.").forEach((key, value) ->
                outputs.put(key.substring("Bonus_Drops.Smelting.".length()).toUpperCase(Locale.ROOT),
                        Boolean.parseBoolean(value)));
        int[] vanilla = new int[8];
        for (int index = 0; index < vanilla.length; index++) {
            vanilla[index] = advanced.integer(
                    "Skills.Smelting.VanillaXPMultiplier.Rank_" + (index + 1),
                    new int[] {1, 2, 3, 3, 4, 4, 5, 5}[index]);
        }
        return new SmeltingSettings(
                mode,
                ranks(ranks, "Smelting.FuelEfficiency.Standard.Rank_", 3,
                        new int[] {10, 50, 75}),
                ranks(ranks, "Smelting.FuelEfficiency.RetroMode.Rank_", 3,
                        new int[] {100, 500, 750}),
                ranks(ranks, "Smelting.UnderstandingTheArt.Standard.Rank_", 8,
                        new int[] {10, 25, 35, 50, 65, 75, 85, 100}),
                ranks(ranks, "Smelting.UnderstandingTheArt.RetroMode.Rank_", 8,
                        new int[] {100, 250, 350, 500, 650, 750, 850, 1000}),
                advanced.decimal("Skills.Smelting.SecondSmelt.ChanceMax", 50.0D),
                advanced.integer("Skills.Smelting.SecondSmelt.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Smelting.SecondSmelt.MaxBonusLevel.RetroMode", 1000),
                vanilla,
                xp,
                outputs);
    }

    public int fuelEfficiencyRank(int level) {
        return rank(level, mode(fuelEfficiencyStandard, fuelEfficiencyRetro));
    }

    public int understandingRank(int level) {
        return rank(level, mode(understandingStandard, understandingRetro));
    }

    public double secondSmeltChance(int level, boolean lucky) {
        int maximum = progressionMode == ProgressionMode.RETRO
                ? secondSmeltMaximumBonusRetro : secondSmeltMaximumBonusStandard;
        double base = Math.min(secondSmeltChanceMaximum,
                secondSmeltChanceMaximum * Math.min(level, maximum) / maximum);
        return Math.min(100.0D, lucky ? base * 1.333D : base);
    }

    public int vanillaXpMultiplier(int level) {
        int rank = understandingRank(level);
        return rank <= 0 ? 1 : vanillaXpMultiplier[Math.min(rank, 8) - 1];
    }

    public int xpForInput(String registryPath) {
        return xpByInputName.getOrDefault(normalize(registryPath), 0);
    }

    public boolean secondSmeltEnabledForOutput(String registryPath) {
        return secondSmeltByOutputName.getOrDefault(normalize(registryPath), false);
    }

    private static String normalize(String registryPath) {
        int colon = registryPath.indexOf(':');
        String path = colon >= 0 ? registryPath.substring(colon + 1) : registryPath;
        return path.toUpperCase(Locale.ROOT);
    }

    private int[] mode(int[] standard, int[] retro) {
        return progressionMode == ProgressionMode.RETRO ? retro : standard;
    }

    private static int rank(int level, int[] values) {
        int rank = 0;
        for (int index = 0; index < values.length; index++) {
            if (level >= values[index]) {
                rank = index + 1;
            }
        }
        return rank;
    }

    private static int[] ranks(FlatYamlConfig yaml, String prefix, int count, int[] defaults) {
        int[] values = new int[count];
        for (int index = 0; index < count; index++) {
            values[index] = yaml.integer(prefix + (index + 1), defaults[index]);
        }
        return ordered(values, prefix);
    }

    private static int[] ordered(int[] values, String name) {
        int[] copy = values.clone();
        int previous = -1;
        for (int value : copy) {
            if (value < previous || value < 0) {
                throw new IllegalArgumentException(name + " must be ordered and non-negative");
            }
            previous = value;
        }
        return copy;
    }
}
