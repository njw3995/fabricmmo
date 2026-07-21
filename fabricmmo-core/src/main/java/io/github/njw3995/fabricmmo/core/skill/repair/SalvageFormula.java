package io.github.njw3995.fabricmmo.core.skill.repair;

/** Pure deterministic Scrap Collector calculations from mcMMO 2.3.000. */
public final class SalvageFormula {
    private SalvageFormula() {
    }

    public static int recoverableAmount(int currentDamage, int maximumDurability, int baseAmount) {
        if (currentDamage < 0 || maximumDurability < 0 || baseAmount <= 0) {
            throw new IllegalArgumentException("Invalid salvage input");
        }
        double percentRemaining = maximumDurability <= 0
                ? 1.0D
                : (maximumDurability - currentDamage) / (double) maximumDurability;
        if (percentRemaining <= 0.0D) {
            return 0;
        }
        return (int) Math.floor(baseAmount * percentRemaining);
    }

    public static int scrapCollectorLimit(int rank) {
        if (rank < 0) {
            throw new IllegalArgumentException("rank must be non-negative");
        }
        return rank == 1 ? 1 : rank * 2;
    }

    public static int recoveredAmount(
            int currentDamage,
            int maximumDurability,
            int baseAmount,
            int scrapCollectorRank) {
        return Math.min(
                recoverableAmount(currentDamage, maximumDurability, baseAmount),
                scrapCollectorLimit(scrapCollectorRank));
    }
}
