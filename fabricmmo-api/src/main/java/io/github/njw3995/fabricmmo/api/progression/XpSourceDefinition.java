package io.github.njw3995.fabricmmo.api.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

public record XpSourceDefinition(NamespacedId id, NamespacedId skillId, Map<String, String> metadata) {
    public XpSourceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(skillId, "skillId");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }
}
