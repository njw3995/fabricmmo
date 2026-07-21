package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.GatheringContentDefinition;
import io.github.njw3995.fabricmmo.api.content.GatheringContentRegistrar;
import io.github.njw3995.fabricmmo.api.content.GatheringContentRegistryView;
import io.github.njw3995.fabricmmo.api.registry.SkillRegistryView;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DefaultGatheringContentRegistry
        implements GatheringContentRegistrar, GatheringContentRegistryView {
    private final SkillRegistryView skills;
    private final Map<NamespacedId, GatheringContentDefinition> codeDefinitions = new TreeMap<>();
    private final Map<NamespacedId, GatheringContentDefinition> datapackDefinitions = new TreeMap<>();
    private final Set<NamespacedId> datapackDisabled = new TreeSet<>();
    private boolean frozen;

    public DefaultGatheringContentRegistry(SkillRegistryView skills) {
        this.skills = skills;
    }

    @Override
    public synchronized void registerGatheringContent(GatheringContentDefinition definition) {
        requireOpen();
        validateSkill(definition);
        if (codeDefinitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate gathering content id: " + definition.id());
        }
    }

    /** Replaces only the reloadable datapack layer; Java addon registrations stay frozen. */
    public synchronized void replaceDatapackDefinitions(
            Collection<GatheringContentDefinition> definitions,
            Collection<NamespacedId> disabledIds) {
        TreeMap<NamespacedId, GatheringContentDefinition> replacement = new TreeMap<>();
        for (GatheringContentDefinition definition : definitions) {
            validateSkill(definition);
            if (replacement.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate datapack gathering content id: " + definition.id());
            }
        }
        datapackDefinitions.clear();
        datapackDefinitions.putAll(replacement);
        datapackDisabled.clear();
        datapackDisabled.addAll(disabledIds);
    }

    @Override
    public synchronized Optional<GatheringContentDefinition> find(NamespacedId id) {
        return Optional.ofNullable(merged().get(id));
    }

    @Override
    public synchronized List<GatheringContentDefinition> definitions() {
        return List.copyOf(merged().values());
    }

    @Override
    public synchronized List<GatheringContentDefinition> definitionsForSkill(NamespacedId skillId) {
        return merged().values().stream()
                .filter(definition -> definition.skillId().equals(skillId))
                .toList();
    }

    public synchronized int codeDefinitionCount() {
        return codeDefinitions.size();
    }

    public synchronized int datapackDefinitionCount() {
        return datapackDefinitions.size();
    }

    public synchronized int datapackDisabledCount() {
        return datapackDisabled.size();
    }

    public synchronized void freeze() {
        frozen = true;
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private Map<NamespacedId, GatheringContentDefinition> merged() {
        TreeMap<NamespacedId, GatheringContentDefinition> merged = new TreeMap<>(codeDefinitions);
        datapackDisabled.forEach(merged::remove);
        merged.putAll(datapackDefinitions);
        return merged;
    }

    String datapackValidationError(GatheringContentDefinition definition) {
        return skills.find(definition.skillId()).isEmpty()
                ? "references unknown skill " + definition.skillId()
                : "";
    }

    private void validateSkill(GatheringContentDefinition definition) {
        String error = datapackValidationError(definition);
        if (!error.isEmpty()) {
            throw new IllegalStateException("Gathering content " + error);
        }
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Gathering content registry is frozen");
        }
    }
}
