package io.github.njw3995.fabricmmo.core.skill.axes;

/** Pure mcMMO 2.3.000 Axes damage and durability formulas. */
public final class AxesDamage {
    private AxesDamage() {
    }

    public static double axeMasteryDamage(int rank, double perRank) {
        return Math.max(0, rank) * Math.max(0.0D, perRank);
    }

    public static double criticalExtraDamage(
            double currentDamage, boolean playerTarget, double pvpModifier, double pveModifier) {
        double modifier = playerTarget ? pvpModifier : pveModifier;
        return Math.max(0.0D, currentDamage) * Math.max(0.0D, modifier - 1.0D);
    }

    public static double skullSplitterDamage(
            double rawPrimaryDamage, double damageModifier, double attackStrength) {
        if (damageModifier <= 0.0D) {
            return 1.0D;
        }
        return Math.max((Math.max(0.0D, rawPrimaryDamage) / damageModifier)
                * clamp01(attackStrength), 1.0D);
    }

    /** Matches SkillUtils.handleArmorDurabilityChange(raw, 1) for Axes Armor Impact. */
    public static int armorImpactDurabilityDamage(
            double rawDamage, int unbreakingLevel, int maximumDurability) {
        if (rawDamage <= 0.0D || maximumDurability <= 0) {
            return 0;
        }
        int level = Math.max(0, unbreakingLevel);
        double adjusted = rawDamage * (0.6D + 0.4D / (level + 1.0D));
        return (int) Math.min(adjusted, maximumDurability);
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
