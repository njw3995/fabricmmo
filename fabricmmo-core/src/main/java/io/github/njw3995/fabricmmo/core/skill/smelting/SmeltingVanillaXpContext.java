package io.github.njw3995.fabricmmo.core.skill.smelting;

/** Extraction-scoped multiplier used while vanilla calculates and spawns furnace XP. */
public final class SmeltingVanillaXpContext {
    private static final ThreadLocal<Integer> MULTIPLIER =
            ThreadLocal.withInitial(() -> 1);

    private SmeltingVanillaXpContext() {
    }

    public static void begin(int multiplier) {
        MULTIPLIER.set(Math.max(1, multiplier));
    }

    public static int multiply(int vanillaXp) {
        return SmeltingFormula.vanillaXp(vanillaXp, MULTIPLIER.get());
    }

    public static void clear() {
        MULTIPLIER.remove();
    }
}
