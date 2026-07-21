package io.github.njw3995.fabricmmo.core.config;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.config.ConfigContribution;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistrar;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistryView;
import io.github.njw3995.fabricmmo.api.config.ConfigService;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

public final class DefaultConfigRegistry
        implements ConfigRegistrar, ConfigRegistryView, ConfigService {
    private final Map<Key, ConfigContribution> contributions = new TreeMap<>();
    private final Map<Key, Map<String, String>> loadedValues = new TreeMap<>();
    private Path root;
    private boolean frozen;

    @Override
    public synchronized void registerConfig(ConfigContribution contribution) {
        requireOpen();
        Key key = new Key(contribution.owner(), contribution.fileName());
        if (contributions.putIfAbsent(key, contribution) != null) {
            throw new IllegalStateException("Duplicate config contribution: " + key);
        }
    }

    @Override
    public synchronized List<ConfigContribution> contributions() {
        return List.copyOf(contributions.values());
    }

    @Override
    public synchronized List<ConfigContribution> contributionsByOwner(NamespacedId owner) {
        return contributions.values().stream()
                .filter(contribution -> contribution.owner().equals(owner))
                .toList();
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized void bind(Path root) throws IOException {
        if (!frozen) {
            throw new IllegalStateException("Addon config registry must be frozen before binding");
        }
        this.root = root.toAbsolutePath().normalize();
        reload();
    }

    @Override
    public synchronized boolean available() {
        return root != null;
    }

    @Override
    public synchronized Optional<String> value(
            NamespacedId owner, String fileName, String key) {
        return Optional.ofNullable(values(owner, fileName).get(key));
    }

    @Override
    public synchronized Map<String, String> values(NamespacedId owner, String fileName) {
        return loadedValues.getOrDefault(new Key(owner, fileName), Map.of());
    }

    @Override
    public synchronized void reload() throws IOException {
        if (root == null) {
            throw new IllegalStateException("Addon config service is not bound to a config directory");
        }
        Files.createDirectories(root);
        TreeMap<Key, Map<String, String>> refreshed = new TreeMap<>();
        for (Map.Entry<Key, ConfigContribution> entry : contributions.entrySet()) {
            Path file = pathFor(entry.getKey());
            materialize(file, entry.getValue());
            refreshed.put(entry.getKey(), load(file));
        }
        loadedValues.clear();
        loadedValues.putAll(refreshed);
    }

    @Override
    public synchronized boolean frozen() {
        return frozen;
    }

    private Path pathFor(Key key) {
        Path ownerRoot = root.resolve(key.owner().namespace()).resolve(key.owner().path()).normalize();
        Path file = ownerRoot.resolve(key.fileName()).normalize();
        if (!file.startsWith(ownerRoot)) {
            throw new IllegalArgumentException("Unsafe addon config path: " + key.fileName());
        }
        return file;
    }

    private static void materialize(Path file, ConfigContribution contribution) throws IOException {
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            List<String> lines = new ArrayList<>();
            lines.add("# FabricMMO addon configuration for " + contribution.owner());
            contribution.defaults().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> lines.add(entry.getKey() + '=' + entry.getValue()));
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return;
        }

        Map<String, String> existing = load(file);
        List<Map.Entry<String, String>> missing = contribution.defaults().entrySet().stream()
                .filter(entry -> !existing.containsKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .toList();
        if (missing.isEmpty()) return;

        List<String> appended = new ArrayList<>();
        appended.add("");
        appended.add("# Added by a newer FabricMMO addon API registration");
        for (Map.Entry<String, String> entry : missing) {
            appended.add(entry.getKey() + '=' + entry.getValue());
        }
        Files.write(file, appended, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }

    private static Map<String, String> load(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        TreeMap<String, String> values = new TreeMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return Map.copyOf(values);
    }

    private void requireOpen() {
        if (frozen) {
            throw new IllegalStateException("Config registry is frozen");
        }
    }

    private record Key(NamespacedId owner, String fileName) implements Comparable<Key> {
        @Override
        public int compareTo(Key other) {
            int ownerComparison = owner.compareTo(other.owner);
            return ownerComparison != 0 ? ownerComparison : fileName.compareTo(other.fileName);
        }
    }
}
