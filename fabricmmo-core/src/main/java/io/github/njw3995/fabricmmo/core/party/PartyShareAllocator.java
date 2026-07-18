package io.github.njw3995.fabricmmo.core.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/** Deterministic-testable implementation of upstream EQUAL and RANDOM item-share selection. */
public final class PartyShareAllocator {
    private final Random random;
    private final Map<UUID, Integer> equalModifiers = new HashMap<>();

    public PartyShareAllocator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public synchronized List<UUID> allocate(
            ShareMode mode,
            List<UUID> eligiblePlayers,
            int count,
            int itemWeight) {
        Objects.requireNonNull(mode, "mode");
        List<UUID> players = List.copyOf(Objects.requireNonNull(eligiblePlayers, "eligiblePlayers"));
        if (players.isEmpty() || count <= 0 || mode == ShareMode.NONE) {
            return List.of();
        }
        if (itemWeight <= 0) {
            throw new IllegalArgumentException("itemWeight must be positive");
        }
        ArrayList<UUID> winners = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            winners.add(switch (mode) {
                case RANDOM -> players.get(random.nextInt(players.size()));
                case EQUAL -> equalWinner(players, itemWeight);
                case NONE -> throw new IllegalStateException("NONE handled before allocation");
            });
        }
        return List.copyOf(winners);
    }

    public synchronized int modifier(UUID playerId) {
        return Math.max(10, equalModifiers.getOrDefault(playerId, 10));
    }

    public synchronized void remove(UUID playerId) {
        equalModifiers.remove(playerId);
    }

    public synchronized void clear() {
        equalModifiers.clear();
    }

    private UUID equalWinner(List<UUID> players, int itemWeight) {
        UUID winningPlayer = null;
        int highestRoll = 0;
        for (UUID playerId : players) {
            int modifier = modifier(playerId);
            int roll = random.nextInt(modifier);
            if (roll <= highestRoll) {
                equalModifiers.put(playerId, modifier + itemWeight);
                continue;
            }
            highestRoll = roll;
            if (winningPlayer != null) {
                equalModifiers.put(winningPlayer, modifier(winningPlayer) + itemWeight);
            }
            winningPlayer = playerId;
        }
        if (winningPlayer == null) {
            winningPlayer = players.getFirst();
        }
        equalModifiers.put(winningPlayer, Math.max(10, modifier(winningPlayer) - itemWeight));
        return winningPlayer;
    }
}
