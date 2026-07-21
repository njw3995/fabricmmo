package io.github.njw3995.fabricmmo.core.skill.smelting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

class ProcessingBlockOwnershipNbtTest {
    @Test
    void ownerRoundTripsAcrossRestartNbt() {
        UUID owner = UUID.randomUUID();
        NbtCompound nbt = new NbtCompound();
        ProcessingBlockOwnershipNbt.write(nbt, owner);
        assertEquals(owner, ProcessingBlockOwnershipNbt.read(nbt).orElseThrow());
    }

    @Test
    void nullOwnerRemovesPersistedValue() {
        NbtCompound nbt = new NbtCompound();
        ProcessingBlockOwnershipNbt.write(nbt, UUID.randomUUID());
        ProcessingBlockOwnershipNbt.write(nbt, null);
        assertTrue(ProcessingBlockOwnershipNbt.read(nbt).isEmpty());
    }
}
