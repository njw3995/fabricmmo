package io.github.njw3995.fabricmmo.core.skill.mining;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime identity for TNT primed by FabricMMO Blast Mining. */
public final class MiningBlastRegistry {
    private static final Map<UUID, BlastData> TRACKED = new ConcurrentHashMap<>();

    private MiningBlastRegistry() {
    }

    public static void track(
            UUID tntId, UUID ownerId, int rank, boolean biggerBombs, boolean demolitionsExpertise) {
        TRACKED.put(tntId, new BlastData(ownerId, rank, biggerBombs, demolitionsExpertise));
    }

    public static Optional<BlastData> find(UUID tntId) {
        return Optional.ofNullable(TRACKED.get(tntId));
    }

    public static Optional<BlastData> remove(UUID tntId) {
        return Optional.ofNullable(TRACKED.remove(tntId));
    }

    public static void clear() {
        TRACKED.clear();
    }

    public record BlastData(
            UUID ownerId, int rank, boolean biggerBombs, boolean demolitionsExpertise) {
    }
}
