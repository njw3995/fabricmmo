package io.github.njw3995.fabricmmo.core.skill.mining;

public final class MiningProbability {
    public static final double LUCKY_MODIFIER = 1.333D;

    private MiningProbability() {
    }

    public static double chance(int skillLevel, int maxBonusLevel, double chanceMaxPercent,
                                boolean lucky) {
        if (skillLevel < 0 || maxBonusLevel < 0 || !Double.isFinite(chanceMaxPercent)
                || chanceMaxPercent < 0.0D) {
            throw new IllegalArgumentException("Invalid probability inputs");
        }
        double percent = skillLevel >= maxBonusLevel || maxBonusLevel == 0
                ? chanceMaxPercent
                : (skillLevel / (double) maxBonusLevel) * chanceMaxPercent;
        double probability = Math.min(Math.max(0.0D, percent), chanceMaxPercent) / 100.0D;
        return Math.min(1.0D, lucky ? probability * LUCKY_MODIFIER : probability);
    }
}
