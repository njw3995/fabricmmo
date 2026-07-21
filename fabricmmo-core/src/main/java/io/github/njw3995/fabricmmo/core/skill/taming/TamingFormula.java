package io.github.njw3995.fabricmmo.core.skill.taming;

public final class TamingFormula {
    private TamingFormula() {}

    public static double catalyzedChance(double configuredPercent, boolean lucky) {
        double value = lucky ? configuredPercent * (4.0D / 3.0D) : configuredPercent;
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    public static float goreDamage(float baseDamage, double modifier) {
        return (float) Math.max(0.0D, baseDamage * modifier);
    }

    public static float sharpenedClaws(float damage, double bonus) {
        return (float) Math.max(0.0D, damage + bonus);
    }

    public static float reducedDamage(float damage, double divisor) {
        return divisor <= 0.0D ? damage : (float) Math.max(0.0D, damage / divisor);
    }
}
