package io.github.njw3995.fabricmmo.core.protection;

import io.github.njw3995.fabricmmo.api.protection.ProtectionService;
import java.util.UUID;

public final class AllowAllProtectionService implements ProtectionService {
    @Override
    public boolean canBreak(UUID playerId, String worldId, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean canModify(UUID playerId, String worldId, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean canInteract(UUID playerId, String worldId, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean canDamage(UUID attackerId, UUID targetId, String worldId) {
        return true;
    }
}
