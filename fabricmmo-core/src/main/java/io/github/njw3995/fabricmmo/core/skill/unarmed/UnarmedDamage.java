package io.github.njw3995.fabricmmo.core.skill.unarmed;

/** Pure mcMMO 2.3.000 Unarmed damage formulas. */
public final class UnarmedDamage {
    public static final double BERSERK_DAMAGE_MODIFIER = 1.5D;

    private UnarmedDamage() {
    }

    public static double steelArmDamage(int rank, boolean override, double[] overrides) {
        int safeRank = Math.max(0, rank);
        if (override && safeRank > 0 && safeRank <= overrides.length) {
            return Math.max(0.0D, overrides[safeRank - 1]);
        }
        double lateRankBonus = safeRank >= 18 ? 1.0D + safeRank - 18.0D : 0.0D;
        return lateRankBonus + 0.5D + safeRank / 2.0D;
    }

    /**
     * Returns the upstream Berserk bonus before processUnarmedCombat applies attack strength
     * to the returned value a second time. This intentionally becomes negative for weak hits.
     */
    public static double berserkBonus(double currentDamage, double attackStrength) {
        double damage = Math.max(0.0D, currentDamage);
        double strength = clamp01(attackStrength);
        return (damage * BERSERK_DAMAGE_MODIFIER * strength) - damage;
    }

    public static double finalBerserkDamage(double currentDamage, double attackStrength) {
        double damage = Math.max(0.0D, currentDamage);
        double strength = clamp01(attackStrength);
        return damage + berserkBonus(damage, strength) * strength;
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

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
