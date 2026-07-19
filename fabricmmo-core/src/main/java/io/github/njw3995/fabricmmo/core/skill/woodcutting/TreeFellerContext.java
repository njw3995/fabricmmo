package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Prevents recursive normal Woodcutting processing while Tree Feller simulates block breaks. */
final class TreeFellerContext implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Map<UUID, BlockLocation> STARTING_BREAKS = new ConcurrentHashMap<>();

    private TreeFellerContext() {
        DEPTH.set(DEPTH.get() + 1);
    }

    static TreeFellerContext begin() {
        return new TreeFellerContext();
    }

    static boolean active() {
        return DEPTH.get() > 0;
    }

    static void markStartingBreak(UUID playerId, BlockLocation location) {
        STARTING_BREAKS.put(playerId, location);
    }

    static boolean startingBreak(UUID playerId, BlockLocation location) {
        return location.equals(STARTING_BREAKS.get(playerId));
    }

    static void clearStartingBreak(UUID playerId, BlockLocation location) {
        STARTING_BREAKS.remove(playerId, location);
    }

    static void clearStartingBreak(UUID playerId) {
        STARTING_BREAKS.remove(playerId);
    }

    static void reset() {
        STARTING_BREAKS.clear();
        DEPTH.remove();
    }

    @Override
    public void close() {
        int next = DEPTH.get() - 1;
        if (next <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(next);
        }
    }
}
