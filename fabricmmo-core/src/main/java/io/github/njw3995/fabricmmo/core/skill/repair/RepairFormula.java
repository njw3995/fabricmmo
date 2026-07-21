package io.github.njw3995.fabricmmo.core.skill.repair;

/** Pure mcMMO 2.3.000 Repair durability and XP formulas. */
public final class RepairFormula {
    private RepairFormula() {
    }

    public static int repairedDamage(
            int currentDamage,
            int maximumDurability,
            int minimumQuantity,
            int skillLevel,
            boolean repairMastery,
            double masteryMaxBonusPercentage,
            int masteryMaxBonusLevel,
            boolean superRepair) {
        if (currentDamage < 0 || maximumDurability <= 0 || minimumQuantity <= 0
                || skillLevel < 0 || masteryMaxBonusLevel <= 0
                || !Double.isFinite(masteryMaxBonusPercentage)
                || masteryMaxBonusPercentage < 0.0D) {
            throw new IllegalArgumentException("Invalid Repair formula input");
        }
        int repairAmount = maximumDurability / minimumQuantity;
        if (repairMastery) {
            double maxBonus = masteryMaxBonusPercentage / 100.0D;
            double levelBonus = (masteryMaxBonusPercentage / masteryMaxBonusLevel)
                    * (skillLevel / 100.0D);
            double bonus = repairAmount * Math.min(levelBonus, maxBonus);
            repairAmount = (int) (repairAmount + bonus); // Explicitly preserve upstream compound-assignment truncation.
        }
        if (superRepair) {
            repairAmount = (int) (repairAmount * 2.0D); // Explicitly preserve upstream compound-assignment truncation.
        }
        if (repairAmount <= 0 || repairAmount > Short.MAX_VALUE) {
            repairAmount = Short.MAX_VALUE;
        }
        return Math.max(currentDamage - repairAmount, 0);
    }

    public static double xp(
            int oldDamage,
            int newDamage,
            int maximumDurability,
            double itemXpMultiplier,
            double baseXp,
            double materialXpMultiplier) {
        if (oldDamage < 0 || newDamage < 0 || maximumDurability <= 0
                || !Double.isFinite(itemXpMultiplier) || itemXpMultiplier < 0.0D
                || !Double.isFinite(baseXp) || baseXp < 0.0D
                || !Double.isFinite(materialXpMultiplier) || materialXpMultiplier < 0.0D) {
            throw new IllegalArgumentException("Invalid Repair XP input");
        }
        return ((oldDamage - newDamage) / (double) maximumDurability)
                * itemXpMultiplier * baseXp * materialXpMultiplier;
    }
}
