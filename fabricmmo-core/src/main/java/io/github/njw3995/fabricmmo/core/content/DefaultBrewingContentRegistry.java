package io.github.njw3995.fabricmmo.core.content;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.content.BrewingContentDefinition;
import io.github.njw3995.fabricmmo.api.content.BrewingContentRegistrar;
import io.github.njw3995.fabricmmo.api.content.BrewingContentRegistryView;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DefaultBrewingContentRegistry
        implements BrewingContentRegistrar, BrewingContentRegistryView {
    private final Map<NamespacedId, BrewingContentDefinition> codeDefinitions = new TreeMap<>();
    private final Map<NamespacedId, BrewingContentDefinition> datapackDefinitions = new TreeMap<>();
    private final Set<NamespacedId> datapackDisabled = new TreeSet<>();
    private boolean frozen;

    @Override
    public synchronized void registerBrewingContent(BrewingContentDefinition definition) {
        requireOpen();
        if (codeDefinitions.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate brewing content id: " + definition.id());
        }
    }

    /** Replaces only the reloadable datapack layer; Java addon registrations stay frozen. */
    public synchronized void replaceDatapackDefinitions(
            Collection<BrewingContentDefinition> definitions,
            Collection<NamespacedId> disabledIds) {
        TreeMap<NamespacedId, BrewingContentDefinition> replacement = new TreeMap<>();
        for (BrewingContentDefinition definition : definitions) {
            if (replacement.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalStateException("Duplicate datapack brewing content id: " + definition.id());
            }
        }
        datapackDefinitions.clear();
        datapackDefinitions.putAll(replacement);
        datapackDisabled.clear();
        datapackDisabled.addAll(disabledIds);
    }

    @Override
    public synchronized Optional<BrewingContentDefinition> find(NamespacedId id) {
        return Optional.ofNullable(merged().get(id));
    }

    @Override
    public synchronized List<BrewingContentDefinition> definitions() {
        return List.copyOf(merged().values());
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

    private Map<NamespacedId, BrewingContentDefinition> merged() {
        TreeMap<NamespacedId, BrewingContentDefinition> merged = new TreeMap<>(codeDefinitions);
        datapackDisabled.forEach(merged::remove);
        merged.putAll(datapackDefinitions);
        return merged;
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Brewing content registry is frozen");
        }
    }
}
