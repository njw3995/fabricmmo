package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentDefinition;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentRegistrar;
import io.github.njw3995.fabricmmo.api.content.EntityXpContentRegistryView;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DefaultEntityXpContentRegistry
        implements EntityXpContentRegistrar, EntityXpContentRegistryView {
    private final Map<NamespacedId, EntityXpContentDefinition> codeDefinitions = new TreeMap<>();
    private final Map<NamespacedId, EntityXpContentDefinition> datapackDefinitions = new TreeMap<>();
    private final Set<NamespacedId> datapackDisabled = new TreeSet<>();
    private boolean frozen;

    @Override
    public synchronized void registerEntityXpContent(EntityXpContentDefinition definition) {
        requireOpen();
        if (codeDefinitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate entity XP content id: " + definition.id());
        }
    }

    public synchronized void replaceDatapackDefinitions(
            Collection<EntityXpContentDefinition> definitions,
            Collection<NamespacedId> disabledIds) {
        TreeMap<NamespacedId, EntityXpContentDefinition> replacement = new TreeMap<>();
        for (EntityXpContentDefinition definition : definitions) {
            if (replacement.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalStateException(
                        "Duplicate datapack entity XP content id: " + definition.id());
            }
        }
        datapackDefinitions.clear();
        datapackDefinitions.putAll(replacement);
        datapackDisabled.clear();
        datapackDisabled.addAll(disabledIds);
    }

    @Override
    public synchronized Optional<EntityXpContentDefinition> find(NamespacedId id) {
        return Optional.ofNullable(merged().get(id));
    }

    @Override
    public synchronized List<EntityXpContentDefinition> definitions() {
        return List.copyOf(merged().values());
    }

    @Override
    public synchronized List<EntityXpContentDefinition> definitionsForScope(
            EntityXpContentDefinition.Scope scope) {
        return merged().values().stream().filter(definition -> definition.scope() == scope).toList();
    }

    public synchronized void freeze() {
        frozen = true;
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private Map<NamespacedId, EntityXpContentDefinition> merged() {
        TreeMap<NamespacedId, EntityXpContentDefinition> merged = new TreeMap<>(codeDefinitions);
        datapackDisabled.forEach(merged::remove);
        merged.putAll(datapackDefinitions);
        return merged;
    }

    private void requireOpen() {
        if (frozen) throw new IllegalStateException("Entity XP content registry is frozen");
    }
}
