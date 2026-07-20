package io.github.njw3995.fabricmmo.core.skill.smelting;

import java.util.Optional;
import java.util.UUID;

/** Fabric-native persistent owner metadata for furnaces and brewing stands. */
public interface OwnedProcessingBlock {
    Optional<UUID> fabricmmo$getOwner();

    void fabricmmo$setOwner(UUID owner);
}
