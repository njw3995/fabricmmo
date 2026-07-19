package io.github.njw3995.fabricmmo.core.skill.excavation;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative Giga Drill Breaker preparation, duration, and persisted cooldown. */
public final class ExcavationAbilityController implements AutoCloseable {
    public static final long PREPARATION_WINDOW_MILLIS = 4_000L;
    private final ExcavationAbilityStore store;
    private final Clock clock;
    private final Map<UUID, RuntimeState> states = new HashMap<>();

    public ExcavationAbilityController(ExcavationAbilityStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Preparation prepare(
            UUID playerId,
            int skillLevel,
            ExcavationSettings settings,
            int cooldownSeconds) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(settings, "settings");
        long now = clock.millis();
        RuntimeState state = state(playerId);
        expireTransient(state, now);
        if (!settings.abilitiesEnabled()) {
            return Preparation.DISABLED;
        }
        if (state.activeUntil > now) {
            return Preparation.ALREADY_ACTIVE;
        }
        int levelsRequired = settings.gigaDrillUnlockLevel() - skillLevel;
        if (levelsRequired > 0) {
            return new Preparation.Locked(levelsRequired);
        }
        long remaining = cooldownRemainingMillis(
                state.persisted.gigaDrillLastUsed(), cooldownSeconds, now);
        if (remaining > 0L) {
            return new Preparation.Cooldown(secondsCeiling(remaining));
        }
        if (state.preparedUntil > now) {
            state.preparedUntil = 0L;
            return Preparation.LOWERED;
        }
        state.preparedUntil = now + PREPARATION_WINDOW_MILLIS;
        return Preparation.READY;
    }

    public synchronized Activation activate(
            UUID playerId,
            int skillLevel,
            ExcavationSettings settings,
            int cooldownSeconds,
            int activationBonusSeconds) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(settings, "settings");
        long now = clock.millis();
        RuntimeState state = state(playerId);
        expireTransient(state, now);
        if (state.preparedUntil <= now) {
            return Activation.NOT_PREPARED;
        }
        int levelsRequired = settings.gigaDrillUnlockLevel() - skillLevel;
        if (levelsRequired > 0) {
            return new Activation.Locked(levelsRequired);
        }
        long remaining = cooldownRemainingMillis(
                state.persisted.gigaDrillLastUsed(), cooldownSeconds, now);
        if (remaining > 0L) {
            return new Activation.Cooldown(secondsCeiling(remaining));
        }
        state.preparedUntil = 0L;
        int durationSeconds = settings.gigaDrillDurationSeconds(skillLevel)
                + Math.max(0, activationBonusSeconds);
        state.activeUntil = now + durationSeconds * 1_000L;
        // Upstream stores the deactivation timestamp for Giga Drill Breaker.
        state.persisted = new ExcavationAbilityData(state.activeUntil);
        store.save(playerId, state.persisted);
        return new Activation.Activated(durationSeconds, state.activeUntil);
    }

    public synchronized boolean isPrepared(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        expireTransient(state, now);
        return state.preparedUntil > now;
    }

    public synchronized boolean isActive(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        expireTransient(state, now);
        return state.activeUntil > now;
    }

    public synchronized int activeSecondsRemaining(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        expireTransient(state, now);
        return secondsCeiling(Math.max(0L, state.activeUntil - now));
    }

    public synchronized int cooldownRemaining(UUID playerId, int cooldownSeconds)
            throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        return secondsCeiling(cooldownRemainingMillis(
                state.persisted.gigaDrillLastUsed(), cooldownSeconds, now));
    }

    public synchronized TickResult tick(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        boolean preparationExpired = state.preparedUntil > 0L && state.preparedUntil <= now;
        boolean abilityExpired = state.activeUntil > 0L && state.activeUntil <= now;
        expireTransient(state, now);
        return new TickResult(preparationExpired, abilityExpired);
    }

    public synchronized void reset(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        state.persisted = ExcavationAbilityData.EMPTY;
        state.preparedUntil = 0L;
        state.activeUntil = 0L;
        store.save(playerId, state.persisted);
    }

    public synchronized void removeTransient(UUID playerId) {
        RuntimeState state = states.get(playerId);
        if (state != null) {
            state.preparedUntil = 0L;
            state.activeUntil = 0L;
        }
    }

    @Override
    public synchronized void close() throws IOException {
        states.clear();
        store.close();
    }

    private RuntimeState state(UUID playerId) throws IOException {
        RuntimeState existing = states.get(playerId);
        if (existing != null) {
            return existing;
        }
        RuntimeState loaded = new RuntimeState(store.load(playerId));
        states.put(playerId, loaded);
        return loaded;
    }

    private static void expireTransient(RuntimeState state, long now) {
        if (state.preparedUntil <= now) {
            state.preparedUntil = 0L;
        }
        if (state.activeUntil <= now) {
            state.activeUntil = 0L;
        }
    }

    private static long cooldownRemainingMillis(long lastUsed, int cooldownSeconds, long now) {
        if (lastUsed <= 0L || cooldownSeconds <= 0) {
            return 0L;
        }
        return Math.max(0L, lastUsed + cooldownSeconds * 1_000L - now);
    }

    private static int secondsCeiling(long milliseconds) {
        return milliseconds <= 0L ? 0 : (int) ((milliseconds + 999L) / 1_000L);
    }

    private static final class RuntimeState {
        private ExcavationAbilityData persisted;
        private long preparedUntil;
        private long activeUntil;

        private RuntimeState(ExcavationAbilityData persisted) {
            this.persisted = persisted;
        }
    }

    public sealed interface Preparation {
        Preparation READY = new Simple("ready");
        Preparation LOWERED = new Simple("lowered");
        Preparation DISABLED = new Simple("disabled");
        Preparation ALREADY_ACTIVE = new Simple("already_active");

        record Simple(String name) implements Preparation {
        }

        record Locked(int levelsRequired) implements Preparation {
        }

        record Cooldown(int secondsRemaining) implements Preparation {
        }
    }

    public sealed interface Activation {
        Activation NOT_PREPARED = new Simple("not_prepared");

        record Simple(String name) implements Activation {
        }

        record Locked(int levelsRequired) implements Activation {
        }

        record Cooldown(int secondsRemaining) implements Activation {
        }

        record Activated(int durationSeconds, long activeUntil) implements Activation {
        }
    }

    public record TickResult(boolean preparationExpired, boolean abilityExpired) {
    }
}
