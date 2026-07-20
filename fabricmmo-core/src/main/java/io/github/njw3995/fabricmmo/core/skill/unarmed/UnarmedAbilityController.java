package io.github.njw3995.fabricmmo.core.skill.unarmed;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative Berserk preparation, duration, and persisted cooldown. */
public final class UnarmedAbilityController implements AutoCloseable {
    public static final long PREPARATION_WINDOW_MILLIS = 4_000L;
    private final UnarmedAbilityStore store;
    private final Clock clock;
    private final Map<UUID, RuntimeState> states = new HashMap<>();

    public UnarmedAbilityController(UnarmedAbilityStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Preparation prepare(
            UUID playerId, int level, UnarmedSettings settings, int cooldownSeconds)
            throws IOException {
        long now = clock.millis();
        RuntimeState state = state(playerId);
        expire(state, now);
        if (!settings.abilitiesEnabled()) return Preparation.DISABLED;
        if (state.activeUntil > now) return Preparation.ALREADY_ACTIVE;
        int levelsRequired = settings.berserkUnlockLevel() - level;
        if (levelsRequired > 0) return new Preparation.Locked(levelsRequired);
        long remaining = remaining(state.persisted.berserkLastUsed(), cooldownSeconds, now);
        if (remaining > 0L) return new Preparation.Cooldown(seconds(remaining));
        if (state.preparedUntil > now) {
            state.preparedUntil = 0L;
            return Preparation.LOWERED;
        }
        state.preparedUntil = now + PREPARATION_WINDOW_MILLIS;
        return Preparation.READY;
    }

    public synchronized Activation activate(
            UUID playerId,
            int level,
            UnarmedSettings settings,
            int cooldownSeconds,
            int activationBonusSeconds) throws IOException {
        long now = clock.millis();
        RuntimeState state = state(playerId);
        expire(state, now);
        if (state.preparedUntil <= now) return Activation.NOT_PREPARED;
        int levelsRequired = settings.berserkUnlockLevel() - level;
        if (levelsRequired > 0) return new Activation.Locked(levelsRequired);
        long remaining = remaining(state.persisted.berserkLastUsed(), cooldownSeconds, now);
        if (remaining > 0L) return new Activation.Cooldown(seconds(remaining));
        state.preparedUntil = 0L;
        int duration = settings.berserkDurationSeconds(level) + Math.max(0, activationBonusSeconds);
        state.activeUntil = now + duration * 1_000L;
        // Upstream stores the ability deactivation timestamp for cooldown calculations.
        state.persisted = new UnarmedAbilityData(state.activeUntil);
        store.save(playerId, state.persisted);
        return new Activation.Activated(duration, state.activeUntil);
    }

    public synchronized boolean isPrepared(UUID playerId) throws IOException {
        RuntimeState state = state(playerId); long now = clock.millis(); expire(state, now);
        return state.preparedUntil > now;
    }
    public synchronized boolean isActive(UUID playerId) throws IOException {
        RuntimeState state = state(playerId); long now = clock.millis(); expire(state, now);
        return state.activeUntil > now;
    }
    public synchronized int activeSecondsRemaining(UUID playerId) throws IOException {
        RuntimeState state = state(playerId); long now = clock.millis(); expire(state, now);
        return seconds(Math.max(0L, state.activeUntil - now));
    }
    public synchronized int cooldownRemaining(UUID playerId, int cooldownSeconds) throws IOException {
        RuntimeState state = state(playerId); long now = clock.millis();
        return seconds(remaining(state.persisted.berserkLastUsed(), cooldownSeconds, now));
    }
    public synchronized TickResult tick(UUID playerId) throws IOException {
        RuntimeState state = state(playerId); long now = clock.millis();
        boolean preparationExpired = state.preparedUntil > 0L && state.preparedUntil <= now;
        boolean abilityExpired = state.activeUntil > 0L && state.activeUntil <= now;
        expire(state, now);
        return new TickResult(preparationExpired, abilityExpired);
    }
    public synchronized void reset(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        state.persisted = UnarmedAbilityData.EMPTY;
        state.preparedUntil = 0L;
        state.activeUntil = 0L;
        store.save(playerId, state.persisted);
    }
    public synchronized void removeTransient(UUID playerId) {
        RuntimeState state = states.get(playerId);
        if (state != null) { state.preparedUntil = 0L; state.activeUntil = 0L; }
    }
    @Override public synchronized void close() throws IOException { states.clear(); store.close(); }

    private RuntimeState state(UUID id) throws IOException {
        RuntimeState existing = states.get(id);
        if (existing != null) return existing;
        RuntimeState loaded = new RuntimeState(store.load(id));
        states.put(id, loaded);
        return loaded;
    }
    private static void expire(RuntimeState state, long now) {
        if (state.preparedUntil <= now) state.preparedUntil = 0L;
        if (state.activeUntil <= now) state.activeUntil = 0L;
    }
    private static long remaining(long lastUsed, int cooldown, long now) {
        if (lastUsed <= 0L || cooldown <= 0) return 0L;
        return Math.max(0L, lastUsed + cooldown * 1_000L - now);
    }
    private static int seconds(long millis) { return millis <= 0 ? 0 : (int) ((millis + 999L) / 1_000L); }

    private static final class RuntimeState {
        private UnarmedAbilityData persisted;
        private long preparedUntil;
        private long activeUntil;
        private RuntimeState(UnarmedAbilityData persisted) { this.persisted = persisted; }
    }
    public sealed interface Preparation {
        Preparation READY = new Simple("ready");
        Preparation LOWERED = new Simple("lowered");
        Preparation DISABLED = new Simple("disabled");
        Preparation ALREADY_ACTIVE = new Simple("already_active");
        record Simple(String name) implements Preparation { }
        record Locked(int levelsRequired) implements Preparation { }
        record Cooldown(int secondsRemaining) implements Preparation { }
    }
    public sealed interface Activation {
        Activation NOT_PREPARED = new Simple("not_prepared");
        record Simple(String name) implements Activation { }
        record Locked(int levelsRequired) implements Activation { }
        record Cooldown(int secondsRemaining) implements Activation { }
        record Activated(int durationSeconds, long activeUntil) implements Activation { }
    }
    public record TickResult(boolean preparationExpired, boolean abilityExpired) { }
}
