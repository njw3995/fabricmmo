package io.github.njw3995.fabricmmo.core.skill.ranged;

/** Pure probability formulas used by Daze and Arrow Retrieval. */
public final class RangedProbability {
    public static final double LUCKY_MULTIPLIER = 1.333D;

    private RangedProbability() {
    }

    public static double chancePercent(
            int level,
            int maximumLevel,
            double maximumChance,
            boolean lucky) {
        double normal = maximumLevel <= 0
                ? maximumChance
                : Math.min(maximumChance,
                        Math.max(0, level) * maximumChance / maximumLevel);
        return lucky ? normal * LUCKY_MULTIPLIER : normal;
    }
}
