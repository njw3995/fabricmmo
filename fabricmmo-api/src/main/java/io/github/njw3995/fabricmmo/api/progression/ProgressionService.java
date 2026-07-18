package io.github.njw3995.fabricmmo.api.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.UUID;

public interface ProgressionService {
    ProgressionSnapshot query(UUID playerId, NamespacedId skillId);

    XpAwardResult award(XpAwardRequest request);

    ProgressionSnapshot setLevel(UUID playerId, NamespacedId skillId, int level);

    ProgressionSnapshot addLevels(UUID playerId, NamespacedId skillId, int levels);

    Map<NamespacedId, ProgressionSnapshot> queryAll(UUID playerId);
}
