package io.github.njw3995.fabricmmo.core.skill.alchemy;

import java.util.List;

/** Pure mcMMO 2.3.000 Alchemy formulas. */
public final class AlchemyFormula {
    private static final double LUCKY_MODIFIER = 4.0D / 3.0D;

    private AlchemyFormula() {}

    public static double catalysisSpeed(int level, int unlockLevel, int maxBonusLevel,
                                        double minSpeed, double maxSpeed, boolean lucky) {
        if (level < unlockLevel) return minSpeed;
        double speedRange = maxSpeed - minSpeed;
        double progress = maxBonusLevel <= unlockLevel ? 1.0D
                : (double) (level - unlockLevel) / (maxBonusLevel - unlockLevel);
        double speed = Math.min(maxSpeed, minSpeed + speedRange * progress);
        return speed * (lucky ? LUCKY_MODIFIER : 1.0D);
    }

    public static int concoctionsTier(int level, List<Integer> unlockLevels) {
        int tier = 0;
        for (int unlock : unlockLevels) {
            if (level < unlock) break;
            tier++;
        }
        return Math.max(1, Math.min(8, tier));
    }

    public static int potionStage(boolean inputWater, PotionShape input, PotionShape output) {
        int inputStage = stage(input);
        int outputStage = stage(output);
        if (!inputWater && inputStage == outputStage) return 5;
        return outputStage;
    }

    public static int stage(PotionShape potion) {
        int stage = 1;
        if (potion.hasEffect()) stage++;
        if (potion.upgraded() || potion.customAmplifier()) stage++;
        if (potion.extended()) stage++;
        if (potion.splashOrLingering()) stage++;
        return Math.max(1, Math.min(5, stage));
    }

    public record PotionShape(boolean hasEffect, boolean upgraded, boolean customAmplifier,
                              boolean extended, boolean splashOrLingering) {}
}
