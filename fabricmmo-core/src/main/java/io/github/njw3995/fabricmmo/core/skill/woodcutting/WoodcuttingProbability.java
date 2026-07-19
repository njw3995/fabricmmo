package io.github.njw3995.fabricmmo.core.skill.woodcutting;

/** Upstream skill-RNG scaling used by Harvest Lumber and Clean Cuts. */
public final class WoodcuttingProbability {
    public static final double LUCKY_MODIFIER = 1.333D;

    private WoodcuttingProbability() {
    }

    public static double chance(
            int skillLevel,
            int maxBonusLevel,
            double chanceMaxPercent,
            boolean lucky) {
        if (skillLevel < 0 || maxBonusLevel < 0 || !Double.isFinite(chanceMaxPercent)
                || chanceMaxPercent < 0.0D) {
            throw new IllegalArgumentException("Invalid probability inputs");
        }
        double percent = maxBonusLevel == 0 || skillLevel >= maxBonusLevel
                ? chanceMaxPercent
                : (skillLevel / (double) maxBonusLevel) * chanceMaxPercent;
        double probability = Math.min(Math.max(0.0D, percent), chanceMaxPercent) / 100.0D;
        return Math.min(1.0D, lucky ? probability * LUCKY_MODIFIER : probability);
    }
}
