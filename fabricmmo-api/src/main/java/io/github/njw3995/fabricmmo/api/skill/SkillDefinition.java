package io.github.njw3995.fabricmmo.api.skill;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SkillDefinition(
        NamespacedId id,
        SkillCategory category,
        String translationKey,
        int levelCap,
        boolean enabledByDefault,
        List<NamespacedId> parents,
        Map<String, String> metadata) {

    public SkillDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(translationKey, "translationKey");
        if (translationKey.isBlank()) {
            throw new IllegalArgumentException("translationKey must not be blank");
        }
        if (levelCap <= 0) {
            throw new IllegalArgumentException("levelCap must be positive");
        }
        parents = List.copyOf(Objects.requireNonNull(parents, "parents"));
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        if (category == SkillCategory.CHILD && parents.isEmpty()) {
            throw new IllegalArgumentException("Child skills require at least one parent");
        }
        if (category != SkillCategory.CHILD && !parents.isEmpty()) {
            throw new IllegalArgumentException("Only child skills may declare parents");
        }
        if (parents.contains(id)) {
            throw new IllegalArgumentException("A skill cannot be its own parent");
        }
    }

    public boolean childSkill() {
        return category == SkillCategory.CHILD;
    }
}
