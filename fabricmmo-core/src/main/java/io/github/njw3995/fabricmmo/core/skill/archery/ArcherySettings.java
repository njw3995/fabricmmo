package io.github.njw3995.fabricmmo.core.skill.archery;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedDamage;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedProbability;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedRanks;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Archery mechanics. */
public record ArcherySettings(
        ProgressionMode progressionMode,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean limitBreakPve,
        double skillShotRankDamageMultiplier,
        double skillShotMaximumDamage,
        double dazeChanceMaximum,
        int dazeMaximumLevelStandard,
        int dazeMaximumLevelRetro,
        double dazeBonusDamage,
        double retrievalChanceMaximum,
        int retrievalMaximumLevelStandard,
        int retrievalMaximumLevelRetro,
        double forceMultiplier,
        double distanceXpMultiplier,
        int[] skillShotUnlocksStandard,
        int[] skillShotUnlocksRetro,
        int[] retrievalUnlocksStandard,
        int[] retrievalUnlocksRetro,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro) {

    public ArcherySettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        skillShotUnlocksStandard = RangedRanks.copy(
                skillShotUnlocksStandard, 20, "skillShotUnlocksStandard");
        skillShotUnlocksRetro = RangedRanks.copy(
                skillShotUnlocksRetro, 20, "skillShotUnlocksRetro");
        retrievalUnlocksStandard = RangedRanks.copy(
                retrievalUnlocksStandard, 1, "retrievalUnlocksStandard");
        retrievalUnlocksRetro = RangedRanks.copy(
                retrievalUnlocksRetro, 1, "retrievalUnlocksRetro");
        limitBreakUnlocksStandard = RangedRanks.copy(
                limitBreakUnlocksStandard, 10, "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = RangedRanks.copy(
                limitBreakUnlocksRetro, 10, "limitBreakUnlocksRetro");
        positive(skillShotRankDamageMultiplier, "skillShotRankDamageMultiplier");
        nonNegative(skillShotMaximumDamage, "skillShotMaximumDamage");
        nonNegative(dazeChanceMaximum, "dazeChanceMaximum");
        positive(dazeMaximumLevelStandard, "dazeMaximumLevelStandard");
        positive(dazeMaximumLevelRetro, "dazeMaximumLevelRetro");
        nonNegative(dazeBonusDamage, "dazeBonusDamage");
        nonNegative(retrievalChanceMaximum, "retrievalChanceMaximum");
        positive(retrievalMaximumLevelStandard, "retrievalMaximumLevelStandard");
        positive(retrievalMaximumLevelRetro, "retrievalMaximumLevelRetro");
        nonNegative(forceMultiplier, "forceMultiplier");
        nonNegative(distanceXpMultiplier, "distanceXpMultiplier");
    }

    @Override public int[] skillShotUnlocksStandard() { return skillShotUnlocksStandard.clone(); }
    @Override public int[] skillShotUnlocksRetro() { return skillShotUnlocksRetro.clone(); }
    @Override public int[] retrievalUnlocksStandard() { return retrievalUnlocksStandard.clone(); }
    @Override public int[] retrievalUnlocksRetro() { return retrievalUnlocksRetro.clone(); }
    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }

    public static ArcherySettings load(
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
        return new ArcherySettings(
                mode,
                config.bool("Skills.Archery.Enabled_For_PVP", true),
                config.bool("Skills.Archery.Enabled_For_PVE", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                advanced.decimal("Skills.Archery.SkillShot.RankDamageMultiplier", 10.0D),
                advanced.decimal("Skills.Archery.SkillShot.MaxDamage", 9.0D),
                advanced.decimal("Skills.Archery.Daze.ChanceMax", 50.0D),
                advanced.integer("Skills.Archery.Daze.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Archery.Daze.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Archery.Daze.BonusDamage", 4.0D),
                advanced.decimal("Skills.Archery.ArrowRetrieval.ChanceMax", 100.0D),
                advanced.integer("Skills.Archery.ArrowRetrieval.MaxBonusLevel.Standard", 100),
                advanced.integer("Skills.Archery.ArrowRetrieval.MaxBonusLevel.RetroMode", 1000),
                advanced.decimal("Skills.Archery.ForceMultiplier", 2.0D),
                experience.decimal("Experience_Values.Archery.Distance_Multiplier", 0.025D),
                RangedRanks.load(ranks, "Archery", "SkillShot", ProgressionMode.STANDARD, 20),
                RangedRanks.load(ranks, "Archery", "SkillShot", ProgressionMode.RETRO, 20),
                RangedRanks.load(ranks, "Archery", "ArrowRetrieval", ProgressionMode.STANDARD, 1),
                RangedRanks.load(ranks, "Archery", "ArrowRetrieval", ProgressionMode.RETRO, 1),
                RangedRanks.load(ranks, "Archery", "ArcheryLimitBreak", ProgressionMode.STANDARD, 10),
                RangedRanks.load(ranks, "Archery", "ArcheryLimitBreak", ProgressionMode.RETRO, 10));
    }

    public int skillShotRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                skillShotUnlocksStandard, skillShotUnlocksRetro);
    }

    public int retrievalRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                retrievalUnlocksStandard, retrievalUnlocksRetro);
    }

    public int limitBreakRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                limitBreakUnlocksStandard, limitBreakUnlocksRetro);
    }

    public double skillShotBonusPercent(int level) {
        return skillShotRank(level) * skillShotRankDamageMultiplier / 100.0D;
    }

    public double skillShotDamage(double damage, int level) {
        return RangedDamage.rankedPercentBonus(
                damage, skillShotRank(level), skillShotRankDamageMultiplier,
                skillShotMaximumDamage);
    }

    public double dazeChancePercent(int level, boolean lucky) {
        int cap = progressionMode == ProgressionMode.RETRO
                ? dazeMaximumLevelRetro : dazeMaximumLevelStandard;
        return RangedProbability.chancePercent(
                level, cap, dazeChanceMaximum, lucky);
    }

    public double retrievalChancePercent(int level, boolean lucky) {
        int cap = progressionMode == ProgressionMode.RETRO
                ? retrievalMaximumLevelRetro : retrievalMaximumLevelStandard;
        return RangedProbability.chancePercent(
                level, cap, retrievalChanceMaximum, lucky);
    }

    public double distanceMultiplier(double distance) {
        return 1.0D + Math.min(Math.max(0.0D, distance), 50.0D) * distanceXpMultiplier;
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private static void nonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
