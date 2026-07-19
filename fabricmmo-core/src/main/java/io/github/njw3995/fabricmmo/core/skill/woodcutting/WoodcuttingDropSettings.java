package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.skill.gathering.ConfiguredBlockXpTable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Harvest Lumber and Clean Cuts configuration pinned to mcMMO 2.3.000. */
public record WoodcuttingDropSettings(
        Set<NamespacedId> enabledMaterials,
        double harvestLumberChanceMaxPercent,
        int harvestLumberMaxLevelStandard,
        int harvestLumberMaxLevelRetro,
        int harvestLumberUnlockStandard,
        int harvestLumberUnlockRetro,
        double cleanCutsChanceMaxPercent,
        int cleanCutsMaxLevelStandard,
        int cleanCutsMaxLevelRetro,
        int cleanCutsUnlockStandard,
        int cleanCutsUnlockRetro) {

    private static final Set<String> UPSTREAM_DEFAULT_MATERIALS = Set.of(
            "crimson_hyphae", "warped_hyphae", "stripped_crimson_hyphae",
            "stripped_warped_hyphae", "shroomlight", "crimson_stem", "warped_stem",
            "crimson_roots", "warped_roots", "acacia_wood", "acacia_log", "birch_wood",
            "birch_log", "cherry_wood", "cherry_log", "dark_oak_wood", "dark_oak_log",
            "oak_wood", "oak_log", "jungle_wood", "jungle_log", "spruce_wood",
            "spruce_log", "mangrove_wood", "mangrove_log");

    public WoodcuttingDropSettings {
        enabledMaterials = Collections.unmodifiableSet(new TreeSet<>(
                Objects.requireNonNull(enabledMaterials, "enabledMaterials")));
        requirePercent(harvestLumberChanceMaxPercent, "harvestLumberChanceMaxPercent");
        requirePercent(cleanCutsChanceMaxPercent, "cleanCutsChanceMaxPercent");
        requireNonNegative(harvestLumberMaxLevelStandard, "harvestLumberMaxLevelStandard");
        requireNonNegative(harvestLumberMaxLevelRetro, "harvestLumberMaxLevelRetro");
        requireNonNegative(harvestLumberUnlockStandard, "harvestLumberUnlockStandard");
        requireNonNegative(harvestLumberUnlockRetro, "harvestLumberUnlockRetro");
        requireNonNegative(cleanCutsMaxLevelStandard, "cleanCutsMaxLevelStandard");
        requireNonNegative(cleanCutsMaxLevelRetro, "cleanCutsMaxLevelRetro");
        requireNonNegative(cleanCutsUnlockStandard, "cleanCutsUnlockStandard");
        requireNonNegative(cleanCutsUnlockRetro, "cleanCutsUnlockRetro");
    }

    public static WoodcuttingDropSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        Set<NamespacedId> materials = new TreeSet<>();
        UPSTREAM_DEFAULT_MATERIALS.forEach(path ->
                materials.add(new NamespacedId("minecraft", path)));
        config.valuesWithPrefix("Bonus_Drops.Woodcutting.").forEach((key, value) -> {
            NamespacedId material = ConfiguredBlockXpTable.materialId(
                    key.substring("Bonus_Drops.Woodcutting.".length()));
            if (parseBoolean(value, key)) {
                materials.add(material);
            } else {
                materials.remove(material);
            }
        });
        return new WoodcuttingDropSettings(
                materials,
                advanced.decimal("Skills.Woodcutting.HarvestLumber.ChanceMax", 100.0D),
                advanced.integer(
                        "Skills.Woodcutting.HarvestLumber.MaxBonusLevel.Standard", 100),
                advanced.integer(
                        "Skills.Woodcutting.HarvestLumber.MaxBonusLevel.RetroMode", 1000),
                ranks.integer("Woodcutting.HarvestLumber.Standard.Rank_1", 1),
                ranks.integer("Woodcutting.HarvestLumber.RetroMode.Rank_1", 1),
                advanced.decimal("Skills.Woodcutting.CleanCuts.ChanceMax", 50.0D),
                advanced.integer("Skills.Woodcutting.CleanCuts.MaxBonusLevel.Standard", 1000),
                advanced.integer("Skills.Woodcutting.CleanCuts.MaxBonusLevel.RetroMode", 10000),
                ranks.integer("Woodcutting.CleanCuts.Standard.Rank_1", 100),
                ranks.integer("Woodcutting.CleanCuts.RetroMode.Rank_1", 1000));
    }

    public boolean materialEnabled(NamespacedId materialId) {
        return enabledMaterials.contains(materialId);
    }

    public boolean harvestLumberUnlocked(int level, ProgressionMode mode) {
        return level >= modeValue(mode, harvestLumberUnlockStandard, harvestLumberUnlockRetro);
    }

    public boolean cleanCutsUnlocked(int level, ProgressionMode mode) {
        return level >= modeValue(mode, cleanCutsUnlockStandard, cleanCutsUnlockRetro);
    }

    public int harvestLumberMaxLevel(ProgressionMode mode) {
        return modeValue(mode, harvestLumberMaxLevelStandard, harvestLumberMaxLevelRetro);
    }

    public int cleanCutsMaxLevel(ProgressionMode mode) {
        return modeValue(mode, cleanCutsMaxLevelStandard, cleanCutsMaxLevelRetro);
    }

    private static int modeValue(ProgressionMode mode, int standard, int retro) {
        return Objects.requireNonNull(mode, "mode") == ProgressionMode.RETRO ? retro : standard;
    }

    private static boolean parseBoolean(String value, String key) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean for " + key + ": " + value);
    }

    private static void requirePercent(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
            throw new IllegalArgumentException(name + " must be in [0,100]");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
