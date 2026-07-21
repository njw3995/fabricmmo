package io.github.njw3995.fabricmmo.core.skill.maces;

/** Pure mcMMO 2.3.000 Maces damage formulas. */
public final class MacesDamage {
    private MacesDamage() {
    }

    public static double crushDamage(int rank, double baseDamage, double rankMultiplier) {
        if (rank <= 0) {
            return 0.0D;
        }
        return Math.max(0.0D, baseDamage) + rank * Math.max(0.0D, rankMultiplier);
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
}
