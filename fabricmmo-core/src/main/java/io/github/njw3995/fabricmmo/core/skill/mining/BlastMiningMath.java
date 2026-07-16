package io.github.njw3995.fabricmmo.core.skill.mining;

public final class BlastMiningMath {
    private BlastMiningMath() {
    }

    public static float radius(float vanillaRadius, int rank) {
        if (!Float.isFinite(vanillaRadius) || vanillaRadius < 0.0F) {
            throw new IllegalArgumentException("vanillaRadius must be finite and non-negative");
        }
        return (float) (vanillaRadius + BlastMiningDefaults.radiusModifier(rank));
    }

    public static double ownerDamage(double rawDamage, int rank) {
        if (!Double.isFinite(rawDamage) || rawDamage < 0.0D) {
            throw new IllegalArgumentException("rawDamage must be finite and non-negative");
        }
        return rawDamage * ((100.0D - BlastMiningDefaults.damageDecreasePercent(rank)) / 100.0D);
    }

    public static double bystanderDamage(double rawDamage) {
        if (!Double.isFinite(rawDamage)) {
            throw new IllegalArgumentException("rawDamage must be finite");
        }
        return Math.max(0.0D, Math.min(rawDamage, BlastMiningDefaults.PVP_DAMAGE_CAP));
    }

    public static float oreYield(float vanillaYield, int rank) {
        if (!Float.isFinite(vanillaYield) || vanillaYield < 0.0F) {
            throw new IllegalArgumentException("vanillaYield must be finite and non-negative");
        }
        float increased = (float) (vanillaYield
                + vanillaYield * (BlastMiningDefaults.oreBonusPercent(rank) / 100.0D));
        return Math.min(increased, 3.0F);
    }
}
