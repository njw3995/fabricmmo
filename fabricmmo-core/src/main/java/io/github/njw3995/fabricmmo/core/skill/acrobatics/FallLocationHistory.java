package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Fixed-size block-location history matching mcMMO 2.3.000 BlockLocationHistory. */
public final class FallLocationHistory {
    public static final int CAPACITY = 50;

    private final ArrayDeque<FallLocation> order = new ArrayDeque<>(CAPACITY);
    private final Map<FallLocation, Integer> counts = new HashMap<>(CAPACITY);

    public synchronized boolean contains(FallLocation location) {
        return counts.containsKey(Objects.requireNonNull(location, "location"));
    }

    /**
     * Adds every occurrence, including duplicates. Upstream uses a multiset, so repeatedly falling
     * at one block keeps that block in the fifty-entry history for the same observable duration.
     */
    public synchronized void add(FallLocation location) {
        Objects.requireNonNull(location, "location");
        order.addFirst(location);
        counts.merge(location, 1, Integer::sum);
        if (order.size() > CAPACITY) {
            FallLocation removed = order.removeLast();
            int remaining = counts.get(removed) - 1;
            if (remaining == 0) {
                counts.remove(removed);
            } else {
                counts.put(removed, remaining);
            }
        }
    }

    synchronized int size() {
        return order.size();
    }

    public record FallLocation(String world, int x, int y, int z) {
        public FallLocation {
            Objects.requireNonNull(world, "world");
        }
    }
}
