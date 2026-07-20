package io.github.njw3995.fabricmmo.core.skill.acrobatics;

/** Pure damage and XP formulas from mcMMO 2.3.000 Acrobatics. */
public final class AcrobaticsDamage {
    private AcrobaticsDamage() {
    }

    public static double dodgeDamage(double damage, double modifier) {
        if (!Double.isFinite(damage) || damage < 0.0D) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
        if (!Double.isFinite(modifier) || modifier <= 0.0D) {
            throw new IllegalArgumentException("modifier must be finite and positive");
        }
        return Math.max(damage / modifier, 1.0D);
    }

    public static double rollDamage(double damage, double threshold) {
        if (!Double.isFinite(damage) || damage < 0.0D) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
        if (!Double.isFinite(threshold) || threshold < 0.0D) {
            throw new IllegalArgumentException("threshold must be finite and non-negative");
        }
        return Math.max(damage - threshold, 0.0D);
    }

    public static double fallXp(
            double damage,
            boolean successfulRoll,
            double rollModifier,
            double fallModifier,
            boolean featherFalling,
            double featherFallingMultiplier) {
        double clampedDamage = Math.min(20.0D, Math.max(0.0D, damage));
        float xp = (float) (clampedDamage * (successfulRoll ? rollModifier : fallModifier));
        if (featherFalling) {
            xp *= (float) featherFallingMultiplier;
        }
        return xp;
    }

    /** RollResult stores the float XP as an int, truncating rather than rounding. */
    public static int rollResultXp(double calculatedXp) {
        if (!Double.isFinite(calculatedXp) || calculatedXp < 0.0D) {
            throw new IllegalArgumentException("calculatedXp must be finite and non-negative");
        }
        return (int) calculatedXp;
    }

    public static boolean fatal(double health, double damage) {
        return health - damage <= 0.0D;
    }
}
