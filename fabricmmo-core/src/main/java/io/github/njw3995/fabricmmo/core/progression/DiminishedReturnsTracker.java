package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory rolling XP totals matching mcMMO's per-profile diminished-return window. */
final class DiminishedReturnsTracker {
    private final Clock clock;
    private final ConcurrentHashMap<UUID, PlayerWindow> players = new ConcurrentHashMap<>();

    DiminishedReturnsTracker(Clock clock) {
        this.clock = clock;
    }

    float registeredXp(UUID playerId, NamespacedId skillId, Duration interval) {
        PlayerWindow player = players.get(playerId);
        if (player == null) {
            return 0.0F;
        }
        synchronized (player) {
            float value = player.window(skillId).totalAfter(purgeBefore(interval));
            if (player.empty()) {
                players.remove(playerId, player);
            }
            return value;
        }
    }

    void register(UUID playerId, NamespacedId skillId, float xp, Duration interval) {
        if (xp <= 0.0F) {
            return;
        }
        PlayerWindow player = players.computeIfAbsent(playerId, ignored -> new PlayerWindow());
        synchronized (player) {
            SkillWindow window = player.window(skillId);
            window.totalAfter(purgeBefore(interval));
            window.add(clock.instant(), xp);
        }
    }

    private Instant purgeBefore(Duration interval) {
        return clock.instant().minus(interval);
    }

    private static final class PlayerWindow {
        private final Map<NamespacedId, SkillWindow> skills = new HashMap<>();

        SkillWindow window(NamespacedId skillId) {
            return skills.computeIfAbsent(skillId, ignored -> new SkillWindow());
        }

        boolean empty() {
            skills.entrySet().removeIf(entry -> entry.getValue().empty());
            return skills.isEmpty();
        }
    }

    private static final class SkillWindow {
        private final Deque<Entry> entries = new ArrayDeque<>();
        private float total;

        void add(Instant instant, float xp) {
            entries.addLast(new Entry(instant, xp));
            total += xp;
        }

        float totalAfter(Instant cutoff) {
            while (!entries.isEmpty() && !entries.peekFirst().instant().isAfter(cutoff)) {
                total -= entries.removeFirst().xp();
            }
            if (Math.abs(total) < 1.0E-6F) {
                total = 0.0F;
            }
            return total;
        }

        boolean empty() {
            return entries.isEmpty();
        }
    }

    private record Entry(Instant instant, float xp) {
    }
}
