package io.github.njw3995.fabricmmo.core.skill.repair;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;

/** Pure confirmation-window state machine with immutable identity snapshots. */
final class TimedIdentityConfirmation<K, T, S> {
    private final Clock clock;
    private final Duration window;
    private final Function<T, S> snapshotter;
    private final BiPredicate<S, T> matcher;
    private final Map<UUID, Pending<K, S>> pending = new HashMap<>();

    TimedIdentityConfirmation(
            Clock clock,
            Duration window,
            Function<T, S> snapshotter,
            BiPredicate<S, T> matcher) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.window = Objects.requireNonNull(window, "window");
        this.snapshotter = Objects.requireNonNull(snapshotter, "snapshotter");
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    synchronized boolean confirmOrPrompt(UUID playerId, K kind, T item) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(item, "item");
        Instant now = clock.instant();
        Pending<K, S> current = pending.get(playerId);
        if (matches(current, kind, item, now)) {
            return true;
        }
        pending.put(playerId, new Pending<>(kind, snapshotter.apply(item), now.plus(window)));
        return false;
    }

    synchronized boolean isAwaiting(UUID playerId, K kind, T item) {
        return matches(pending.get(playerId), kind, item, clock.instant());
    }

    synchronized void rebind(UUID playerId, K kind, T item) {
        Pending<K, S> current = pending.get(playerId);
        if (current != null && Objects.equals(current.kind(), kind)
                && !expired(current, clock.instant())) {
            pending.put(playerId,
                    new Pending<>(kind, snapshotter.apply(item), current.expiresAt()));
        }
    }

    synchronized boolean blocksItemUse(UUID playerId, T item) {
        Pending<K, S> current = pending.get(playerId);
        Instant now = clock.instant();
        if (current == null) {
            return false;
        }
        if (expired(current, now)) {
            pending.remove(playerId);
            return false;
        }
        return matcher.test(current.snapshot(), item);
    }

    synchronized boolean cancel(UUID playerId, K kind) {
        Pending<K, S> current = pending.get(playerId);
        if (current == null || !Objects.equals(current.kind(), kind)
                || expired(current, clock.instant())) {
            pending.remove(playerId);
            return false;
        }
        pending.remove(playerId);
        return true;
    }

    synchronized void clear(UUID playerId) {
        pending.remove(playerId);
    }

    synchronized void clearAll() {
        pending.clear();
    }

    private boolean matches(Pending<K, S> current, K kind, T item, Instant now) {
        return current != null && Objects.equals(current.kind(), kind)
                && !expired(current, now) && matcher.test(current.snapshot(), item);
    }

    private static boolean expired(Pending<?, ?> current, Instant now) {
        return !now.isBefore(current.expiresAt());
    }

    private record Pending<K, S>(K kind, S snapshot, Instant expiresAt) {
    }
}
