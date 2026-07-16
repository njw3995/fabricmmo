package io.github.njw3995.fabricmmo.core.persistence;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProgressionStore implements ProgressionStore {
    private final ConcurrentHashMap<UUID, PlayerProgressionData> data = new ConcurrentHashMap<>();

    @Override
    public PlayerProgressionData load(UUID playerId) {
        return data.getOrDefault(playerId, PlayerProgressionData.empty(playerId));
    }

    @Override
    public void save(PlayerProgressionData progressionData) {
        data.compute(progressionData.playerId(), (ignored, existing) -> {
            if (existing != null && progressionData.revision() < existing.revision()) {
                throw new IllegalStateException("Refusing stale progression save for " + progressionData.playerId());
            }
            return progressionData;
        });
    }
}
