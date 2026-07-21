package io.github.njw3995.fabricmmo.core.skill.smelting;

/** Pure child-level, Fuel Efficiency, Second Smelt, and vanilla XP formulas. */
public final class SmeltingFormula {
    private SmeltingFormula() {
    }

    public static int childLevel(int miningLevel, int repairLevel) {
        if (miningLevel < 0 || repairLevel < 0) {
            throw new IllegalArgumentException("Parent levels must be non-negative");
        }
        return (int) (((long) miningLevel + repairLevel) / 2L);
    }

    public static int fuelMultiplier(int rank) {
        return switch (rank) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 1;
        };
    }

    public static int fuelTime(int vanillaBurnTime, int fuelEfficiencyRank) {
        if (vanillaBurnTime <= 0) {
            return 0;
        }
        return Math.min(
                Short.MAX_VALUE,
                Math.max(1, vanillaBurnTime * fuelMultiplier(fuelEfficiencyRank)));
    }

    public static boolean hasRoomForSecondSmelt(int currentAmount, int maximumStackSize) {
        if (currentAmount < 0 || maximumStackSize <= 0 || currentAmount > maximumStackSize) {
            throw new IllegalArgumentException("Invalid result slot state");
        }
        return currentAmount == 0 || currentAmount <= maximumStackSize - 2;
    }

    public static int vanillaXp(int vanillaXp, int multiplier) {
        if (vanillaXp < 0) {
            throw new IllegalArgumentException("Vanilla XP must be non-negative");
        }
        return vanillaXp * Math.max(1, multiplier);
    }
}
