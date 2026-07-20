package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Per-attacker Dodge XP cap copied from the pinned mcMMO 2.3.000 behavior. */
public final class DodgeXpTracker {
    public static final int MAX_XP_REWARDS_PER_MOB = 6;
    public static final long IDLE_RESET_MILLIS = 60_000L;

    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();
    private final AtomicLong lastSweepTime = new AtomicLong();

    public boolean tryConsume(UUID mobId, long nowMillis) {
        Objects.requireNonNull(mobId, "mobId");
        RewardDecision decision = new RewardDecision();
        states.compute(mobId, (ignored, previous) -> {
            if (previous == null || previous.stale(nowMillis)) {
                decision.rewardable = true;
                return new State(1, nowMillis);
            }
            decision.rewardable = previous.rewards < MAX_XP_REWARDS_PER_MOB;
            return new State(
                    decision.rewardable ? previous.rewards + 1 : previous.rewards,
                    nowMillis);
        });
        sweepStaleEntries(nowMillis);
        return decision.rewardable;
    }

    public void clear() {
        states.clear();
        lastSweepTime.set(0L);
    }

    int trackedMobCount() {
        return states.size();
    }

    int rewardsFor(UUID mobId) {
        State state = states.get(mobId);
        return state == null ? 0 : state.rewards;
    }

    private void sweepStaleEntries(long nowMillis) {
        long previousSweep = lastSweepTime.get();
        if (nowMillis - previousSweep < IDLE_RESET_MILLIS
                || !lastSweepTime.compareAndSet(previousSweep, nowMillis)) {
            return;
        }
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            if (entry.getValue().stale(nowMillis)) {
                states.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    private static final class RewardDecision {
        private boolean rewardable;
    }

    private record State(int rewards, long lastDodgeMillis) {
        private boolean stale(long nowMillis) {
            return nowMillis - lastDodgeMillis >= IDLE_RESET_MILLIS;
        }
    }
}
