package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.ability.AbilityDurationFormula;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/** Complete Mining configuration used by commands, abilities, Blast Mining and exploit fixes. */
public record MiningSettings(
        ProgressionMode progressionMode,
        boolean abilitiesEnabled,
        boolean abilityMessages,
        boolean notifyNearbyPlayers,
        boolean onlyActivateWhenSneaking,
        int abilityLengthCapStandard,
        int abilityLengthCapRetro,
        int abilityLengthIncreaseStandard,
        int abilityLengthIncreaseRetro,
        int abilityEnchantBuff,
        int superBreakerCooldownSeconds,
        int superBreakerMaximumSeconds,
        int superBreakerUnlockStandard,
        int superBreakerUnlockRetro,
        int abilityToolDamage,
        int blastMiningCooldownSeconds,
        int blastMiningMaximumSeconds,
        int remoteDetonationDistance,
        String detonatorName,
        boolean blastBonusDropsEnabled,
        boolean pistonExploitPrevention,
        boolean lavaStoneExploitPrevention,
        int[] blastUnlockStandard,
        int[] blastUnlockRetro,
        double[] blastDamageDecreasePercent,
        double[] oreBonusPercent,
        double[] debrisReductionPercent,
        int[] dropMultiplier,
        double[] blastRadiusModifier) {
    public static final int BLAST_RANKS = 8;

    public MiningSettings {
        Objects.requireNonNull(progressionMode, "progressionMode");
        detonatorName = Objects.requireNonNull(detonatorName, "detonatorName")
                .trim().toLowerCase(Locale.ROOT);
        if (detonatorName.isEmpty()) {
            throw new IllegalArgumentException("detonatorName must not be empty");
        }
        requireNonNegative(abilityLengthCapStandard, "abilityLengthCapStandard");
        requireNonNegative(abilityLengthCapRetro, "abilityLengthCapRetro");
        requirePositive(abilityLengthIncreaseStandard, "abilityLengthIncreaseStandard");
        requirePositive(abilityLengthIncreaseRetro, "abilityLengthIncreaseRetro");
        requireNonNegative(abilityEnchantBuff, "abilityEnchantBuff");
        requireNonNegative(superBreakerCooldownSeconds, "superBreakerCooldownSeconds");
        requireNonNegative(superBreakerMaximumSeconds, "superBreakerMaximumSeconds");
        requireNonNegative(superBreakerUnlockStandard, "superBreakerUnlockStandard");
        requireNonNegative(superBreakerUnlockRetro, "superBreakerUnlockRetro");
        requireNonNegative(abilityToolDamage, "abilityToolDamage");
        requireNonNegative(blastMiningCooldownSeconds, "blastMiningCooldownSeconds");
        requireNonNegative(blastMiningMaximumSeconds, "blastMiningMaximumSeconds");
        if (remoteDetonationDistance < 1 || remoteDetonationDistance > 100) {
            throw new IllegalArgumentException("remoteDetonationDistance must be in [1,100]");
        }
        blastUnlockStandard = copyLength(blastUnlockStandard, "blastUnlockStandard");
        blastUnlockRetro = copyLength(blastUnlockRetro, "blastUnlockRetro");
        blastDamageDecreasePercent = copyLength(
                blastDamageDecreasePercent, "blastDamageDecreasePercent");
        oreBonusPercent = copyLength(oreBonusPercent, "oreBonusPercent");
        debrisReductionPercent = copyLength(debrisReductionPercent, "debrisReductionPercent");
        dropMultiplier = copyLength(dropMultiplier, "dropMultiplier");
        blastRadiusModifier = copyLength(blastRadiusModifier, "blastRadiusModifier");
        for (int rank = 0; rank < BLAST_RANKS; rank++) {
            requireNonNegative(blastUnlockStandard[rank], "blastUnlockStandard");
            requireNonNegative(blastUnlockRetro[rank], "blastUnlockRetro");
            requirePercent(blastDamageDecreasePercent[rank], "blastDamageDecreasePercent");
            requireNonNegative(oreBonusPercent[rank], "oreBonusPercent");
            requirePercent(debrisReductionPercent[rank], "debrisReductionPercent");
            requirePositive(dropMultiplier[rank], "dropMultiplier");
            requireNonNegative(blastRadiusModifier[rank], "blastRadiusModifier");
        }
    }

    @Override
    public int[] blastUnlockStandard() {
        return blastUnlockStandard.clone();
    }

    @Override
    public int[] blastUnlockRetro() {
        return blastUnlockRetro.clone();
    }

    @Override
    public double[] blastDamageDecreasePercent() {
        return blastDamageDecreasePercent.clone();
    }

    @Override
    public double[] oreBonusPercent() {
        return oreBonusPercent.clone();
    }

    @Override
    public double[] debrisReductionPercent() {
        return debrisReductionPercent.clone();
    }

    @Override
    public int[] dropMultiplier() {
        return dropMultiplier.clone();
    }

    @Override
    public double[] blastRadiusModifier() {
        return blastRadiusModifier.clone();
    }

    public static MiningSettings load(
            Path configFile,
            Path advancedFile,
            Path skillRanksFile,
            Path experienceFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig advanced = FlatYamlConfig.load(advancedFile);
        FlatYamlConfig ranks = FlatYamlConfig.load(skillRanksFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);
        boolean retro = config.requiredBoolean("General.RetroMode.Enabled");
        return new MiningSettings(
                retro ? ProgressionMode.RETRO : ProgressionMode.STANDARD,
                config.requiredBoolean("Abilities.Enabled"),
                config.requiredBoolean("Abilities.Messages"),
                advanced.bool("Feedback.Events.AbilityActivation.SendNotificationToOtherPlayers", true),
                config.requiredBoolean("Abilities.Activation.Only_Activate_When_Sneaking"),
                advanced.requiredInt("Skills.General.Ability.Length.Standard.CapLevel"),
                advanced.requiredInt("Skills.General.Ability.Length.RetroMode.CapLevel"),
                advanced.requiredInt("Skills.General.Ability.Length.Standard.IncreaseLevel"),
                advanced.requiredInt("Skills.General.Ability.Length.RetroMode.IncreaseLevel"),
                advanced.requiredInt("Skills.General.Ability.EnchantBuff"),
                config.requiredInt("Abilities.Cooldowns.Super_Breaker"),
                config.requiredInt("Abilities.Max_Seconds.Super_Breaker"),
                ranks.requiredInt("Mining.SuperBreaker.Standard.Rank_1"),
                ranks.requiredInt("Mining.SuperBreaker.RetroMode.Rank_1"),
                config.requiredInt("Abilities.Tools.Durability_Loss"),
                config.requiredInt("Abilities.Cooldowns.Blast_Mining"),
                config.requiredInt("Abilities.Max_Seconds.Blast_Mining"),
                advanced.requiredInt("Skills.Mining.BlastMining.RemoteDetonationDistance"),
                config.requiredString("Skills.Mining.Detonator_Name"),
                advanced.requiredBoolean("Skills.Mining.BlastMining.Bonus_Drops.Enabled"),
                experience.bool("ExploitFix.PistonCheating", true),
                experience.bool("ExploitFix.LavaStoneAndCobbleFarming", true),
                rankInts(ranks, "Mining.BlastMining.Standard.Rank_"),
                rankInts(ranks, "Mining.BlastMining.RetroMode.Rank_"),
                rankDoubles(advanced, "Skills.Mining.BlastMining.BlastDamageDecrease.Rank_"),
                rankDoubles(advanced, "Skills.Mining.BlastMining.OreBonus.Rank_"),
                rankDoubles(advanced, "Skills.Mining.BlastMining.DebrisReduction.Rank_"),
                rankInts(advanced, "Skills.Mining.BlastMining.DropMultiplier.Rank_"),
                rankDoubles(advanced, "Skills.Mining.BlastMining.BlastRadiusModifier.Rank_"));
    }

    public int abilityLengthCap() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthCapRetro : abilityLengthCapStandard;
    }

    public int abilityLengthIncrease() {
        return progressionMode == ProgressionMode.RETRO
                ? abilityLengthIncreaseRetro : abilityLengthIncreaseStandard;
    }

    public int superBreakerUnlockLevel() {
        return progressionMode == ProgressionMode.RETRO
                ? superBreakerUnlockRetro : superBreakerUnlockStandard;
    }

    public int superBreakerMaximumSeconds() {
        return superBreakerMaximumSeconds;
    }

    public int superBreakerDurationSeconds(int level) {
        return (int) AbilityDurationFormula.baseDuration(
                level, abilityLengthCap(), abilityLengthIncrease(), superBreakerMaximumSeconds)
                .toSeconds();
    }

    public int blastRank(int level) {
        int[] unlocks = progressionMode == ProgressionMode.RETRO
                ? blastUnlockRetro : blastUnlockStandard;
        int result = 0;
        for (int index = 0; index < unlocks.length; index++) {
            if (level >= unlocks[index]) {
                result = index + 1;
            }
        }
        return result;
    }

    public int blastUnlockLevel(int rank) {
        requireRank(rank);
        return (progressionMode == ProgressionMode.RETRO
                ? blastUnlockRetro : blastUnlockStandard)[rank - 1];
    }

    public double blastDamageDecreasePercent(int rank) {
        return rank == 0 ? 0.0D : blastDamageDecreasePercent[rankIndex(rank)];
    }

    public double oreBonusFraction(int rank) {
        return rank == 0 ? 0.0D : oreBonusPercent[rankIndex(rank)] / 100.0D;
    }

    public double debrisReductionPercent(int rank) {
        return rank == 0 ? 0.0D : debrisReductionPercent[rankIndex(rank)];
    }

    public int dropMultiplier(int rank) {
        if (rank == 0 || !blastBonusDropsEnabled) {
            return 0;
        }
        requireRank(rank);
        return switch (rank) {
            case 1, 2 -> 1;
            case 3, 4, 5, 6 -> 2;
            case 7, 8 -> 3;
            default -> throw new IllegalStateException("Unexpected Blast Mining rank: " + rank);
        };
    }

    public double blastRadiusModifier(int rank) {
        return rank == 0 ? 0.0D : blastRadiusModifier[rankIndex(rank)];
    }

    public int biggerBombsUnlockLevel() {
        return firstPositiveUnlock(blastRadiusModifier);
    }

    public int demolitionsExpertiseUnlockLevel() {
        return firstPositiveUnlock(blastDamageDecreasePercent);
    }

    private int firstPositiveUnlock(double[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] > 0.0D) {
                return blastUnlockLevel(index + 1);
            }
        }
        return 0;
    }

    private static int[] rankInts(FlatYamlConfig config, String prefix) {
        int[] values = new int[BLAST_RANKS];
        for (int rank = 1; rank <= BLAST_RANKS; rank++) {
            values[rank - 1] = config.requiredInt(prefix + rank);
        }
        return values;
    }

    private static double[] rankDoubles(FlatYamlConfig config, String prefix) {
        double[] values = new double[BLAST_RANKS];
        for (int rank = 1; rank <= BLAST_RANKS; rank++) {
            values[rank - 1] = config.requiredDouble(prefix + rank);
        }
        return values;
    }

    private static int rankIndex(int rank) {
        requireRank(rank);
        return rank - 1;
    }

    private static void requireRank(int rank) {
        if (rank < 1 || rank > BLAST_RANKS) {
            throw new IllegalArgumentException("rank must be in [1," + BLAST_RANKS + ']');
        }
    }

    private static int[] copyLength(int[] values, String name) {
        Objects.requireNonNull(values, name);
        if (values.length != BLAST_RANKS) {
            throw new IllegalArgumentException(name + " must have " + BLAST_RANKS + " entries");
        }
        return values.clone();
    }

    private static double[] copyLength(double[] values, String name) {
        Objects.requireNonNull(values, name);
        if (values.length != BLAST_RANKS) {
            throw new IllegalArgumentException(name + " must have " + BLAST_RANKS + " entries");
        }
        return values.clone();
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static void requireNonNegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    private static void requirePercent(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
            throw new IllegalArgumentException(name + " must be in [0,100]");
        }
    }
}
