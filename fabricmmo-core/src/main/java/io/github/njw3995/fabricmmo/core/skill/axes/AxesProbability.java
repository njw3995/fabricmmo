package io.github.njw3995.fabricmmo.core.skill.axes;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;

/** Pure mcMMO 2.3.000 Axes probability formulas. */
public final class AxesProbability {
    public static final double LUCKY_MULTIPLIER = 1.333D;

    private AxesProbability() {
    }

    public static double criticalChancePercent(
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

    public static double staticChancePercent(double configuredChance, boolean lucky) {
        double normal = Math.max(0.0D, configuredChance);
        return lucky ? normal * LUCKY_MULTIPLIER : normal;
    }

    public static boolean succeeds(double roll, double chancePercent, double attackStrength) {
        return roll < Math.max(0.0D, chancePercent) * Math.max(0.0D, Math.min(1.0D, attackStrength));
    }
}
