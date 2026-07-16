package io.github.njw3995.fabricmmo.core.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.command.CommandMetadata;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistrar;
import io.github.njw3995.fabricmmo.api.command.CommandMetadataRegistryView;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class DefaultCommandMetadataRegistry
        implements CommandMetadataRegistrar, CommandMetadataRegistryView {
    private final Map<NamespacedId, CommandMetadata> commands = new TreeMap<>();
    private final Set<String> literals = new HashSet<>();
    private boolean frozen;

    @Override
    public synchronized void registerCommandMetadata(CommandMetadata metadata) {
        requireOpen();
        if (commands.containsKey(metadata.id())) {
            throw new IllegalStateException("Duplicate command metadata id: " + metadata.id());
        }
        Set<String> requested = new HashSet<>();
        requested.add(normalize(metadata.literal()));
        metadata.aliases().stream().map(DefaultCommandMetadataRegistry::normalize).forEach(requested::add);
        Set<String> collisions = new HashSet<>(requested);
        collisions.retainAll(literals);
        if (!collisions.isEmpty()) {
            throw new IllegalStateException("Command literal or alias collision: " + collisions);
        }
        commands.put(metadata.id(), metadata);
        literals.addAll(requested);
    }

    @Override
    public synchronized Optional<CommandMetadata> find(NamespacedId id) {
        return Optional.ofNullable(commands.get(id));
    }

    @Override
    public synchronized List<CommandMetadata> commands() {
        return List.copyOf(commands.values());
    }

    public synchronized void freeze() {
        frozen = true;
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Command metadata registry is frozen");
        }
    }
}
