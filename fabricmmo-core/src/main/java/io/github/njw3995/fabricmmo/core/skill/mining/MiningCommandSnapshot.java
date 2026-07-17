package io.github.njw3995.fabricmmo.core.skill.mining;

/** Values shown by the Mining skill command without coupling formatting to Minecraft classes. */
public record MiningCommandSnapshot(
        int level,
        int xp,
        int xpToNextLevel,
        double doubleDropChancePercent,
        double motherLodeChancePercent,
        int superBreakerDurationSeconds,
        int superBreakerCooldownSeconds,
        boolean superBreakerActive,
        int superBreakerSecondsRemaining,
        int blastRank,
        int maximumBlastRank,
        double oreBonusPercent,
        int dropMultiplier,
        double radiusIncrease,
        double demolitionsDamageDecreasePercent,
        int blastCooldownSeconds,
        boolean showDoubleDrops,
        boolean showMotherLode,
        boolean showSuperBreaker,
        boolean showBlastMining,
        boolean showBiggerBombs,
        boolean showDemolitionsExpertise) {
    public MiningCommandSnapshot {
        if (level < 0 || xp < 0 || xpToNextLevel < 0 || superBreakerDurationSeconds < 0
                || superBreakerCooldownSeconds < 0 || superBreakerSecondsRemaining < 0
                || blastRank < 0 || maximumBlastRank < 1 || blastCooldownSeconds < 0) {
            throw new IllegalArgumentException("Mining command values must not be negative");
        }
    }

    public MiningCommandSnapshot(
            int level,
            int xp,
            int xpToNextLevel,
            double doubleDropChancePercent,
            double motherLodeChancePercent,
            int superBreakerDurationSeconds,
            int superBreakerCooldownSeconds,
            boolean superBreakerActive,
            int superBreakerSecondsRemaining,
            int blastRank,
            int maximumBlastRank,
            double oreBonusPercent,
            int dropMultiplier,
            double radiusIncrease,
            double demolitionsDamageDecreasePercent,
            int blastCooldownSeconds) {
        this(
                level,
                xp,
                xpToNextLevel,
                doubleDropChancePercent,
                motherLodeChancePercent,
                superBreakerDurationSeconds,
                superBreakerCooldownSeconds,
                superBreakerActive,
                superBreakerSecondsRemaining,
                blastRank,
                maximumBlastRank,
                oreBonusPercent,
                dropMultiplier,
                radiusIncrease,
                demolitionsDamageDecreasePercent,
                blastCooldownSeconds,
                true,
                true,
                true,
                true,
                true,
                true);
    }
}
