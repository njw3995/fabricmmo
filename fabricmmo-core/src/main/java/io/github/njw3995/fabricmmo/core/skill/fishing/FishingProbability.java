package io.github.njw3995.fabricmmo.core.skill.fishing;

/** Shared upstream-style Fishing probability helpers. */
public final class FishingProbability {
    public static final double LUCKY_MODIFIER = 1.333D;

    private FishingProbability() {
    }

    public static double luckyPercent(double basePercent, boolean lucky) {
        return lucky ? basePercent * LUCKY_MODIFIER : basePercent;
    }

    public static boolean succeeds(double rollZeroToOne, double percent) {
        if (Double.isNaN(rollZeroToOne) || rollZeroToOne < 0.0D || rollZeroToOne >= 1.0D) {
            throw new IllegalArgumentException("rollZeroToOne must be in [0, 1)");
        }
        return rollZeroToOne * 100.0D <= percent;
    }
}
