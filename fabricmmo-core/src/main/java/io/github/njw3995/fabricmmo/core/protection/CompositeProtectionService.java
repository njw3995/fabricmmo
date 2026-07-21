package io.github.njw3995.fabricmmo.core.protection;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.protection.ProtectionProviderRegistrar;
import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

/** Deterministic fail-closed composition of independently supplied protection providers. */
public final class CompositeProtectionService
        implements ProtectionService, ProtectionProviderRegistrar {
    private static final System.Logger LOGGER = System.getLogger("FabricMMO/Protection");
    private static final NamespacedId BASE = NamespacedId.parse("fabricmmo:base_protection");
    private static final Comparator<Entry> ORDER = Comparator
            .comparingInt(Entry::priority).reversed()
            .thenComparing(Entry::id);

    private final Map<NamespacedId, Entry> entries = new TreeMap<>();
    private boolean frozen;

    public CompositeProtectionService(ProtectionService base) {
        entries.put(BASE, new Entry(BASE, Integer.MIN_VALUE,
                Objects.requireNonNull(base, "base")));
    }

    @Override
    public synchronized void registerProtectionProvider(
            NamespacedId providerId,
            int priority,
            ProtectionService provider) {
        if (frozen) throw new IllegalStateException("Protection provider registry is frozen");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(provider, "provider");
        if (entries.putIfAbsent(providerId, new Entry(providerId, priority, provider)) != null) {
            throw new IllegalStateException("Duplicate protection provider id: " + providerId);
        }
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized boolean frozen() {
        return frozen;
    }

    public synchronized List<NamespacedId> providerIds() {
        return ordered().stream().map(Entry::id).toList();
    }

    @Override
    public boolean canBreak(UUID playerId, String worldId, int x, int y, int z) {
        return all(entry -> entry.provider().canBreak(playerId, worldId, x, y, z), "break");
    }

    @Override
    public boolean canModify(UUID playerId, String worldId, int x, int y, int z) {
        return all(entry -> entry.provider().canModify(playerId, worldId, x, y, z), "modify");
    }

    @Override
    public boolean canInteract(UUID playerId, String worldId, int x, int y, int z) {
        return all(entry -> entry.provider().canInteract(playerId, worldId, x, y, z), "interact");
    }

    @Override
    public boolean canDamage(UUID attackerId, UUID targetId, String worldId) {
        return all(entry -> entry.provider().canDamage(attackerId, targetId, worldId), "damage");
    }

    private boolean all(Predicate<Entry> decision, String action) {
        for (Entry entry : ordered()) {
            try {
                if (!decision.test(entry)) return false;
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "Protection provider " + entry.id() + " failed during " + action
                                + "; denying the action",
                        exception);
                return false;
            }
        }
        return true;
    }

    private synchronized List<Entry> ordered() {
        ArrayList<Entry> result = new ArrayList<>(entries.values());
        result.sort(ORDER);
        return List.copyOf(result);
    }

    private record Entry(NamespacedId id, int priority, ProtectionService provider) {
    }
}
