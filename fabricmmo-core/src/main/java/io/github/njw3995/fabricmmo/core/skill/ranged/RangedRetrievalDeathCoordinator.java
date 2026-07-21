package io.github.njw3995.fabricmmo.core.skill.ranged;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Orders corpse drops after the lethal ranged hit has finished recording retrieval state. */
final class RangedRetrievalDeathCoordinator {
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    boolean onDeath(UUID targetId, boolean rangedHitInProgress) {
        if (rangedHitInProgress) {
            pending.add(targetId);
            return false;
        }
        return true;
    }

    boolean afterDamage(UUID targetId) {
        return pending.remove(targetId);
    }

    void clear() {
        pending.clear();
    }
}
