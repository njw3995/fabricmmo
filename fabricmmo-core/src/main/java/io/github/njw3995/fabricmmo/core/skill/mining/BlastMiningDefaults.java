package io.github.njw3995.fabricmmo.core.skill.mining;

import java.util.Arrays;

public final class BlastMiningDefaults {
    public static final int MAX_RANK = 8;
    public static final int REMOTE_DETONATION_DISTANCE = 100;
    public static final double PVP_DAMAGE_CAP = 24.0D;

    private static final double[] DAMAGE_DECREASE = {0, 0, 0, 0, 25, 25, 50, 50, 100};
    private static final double[] ORE_BONUS = {0, 35, 40, 45, 50, 55, 60, 65, 70};
    private static final double[] DEBRIS_REDUCTION = {0, 10, 20, 30, 30, 30, 30, 30, 30};
    private static final int[] DROP_MULTIPLIER = {0, 1, 1, 2, 2, 2, 2, 3, 3};
    private static final double[] RADIUS_MODIFIER = {0, 1, 1, 2, 2, 3, 3, 4, 4};
    private static final int[] UNLOCK_STANDARD = {0, 10, 25, 35, 50, 65, 75, 85, 100};
    private static final int[] UNLOCK_RETRO = {0, 100, 250, 350, 500, 650, 750, 850, 1000};

    private BlastMiningDefaults() {
    }

    public static double damageDecreasePercent(int rank) {
        return value(DAMAGE_DECREASE, rank);
    }

    public static double oreBonusPercent(int rank) {
        return value(ORE_BONUS, rank);
    }

    public static double debrisReductionPercent(int rank) {
        return value(DEBRIS_REDUCTION, rank);
    }

    public static int dropMultiplier(int rank) {
        requireRank(rank);
        return DROP_MULTIPLIER[rank];
    }

    public static double radiusModifier(int rank) {
        return value(RADIUS_MODIFIER, rank);
    }

    public static int rankForLevel(int level, boolean retro) {
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        int[] thresholds = retro ? UNLOCK_RETRO : UNLOCK_STANDARD;
        int rank = 0;
        for (int candidate = 1; candidate <= MAX_RANK; candidate++) {
            if (level < thresholds[candidate]) {
                break;
            }
            rank = candidate;
        }
        return rank;
    }

    public static int firstDamageReductionUnlock(boolean retro) {
        return firstPositiveUnlock(DAMAGE_DECREASE, retro);
    }

    public static int firstRadiusIncreaseUnlock(boolean retro) {
        return firstPositiveUnlock(RADIUS_MODIFIER, retro);
    }

    private static int firstPositiveUnlock(double[] values, boolean retro) {
        int rank = 0;
        for (int candidate = 1; candidate <= MAX_RANK; candidate++) {
            if (values[candidate] > 0.0D) {
                rank = candidate;
                break;
            }
        }
        return rank == 0 ? 0 : (retro ? UNLOCK_RETRO[rank] : UNLOCK_STANDARD[rank]);
    }

    private static double value(double[] values, int rank) {
        requireRank(rank);
        return values[rank];
    }

    private static void requireRank(int rank) {
        if (rank < 0 || rank > MAX_RANK) {
            throw new IllegalArgumentException("rank must be in [0," + MAX_RANK + "]: " + rank);
        }
    }

    static int[] standardUnlocksForTest() {
        return Arrays.copyOf(UNLOCK_STANDARD, UNLOCK_STANDARD.length);
    }
}
