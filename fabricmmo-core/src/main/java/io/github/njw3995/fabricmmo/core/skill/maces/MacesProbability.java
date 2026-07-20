package io.github.njw3995.fabricmmo.core.skill.maces;

/** Pure mcMMO 2.3.000 Maces probability formulas. */
public final class MacesProbability {
    public static final double LUCKY_MULTIPLIER = 1.333D;
    public static final double PANEL_LUCKY_MULTIPLIER = 1.33D;

    private MacesProbability() {
    }

    public static double chancePercent(double configuredChance, boolean lucky) {
        double normal = Math.max(0.0D, configuredChance);
        return lucky ? normal * LUCKY_MULTIPLIER : normal;
    }

    public static boolean succeeds(double roll, double chancePercent, double attackStrength) {
        double strength = Math.max(0.0D, Math.min(1.0D, attackStrength));
        return roll < Math.max(0.0D, chancePercent) * strength;
    }
}
