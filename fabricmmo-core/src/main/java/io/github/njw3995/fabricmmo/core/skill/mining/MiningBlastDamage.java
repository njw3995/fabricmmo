package io.github.njw3995.fabricmmo.core.skill.mining;

/** Pure Blast Mining damage rules shared by the Fabric damage hook and tests. */
public final class MiningBlastDamage {
    public static final float BYSTANDER_DAMAGE_CAP = 24.0F;

    private MiningBlastDamage() {
    }

    public static float ownerDamage(float rawDamage, double decreasePercent) {
        requireDamage(rawDamage);
        if (!Double.isFinite(decreasePercent) || decreasePercent < 0.0D || decreasePercent > 100.0D) {
            throw new IllegalArgumentException("decreasePercent must be in [0,100]");
        }
        return (float) (rawDamage * ((100.0D - decreasePercent) / 100.0D));
    }

    public static float bystanderDamage(float rawDamage) {
        requireDamage(rawDamage);
        return Math.min(rawDamage, BYSTANDER_DAMAGE_CAP);
    }

    private static void requireDamage(float damage) {
        if (!Float.isFinite(damage) || damage < 0.0F) {
            throw new IllegalArgumentException("damage must be finite and non-negative");
        }
    }
}
