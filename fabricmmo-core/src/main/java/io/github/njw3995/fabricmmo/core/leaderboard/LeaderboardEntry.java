package io.github.njw3995.fabricmmo.core.leaderboard;

import java.util.Objects;
import java.util.UUID;

public record LeaderboardEntry(UUID playerId, String playerName, int level, int powerLevel) {
    public LeaderboardEntry {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        if (level < 0 || powerLevel < 0) throw new IllegalArgumentException("levels must be non-negative");
    }
}
