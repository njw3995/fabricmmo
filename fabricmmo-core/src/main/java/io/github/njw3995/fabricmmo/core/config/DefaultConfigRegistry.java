package io.github.njw3995.fabricmmo.core.config;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.config.ConfigContribution;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistrar;
import io.github.njw3995.fabricmmo.api.config.ConfigRegistryView;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DefaultConfigRegistry implements ConfigRegistrar, ConfigRegistryView {
    private final Map<Key, ConfigContribution> contributions = new TreeMap<>();
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

    @Override
    public synchronized boolean frozen() {
        return frozen;
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
