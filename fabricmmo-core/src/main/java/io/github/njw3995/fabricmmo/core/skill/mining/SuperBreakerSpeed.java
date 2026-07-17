package io.github.njw3995.fabricmmo.core.skill.mining;

/** Exact additive Mining Efficiency amount for Super Breaker's temporary enchantment boost. */
public final class SuperBreakerSpeed {
    private SuperBreakerSpeed() {
    }

    public static double additionalMiningEfficiency(int existingEfficiencyLevel, int bonusLevels) {
        int existing = Math.max(0, existingEfficiencyLevel);
        int bonus = Math.max(0, bonusLevels);
        if (bonus == 0) {
            return 0.0D;
        }
        int boosted = existing + bonus;
        return efficiencyContribution(boosted) - efficiencyContribution(existing);
    }

    private static double efficiencyContribution(int level) {
        return level <= 0 ? 0.0D : (double) level * level + 1.0D;
    }
}
