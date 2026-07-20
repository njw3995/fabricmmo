package io.github.njw3995.fabricmmo.core.skill.taming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

class TamingSummonStateTest {
    @Test
    void summonRecoveryMarkerSurvivesNbtRoundTrip() {
        TamingSummonState original = new TamingSummonState(
                UUID.randomUUID(), TamingSummonType.WOLF, 123456789L);
        NbtCompound nbt = new NbtCompound();
        original.write(nbt);
        assertEquals(original, TamingSummonState.read(nbt).orElseThrow());
    }

    @Test
    void corruptSummonTypeIsRejectedWithoutInventingState() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("FabricMMOCotwOwner", UUID.randomUUID());
        nbt.putString("FabricMMOCotwType", "NOT_A_SUMMON");
        nbt.putLong("FabricMMOCotwExpiresAt", 1L);
        assertTrue(TamingSummonState.read(nbt).isEmpty());
    }
}
