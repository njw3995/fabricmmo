package io.github.njw3995.fabricmmo.core.skill.smelting;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;

/** Stable NBT encoding shared by furnace and brewing-stand ownership mixins. */
public final class ProcessingBlockOwnershipNbt {
    public static final String OWNER_KEY = "fabricmmo_owner";

    private ProcessingBlockOwnershipNbt() {
    }

    public static Optional<UUID> read(NbtCompound nbt) {
        return nbt.containsUuid(OWNER_KEY)
                ? Optional.of(nbt.getUuid(OWNER_KEY)) : Optional.empty();
    }

    public static void write(NbtCompound nbt, UUID owner) {
        if (owner == null) {
            nbt.remove(OWNER_KEY);
        } else {
            nbt.putUuid(OWNER_KEY, owner);
        }
    }
}
