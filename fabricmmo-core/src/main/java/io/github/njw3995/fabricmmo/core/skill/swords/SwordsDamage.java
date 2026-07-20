package io.github.njw3995.fabricmmo.core.skill.swords;

/** Pure pinned-upstream Swords damage calculations. */
public final class SwordsDamage {
    private SwordsDamage() {
    }

    public static double stabDamage(int rank, double baseDamage, double perRankMultiplier) {
        return rank <= 0 ? 0.0D : baseDamage + rank * perRankMultiplier;
    }

    public static double counterDamage(double incomingDamage, double modifier) {
        return modifier <= 0.0D ? 0.0D : Math.max(0.0D, incomingDamage) / modifier;
    }

    public static double serratedAoeDamage(double incomingDamage, double modifier) {
        return Math.max(Math.max(0.0D, incomingDamage) / modifier, 1.0D);
    }

    public static int limitBreakDamage(int rank, int armorQuality) {
        float damage = Math.max(0, rank);
        if (armorQuality <= 4) {
            damage *= 0.25F;
        } else if (armorQuality <= 8) {
            damage *= 0.50F;
        } else if (armorQuality <= 12) {
            damage *= 0.75F;
        }
        return (int) damage;
    }

    public static double attackStrengthScale(double rawDamage, double attackAttribute) {
        if (attackAttribute <= 0.0D) {
            return 1.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, rawDamage / attackAttribute));
    }
}
