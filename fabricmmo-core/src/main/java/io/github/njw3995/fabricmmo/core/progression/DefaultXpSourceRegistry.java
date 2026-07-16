package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpSourceDefinition;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistrar;
import io.github.njw3995.fabricmmo.api.progression.XpSourceRegistryView;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class DefaultXpSourceRegistry implements XpSourceRegistrar, XpSourceRegistryView {
    private final SkillRegistryView skills;
    private final Map<NamespacedId, XpSourceDefinition> sources = new TreeMap<>();
    private boolean frozen;

    public DefaultXpSourceRegistry(SkillRegistryView skills) {
        this.skills = skills;
    }

    @Override
    public synchronized void registerXpSource(XpSourceDefinition source) {
        requireOpen();
        if (skills.find(source.skillId()).isEmpty()) {
            throw new IllegalStateException("XP source references unknown skill: " + source.skillId());
        }
        if (sources.putIfAbsent(source.id(), source) != null) {
            throw new IllegalStateException("Duplicate XP source id: " + source.id());
        }
    }

    @Override
    public synchronized Optional<XpSourceDefinition> find(NamespacedId id) {
        return Optional.ofNullable(sources.get(id));
    }

    @Override
    public synchronized List<XpSourceDefinition> sources() {
        return List.copyOf(sources.values());
    }

    @Override
    public synchronized List<XpSourceDefinition> sourcesForSkill(NamespacedId skillId) {
        return sources.values().stream()
                .filter(source -> source.skillId().equals(skillId))
                .toList();
    }

    public synchronized void freeze() {
        frozen = true;
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("XP source registry is frozen");
        }
    }
}
