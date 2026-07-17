package io.github.njw3995.fabricmmo.core.skill.mining;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-authoritative transient ability state backed by persistent cooldown timestamps. */
public final class MiningAbilityController implements AutoCloseable {
    public static final long PREPARATION_WINDOW_MILLIS = 4_000L;
    private final MiningAbilityStore store;
    private final Clock clock;
    private final Map<UUID, RuntimeState> states = new HashMap<>();

    public MiningAbilityController(MiningAbilityStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized SuperBreakerPreparation prepareSuperBreaker(
            UUID playerId,
            int skillLevel,
            MiningSettings settings) throws IOException {
        return prepareSuperBreaker(
                playerId, skillLevel, settings, settings.superBreakerCooldownSeconds());
    }

    public synchronized SuperBreakerPreparation prepareSuperBreaker(
            UUID playerId,
            int skillLevel,
            MiningSettings settings,
            int cooldownSeconds) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(settings, "settings");
        long now = clock.millis();
        RuntimeState state = state(playerId);
        expireTransient(state, now);
        if (!settings.abilitiesEnabled()) {
            return SuperBreakerPreparation.DISABLED;
        }
        if (skillLevel < settings.superBreakerUnlockLevel()) {
            return SuperBreakerPreparation.LOCKED;
        }
        if (state.activeUntil > now) {
            return SuperBreakerPreparation.ALREADY_ACTIVE;
        }
        long remaining = cooldownRemainingMillis(
                state.persisted.superBreakerLastUsed(),
                cooldownSeconds, now);
        if (remaining > 0L) {
            return new SuperBreakerPreparation.Cooldown(secondsCeiling(remaining));
        }
        if (state.preparedUntil > now) {
            state.preparedUntil = 0L;
            return SuperBreakerPreparation.LOWERED;
        }
        state.preparedUntil = now + PREPARATION_WINDOW_MILLIS;
        return SuperBreakerPreparation.READY;
    }

    public synchronized SuperBreakerActivation activateSuperBreaker(
            UUID playerId,
            int skillLevel,
            MiningSettings settings) throws IOException {
        return activateSuperBreaker(playerId, skillLevel, settings, 0);
    }

    public synchronized SuperBreakerActivation activateSuperBreaker(
            UUID playerId,
            int skillLevel,
            MiningSettings settings,
            int activationBonusSeconds) throws IOException {
        long now = clock.millis();
        RuntimeState state = state(playerId);
        expireTransient(state, now);
        if (state.preparedUntil <= now) {
            return SuperBreakerActivation.NOT_PREPARED;
        }
        state.preparedUntil = 0L;
        int durationSeconds = settings.superBreakerDurationSeconds(skillLevel)
                + Math.max(0, activationBonusSeconds);
        state.activeUntil = now + durationSeconds * 1_000L;
        state.persisted = state.persisted.withSuperBreaker(state.activeUntil);
        store.save(playerId, state.persisted);
        return new SuperBreakerActivation.Activated(durationSeconds, state.activeUntil);
    }

    public synchronized BlastActivation activateBlastMining(
            UUID playerId,
            int skillLevel,
            MiningSettings settings) throws IOException {
        return activateBlastMining(
                playerId, skillLevel, settings, settings.blastMiningCooldownSeconds());
    }

    public synchronized BlastActivation activateBlastMining(
            UUID playerId,
            int skillLevel,
            MiningSettings settings,
            int cooldownSeconds) throws IOException {
        long now = clock.millis();
        RuntimeState state = state(playerId);
        int rank = settings.blastRank(skillLevel);
        if (!settings.abilitiesEnabled()) {
            return BlastActivation.DISABLED;
        }
        if (rank == 0) {
            return new BlastActivation.Locked(settings.blastUnlockLevel(1));
        }
        long remaining = cooldownRemainingMillis(
                state.persisted.blastMiningLastUsed(),
                cooldownSeconds, now);
        if (remaining > 0L) {
            return new BlastActivation.Cooldown(secondsCeiling(remaining));
        }
        state.persisted = state.persisted.withBlastMining(now);
        store.save(playerId, state.persisted);
        return new BlastActivation.Activated(rank);
    }

    public synchronized boolean isSuperBreakerPrepared(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        expireTransient(state, clock.millis());
        return state.preparedUntil > clock.millis();
    }

    public synchronized boolean isSuperBreakerActive(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        expireTransient(state, clock.millis());
        return state.activeUntil > clock.millis();
    }

    public synchronized int superBreakerSecondsRemaining(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        expireTransient(state, now);
        return secondsCeiling(Math.max(0L, state.activeUntil - now));
    }

    public synchronized int superBreakerCooldownRemaining(
            UUID playerId,
            MiningSettings settings) throws IOException {
        return superBreakerCooldownRemaining(
                playerId, settings.superBreakerCooldownSeconds());
    }

    public synchronized int superBreakerCooldownRemaining(
            UUID playerId,
            int cooldownSeconds) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        return secondsCeiling(cooldownRemainingMillis(
                state.persisted.superBreakerLastUsed(), cooldownSeconds, now));
    }

    public synchronized int blastCooldownRemaining(
            UUID playerId,
            MiningSettings settings) throws IOException {
        return blastCooldownRemaining(playerId, settings.blastMiningCooldownSeconds());
    }

    public synchronized int blastCooldownRemaining(
            UUID playerId,
            int cooldownSeconds) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        return secondsCeiling(cooldownRemainingMillis(
                state.persisted.blastMiningLastUsed(), cooldownSeconds, now));
    }

    public synchronized TickResult tick(UUID playerId) throws IOException {
        RuntimeState state = state(playerId);
        long now = clock.millis();
        boolean preparationExpired = state.preparedUntil > 0L && state.preparedUntil <= now;
        boolean abilityExpired = state.activeUntil > 0L && state.activeUntil <= now;
        expireTransient(state, now);
        return new TickResult(preparationExpired, abilityExpired);
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
        private MiningAbilityData persisted;
        private long preparedUntil;
        private long activeUntil;

        private RuntimeState(MiningAbilityData persisted) {
            this.persisted = persisted;
        }
    }

    public sealed interface SuperBreakerPreparation {
        SuperBreakerPreparation READY = new Simple("ready");
        SuperBreakerPreparation LOWERED = new Simple("lowered");
        SuperBreakerPreparation DISABLED = new Simple("disabled");
        SuperBreakerPreparation LOCKED = new Simple("locked");
        SuperBreakerPreparation ALREADY_ACTIVE = new Simple("already_active");

        record Simple(String name) implements SuperBreakerPreparation {
        }

        record Cooldown(int secondsRemaining) implements SuperBreakerPreparation {
        }
    }

    public sealed interface SuperBreakerActivation {
        SuperBreakerActivation NOT_PREPARED = new Simple("not_prepared");

        record Simple(String name) implements SuperBreakerActivation {
        }

        record Activated(int durationSeconds, long activeUntil) implements SuperBreakerActivation {
        }
    }

    public sealed interface BlastActivation {
        BlastActivation DISABLED = new Simple("disabled");

        record Simple(String name) implements BlastActivation {
        }

        record Locked(int requiredLevel) implements BlastActivation {
        }

        record Cooldown(int secondsRemaining) implements BlastActivation {
        }

        record Activated(int rank) implements BlastActivation {
        }
    }

    public record TickResult(boolean preparationExpired, boolean abilityExpired) {
    }
}
