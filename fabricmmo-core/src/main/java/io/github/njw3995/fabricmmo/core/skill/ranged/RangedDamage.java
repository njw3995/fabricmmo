package io.github.njw3995.fabricmmo.core.skill.ranged;

/** Pure damage formulas copied from the mcMMO 2.3.000 ranged managers. */
public final class RangedDamage {
    private RangedDamage() {
    }

    public static double rankedPercentBonus(
            double originalDamage,
            int rank,
            double rankMultiplierPercent,
            double maximumAdditionalDamage) {
        double original = Math.max(0.0D, originalDamage);
        if (rank <= 0) {
            return original;
        }
        double result = original + original * rank * rankMultiplierPercent / 100.0D;
        return Math.min(result, original + Math.max(0.0D, maximumAdditionalDamage));
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

    public static double impaleDamage(int rank, double baseDamage, double perRankDamage) {
        return rank <= 0 ? 0.0D : baseDamage + rank * perRankDamage;
    }
}
