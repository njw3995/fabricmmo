package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;

/** Pure upstream linear probability helpers for Herbalism passives. */
public final class HerbalismProbability {
    private HerbalismProbability() {
    }

    public static double chancePercent(
            int level,
            ProgressionMode mode,
            double chanceMax,
            int standardMaxLevel,
            int retroMaxLevel,
            boolean lucky) {
        int maxLevel = mode == ProgressionMode.RETRO ? retroMaxLevel : standardMaxLevel;
        double base = maxLevel <= 0
                ? chanceMax
                : Math.min(chanceMax, Math.max(0, level) * chanceMax / maxLevel);
        return lucky ? Math.min(100.0D, base * 1.3333333333333333D) : base;
    }

    public static boolean roll(double randomUnit, double chancePercent) {
        return randomUnit * 100.0D < chancePercent;
    }
}
