package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;

/** Exact mcMMO 2.3.000 linear Acrobatics chance curves. */
public final class AcrobaticsProbability {
    public static final double LUCKY_MODIFIER = 1.333D;

    private AcrobaticsProbability() {
    }

    public static double chancePercent(
            int level,
            ProgressionMode mode,
            double maximumPercent,
            int standardMaximumLevel,
            int retroMaximumLevel,
            boolean lucky) {
        int maximumLevel = mode == ProgressionMode.RETRO
                ? retroMaximumLevel : standardMaximumLevel;
        double base = maximumLevel <= 0 || level >= maximumLevel
                ? maximumPercent
                : Math.min(maximumPercent,
                        Math.max(0.0D, level) / maximumLevel * maximumPercent);
        return lucky ? base * LUCKY_MODIFIER : base;
    }

    public static double gracefulRollChancePercent(double rollChancePercent) {
        return rollChancePercent * 2.0D;
    }
}
