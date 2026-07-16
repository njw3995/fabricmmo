package io.github.njw3995.fabricmmo.api.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record XpAwardRequest(UUID playerId, NamespacedId skillId, NamespacedId sourceId, double rawXp,
                             Map<String, String> context) {
    public XpAwardRequest {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(sourceId, "sourceId");
        if (!Double.isFinite(rawXp) || rawXp <= 0.0) {
            throw new IllegalArgumentException("rawXp must be finite and positive");
        }
        context = Map.copyOf(Objects.requireNonNull(context, "context"));
    }
}
