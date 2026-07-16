package io.github.njw3995.fabricmmo.api.protection;

import java.util.UUID;

public interface ProtectionService {
    boolean canBreak(UUID playerId, String worldId, int x, int y, int z);

    boolean canModify(UUID playerId, String worldId, int x, int y, int z);

    boolean canInteract(UUID playerId, String worldId, int x, int y, int z);

    boolean canDamage(UUID attackerId, UUID targetId, String worldId);
}
