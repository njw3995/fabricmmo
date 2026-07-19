package io.github.njw3995.fabricmmo.core.skill.excavation;

/** mcMMO static skill RNG with the Lucky 1.333 multiplier. */
public final class ExcavationProbability {
    public static final double LUCKY_MODIFIER = 1.333D;

    private ExcavationProbability() {
    }

    public static boolean succeeds(double roll, double chancePercent, boolean lucky) {
        if (!Double.isFinite(roll) || roll < 0.0D || roll >= 1.0D) {
            throw new IllegalArgumentException("roll must be in [0,1)");
        }
        double chance = Math.max(0.0D, chancePercent) * (lucky ? LUCKY_MODIFIER : 1.0D);
        return roll * 100.0D <= Math.min(100.0D, chance);
    }
}
