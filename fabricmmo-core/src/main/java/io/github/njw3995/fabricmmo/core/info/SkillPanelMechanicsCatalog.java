package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Registered exact mechanic-stat providers for implemented skills. */
public final class SkillPanelMechanicsCatalog {
    private final Map<NamespacedId, SkillPanelMechanicsProvider> providers =
            new ConcurrentHashMap<>();

    public void register(NamespacedId id, SkillPanelMechanicsProvider provider) {
        providers.put(id, provider);
    }

    public SkillPanelMechanicsProvider provider(NamespacedId id) {
        return providers.getOrDefault(id, (playerId, level) -> List.of());
    }
}
