package io.github.njw3995.fabricmmo.core.skill.mining;

/** Prevents vanilla explosion drops while FabricMMO emits upstream-equivalent Blast Mining drops. */
public final class BlastDropSuppression {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private BlastDropSuppression() {
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }

    public static void begin() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void end() {
        int remaining = DEPTH.get() - 1;
        if (remaining <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(remaining);
        }
    }
}
