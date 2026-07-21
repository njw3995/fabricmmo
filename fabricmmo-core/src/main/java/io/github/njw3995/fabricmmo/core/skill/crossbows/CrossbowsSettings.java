package io.github.njw3995.fabricmmo.core.skill.crossbows;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedDamage;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedRanks;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Crossbows mechanics. */
public record CrossbowsSettings(
        ProgressionMode progressionMode,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean limitBreakPve,
        double poweredShotRankDamageMultiplier,
        double poweredShotMaximumDamage,
        double distanceXpMultiplier,
        int[] poweredShotUnlocksStandard,
        int[] poweredShotUnlocksRetro,
        int[] trickShotUnlocksStandard,
        int[] trickShotUnlocksRetro,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro) {

    public CrossbowsSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        poweredShotUnlocksStandard = RangedRanks.copy(
                poweredShotUnlocksStandard, 20, "poweredShotUnlocksStandard");
        poweredShotUnlocksRetro = RangedRanks.copy(
                poweredShotUnlocksRetro, 20, "poweredShotUnlocksRetro");
        trickShotUnlocksStandard = RangedRanks.copy(
                trickShotUnlocksStandard, 3, "trickShotUnlocksStandard");
        trickShotUnlocksRetro = RangedRanks.copy(
                trickShotUnlocksRetro, 3, "trickShotUnlocksRetro");
        limitBreakUnlocksStandard = RangedRanks.copy(
                limitBreakUnlocksStandard, 10, "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = RangedRanks.copy(
                limitBreakUnlocksRetro, 10, "limitBreakUnlocksRetro");
        if (!Double.isFinite(poweredShotRankDamageMultiplier)
                || poweredShotRankDamageMultiplier <= 0.0D) {
            throw new IllegalArgumentException(
                    "poweredShotRankDamageMultiplier must be finite and positive");
        }
        if (!Double.isFinite(poweredShotMaximumDamage) || poweredShotMaximumDamage < 0.0D) {
            throw new IllegalArgumentException(
                    "poweredShotMaximumDamage must be finite and non-negative");
        }
        if (!Double.isFinite(distanceXpMultiplier) || distanceXpMultiplier < 0.0D) {
            throw new IllegalArgumentException(
                    "distanceXpMultiplier must be finite and non-negative");
        }
    }

    @Override public int[] poweredShotUnlocksStandard() { return poweredShotUnlocksStandard.clone(); }
    @Override public int[] poweredShotUnlocksRetro() { return poweredShotUnlocksRetro.clone(); }
    @Override public int[] trickShotUnlocksStandard() { return trickShotUnlocksStandard.clone(); }
    @Override public int[] trickShotUnlocksRetro() { return trickShotUnlocksRetro.clone(); }
    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }

    public static CrossbowsSettings load(
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
        return new CrossbowsSettings(
                mode,
                config.bool("Skills.Crossbows.Enabled_For_PVP", true),
                config.bool("Skills.Crossbows.Enabled_For_PVE", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                advanced.decimal("Skills.Crossbows.PoweredShot.RankDamageMultiplier", 10.0D),
                advanced.decimal("Skills.Crossbows.PoweredShot.MaxDamage", 9.0D),
                experience.decimal("Experience_Values.Archery.Distance_Multiplier", 0.025D),
                RangedRanks.load(ranks, "Crossbows", "PoweredShot", ProgressionMode.STANDARD, 20),
                RangedRanks.load(ranks, "Crossbows", "PoweredShot", ProgressionMode.RETRO, 20),
                RangedRanks.load(ranks, "Crossbows", "TrickShot", ProgressionMode.STANDARD, 3),
                RangedRanks.load(ranks, "Crossbows", "TrickShot", ProgressionMode.RETRO, 3),
                RangedRanks.load(ranks, "Crossbows", "CrossbowsLimitBreak", ProgressionMode.STANDARD, 10),
                RangedRanks.load(ranks, "Crossbows", "CrossbowsLimitBreak", ProgressionMode.RETRO, 10));
    }

    public int poweredShotRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                poweredShotUnlocksStandard, poweredShotUnlocksRetro);
    }

    public int trickShotRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                trickShotUnlocksStandard, trickShotUnlocksRetro);
    }

    public int limitBreakRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                limitBreakUnlocksStandard, limitBreakUnlocksRetro);
    }

    public double poweredShotBonusPercent(int level) {
        return poweredShotRank(level) * poweredShotRankDamageMultiplier / 100.0D;
    }

    public double poweredShotDamage(double damage, int level) {
        return RangedDamage.rankedPercentBonus(
                damage, poweredShotRank(level), poweredShotRankDamageMultiplier,
                poweredShotMaximumDamage);
    }

    public double distanceMultiplier(double distance) {
        return 1.0D + Math.min(Math.max(0.0D, distance), 50.0D) * distanceXpMultiplier;
    }
}
