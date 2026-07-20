package io.github.njw3995.fabricmmo.core.skill.acrobatics;

/** Per-player rapid-roll XP throttle from mcMMO 2.3.000 AcrobaticsManager. */
public final class RollXpThrottle {
    static final long BASE_INTERVAL_MILLIS = 3_000L;
    static final long INITIAL_PENALTY_MILLIS = 10_000L;

    private long cooldownUntil;
    private long nextPenalty = INITIAL_PENALTY_MILLIS;

    public synchronized boolean tryConsume(long nowMillis, boolean exploitPreventionEnabled) {
        if (!exploitPreventionEnabled) {
            return true;
        }
        if (nowMillis >= cooldownUntil) {
            cooldownUntil = nowMillis + BASE_INTERVAL_MILLIS;
            nextPenalty = INITIAL_PENALTY_MILLIS;
            return true;
        }
        cooldownUntil += nextPenalty;
        nextPenalty += 1_000L;
        return false;
    }

    synchronized long cooldownUntil() {
        return cooldownUntil;
    }
}
