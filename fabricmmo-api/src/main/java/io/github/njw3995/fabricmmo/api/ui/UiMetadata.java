package io.github.njw3995.fabricmmo.api.ui;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

public record UiMetadata(NamespacedId skillId, String iconId, Map<String, String> properties) {
    public UiMetadata {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(iconId, "iconId");
        if (iconId.isBlank() || !iconId.contains(":")) {
            throw new IllegalArgumentException("iconId must be a namespaced identifier string");
        }
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
    }
}
