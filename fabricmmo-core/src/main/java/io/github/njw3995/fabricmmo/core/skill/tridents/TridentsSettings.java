package io.github.njw3995.fabricmmo.core.skill.tridents;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedDamage;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedRanks;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 Tridents mechanics. */
public record TridentsSettings(
        ProgressionMode progressionMode,
        boolean pvpEnabled,
        boolean pveEnabled,
        boolean limitBreakPve,
        boolean adjustForAttackCooldown,
        double impaleBaseDamage,
        double impaleRankDamageMultiplier,
        int[] impaleUnlocksStandard,
        int[] impaleUnlocksRetro,
        int[] limitBreakUnlocksStandard,
        int[] limitBreakUnlocksRetro) {

    public TridentsSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        impaleUnlocksStandard = RangedRanks.copy(
                impaleUnlocksStandard, 10, "impaleUnlocksStandard");
        impaleUnlocksRetro = RangedRanks.copy(
                impaleUnlocksRetro, 10, "impaleUnlocksRetro");
        limitBreakUnlocksStandard = RangedRanks.copy(
                limitBreakUnlocksStandard, 10, "limitBreakUnlocksStandard");
        limitBreakUnlocksRetro = RangedRanks.copy(
                limitBreakUnlocksRetro, 10, "limitBreakUnlocksRetro");
        if (!Double.isFinite(impaleBaseDamage) || impaleBaseDamage < 0.0D) {
            throw new IllegalArgumentException("impaleBaseDamage must be finite and non-negative");
        }
        if (!Double.isFinite(impaleRankDamageMultiplier)
                || impaleRankDamageMultiplier < 0.0D) {
            throw new IllegalArgumentException(
                    "impaleRankDamageMultiplier must be finite and non-negative");
        }
    }

    @Override public int[] impaleUnlocksStandard() { return impaleUnlocksStandard.clone(); }
    @Override public int[] impaleUnlocksRetro() { return impaleUnlocksRetro.clone(); }
    @Override public int[] limitBreakUnlocksStandard() { return limitBreakUnlocksStandard.clone(); }
    @Override public int[] limitBreakUnlocksRetro() { return limitBreakUnlocksRetro.clone(); }

    public static TridentsSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        ProgressionMode mode = config.bool("General.RetroMode.Enabled", true)
                ? ProgressionMode.RETRO : ProgressionMode.STANDARD;
        return new TridentsSettings(
                mode,
                config.bool("Skills.Tridents.Enabled_For_PVP", true),
                config.bool("Skills.Tridents.Enabled_For_PVE", true),
                advanced.bool("Skills.General.LimitBreak.AllowPVE", false),
                advanced.bool(
                        "Skills.General.Attack_Cooldown.Adjust_Skills_For_Attack_Cooldown", true),
                advanced.decimal("Skills.Tridents.Impale.Base_Damage", 1.0D),
                advanced.decimal("Skills.Tridents.Impale.Rank_Damage_Multiplier", 0.5D),
                RangedRanks.load(ranks, "Tridents", "Impale", ProgressionMode.STANDARD, 10),
                RangedRanks.load(ranks, "Tridents", "Impale", ProgressionMode.RETRO, 10),
                RangedRanks.load(ranks, "Tridents", "TridentsLimitBreak", ProgressionMode.STANDARD, 10),
                RangedRanks.load(ranks, "Tridents", "TridentsLimitBreak", ProgressionMode.RETRO, 10));
    }

    public int impaleRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                impaleUnlocksStandard, impaleUnlocksRetro);
    }

    public int limitBreakRank(int level) {
        return RangedRanks.rank(level, progressionMode,
                limitBreakUnlocksStandard, limitBreakUnlocksRetro);
    }

    public double impaleDamage(int level) {
        return RangedDamage.impaleDamage(
                impaleRank(level), impaleBaseDamage, impaleRankDamageMultiplier);
    }
}
