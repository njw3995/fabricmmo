package io.github.njw3995.fabricmmo.core.skill.unarmed;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;

/** Pure mcMMO 2.3.000 Unarmed probability formulas. */
public final class UnarmedProbability {
    public static final double LUCKY_MULTIPLIER = 1.333D;

    private UnarmedProbability() {
    }

    public static double chancePercent(
            int level,
            ProgressionMode mode,
            double chanceMax,
            int maxLevelStandard,
            int maxLevelRetro,
            boolean lucky) {
        int cap = mode == ProgressionMode.RETRO ? maxLevelRetro : maxLevelStandard;
        double normal = cap <= 0 ? chanceMax
                : Math.min(chanceMax, Math.max(0, level) * chanceMax / cap);
        return lucky ? normal * LUCKY_MULTIPLIER : normal;
    }

    public static boolean succeeds(double roll, double chancePercent, double attackStrength) {
        return roll < Math.max(0.0D, chancePercent) * clamp01(attackStrength);
    }

    public static boolean succeedsUnscaled(double roll, double chancePercent) {
        return roll < Math.max(0.0D, chancePercent);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
