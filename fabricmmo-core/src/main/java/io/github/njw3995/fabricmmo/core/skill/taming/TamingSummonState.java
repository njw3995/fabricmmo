package io.github.njw3995.fabricmmo.core.skill.taming;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;

/** Persistent recovery marker for a transient Call of the Wild summon. */
public record TamingSummonState(UUID owner, TamingSummonType type, long expiresAt) {
    private static final String OWNER = "FabricMMOCotwOwner";
    private static final String TYPE = "FabricMMOCotwType";
    private static final String EXPIRES_AT = "FabricMMOCotwExpiresAt";

    public void write(NbtCompound nbt) {
        nbt.putUuid(OWNER, owner);
        nbt.putString(TYPE, type.name());
        nbt.putLong(EXPIRES_AT, expiresAt);
    }

    public static Optional<TamingSummonState> read(NbtCompound nbt) {
        if (!nbt.containsUuid(OWNER)) return Optional.empty();
        try {
            return Optional.of(new TamingSummonState(
                    nbt.getUuid(OWNER),
                    TamingSummonType.valueOf(nbt.getString(TYPE)),
                    nbt.getLong(EXPIRES_AT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
