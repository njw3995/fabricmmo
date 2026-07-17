package io.github.njw3995.fabricmmo.core.block;

/** Marks block mutations performed by vanilla random growth or bonemeal. */
public final class NaturalGrowthContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private NaturalGrowthContext() {
    }

    public static void begin() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void end() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }
}
