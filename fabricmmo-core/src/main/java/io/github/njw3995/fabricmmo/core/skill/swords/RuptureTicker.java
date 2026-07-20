package io.github.njw3995.fabricmmo.core.skill.swords;

/** Pure pinned mcMMO 2.3.000 Rupture task timing state. */
final class RuptureTicker {
    static final int DAMAGE_TICK_INTERVAL = 10;
    private static final int ANIMATION_TICK_INTERVAL = 1;

    private final int totalTickCeiling;
    private int ruptureTick;
    private int damageTickTracker;
    private int animationTick = ANIMATION_TICK_INTERVAL;
    private int totalTicks;

    RuptureTicker(int expireTick) {
        totalTickCeiling = Math.min(Math.max(0, expireTick), 200);
    }

    Step tick() {
        totalTicks++;
        if (totalTicks >= totalTickCeiling) {
            return Step.EXPIRED;
        }

        ruptureTick++;
        damageTickTracker++;
        if (damageTickTracker < DAMAGE_TICK_INTERVAL) {
            return Step.NONE;
        }

        damageTickTracker = 0;
        if (animationTick >= ANIMATION_TICK_INTERVAL) {
            animationTick = 0;
            return Step.DAMAGE_AND_ANIMATE;
        }
        animationTick++;
        return Step.DAMAGE;
    }

    void refresh() {
        damageTickTracker = DAMAGE_TICK_INTERVAL;
        ruptureTick = 0;
    }

    int totalTicks() {
        return totalTicks;
    }

    enum Step {
        NONE,
        DAMAGE,
        DAMAGE_AND_ANIMATE,
        EXPIRED
    }
}
