package io.github.njw3995.fabricmmo.client;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.skill.SkillDefinition;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Client-safe metadata only. It contains no authoritative progression or RNG state. */
public final class ClientSkillMetadataCache {
    private final Map<NamespacedId, SkillDefinition> skills = new TreeMap<>();

    public synchronized void replace(List<SkillDefinition> definitions) {
        skills.clear();
        for (SkillDefinition definition : definitions) {
            skills.put(definition.id(), definition);
        }
    }

    public synchronized List<SkillDefinition> snapshot() {
        return List.copyOf(skills.values());
    }
}
