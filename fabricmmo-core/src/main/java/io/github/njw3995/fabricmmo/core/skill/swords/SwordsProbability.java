package io.github.njw3995.fabricmmo.core.skill.swords;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;

/** Pure probability formulas used by Counter Attack and Rupture. */
public final class SwordsProbability {
    public static final double LUCKY_MULTIPLIER = 1.333D;

    private SwordsProbability() {
    }

    public static double counterChancePercent(
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

    public static double ruptureChancePercent(double configuredChance, boolean lucky) {
        return lucky ? configuredChance * LUCKY_MULTIPLIER : configuredChance;
    }
}
