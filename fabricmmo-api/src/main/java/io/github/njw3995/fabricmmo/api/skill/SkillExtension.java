package io.github.njw3995.fabricmmo.api.skill;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

public record SkillExtension(NamespacedId targetSkill, NamespacedId extensionId, Map<String, String> metadata) {
    public SkillExtension {
        Objects.requireNonNull(targetSkill, "targetSkill");
        Objects.requireNonNull(extensionId, "extensionId");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }
}
