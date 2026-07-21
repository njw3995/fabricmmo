package io.github.njw3995.fabricmmo.core.skill.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

class AlchemyBrewStateTest {
    @Test
    void ownerAndInFlightBrewSurviveNbtRoundTrip() {
        UUID owner = UUID.randomUUID();
        AlchemyBrewState original = new AlchemyBrewState(
                owner, 173.5D, 8.0D / 3.0D, "minecraft:fern");
        NbtCompound nbt = new NbtCompound();
        original.write(nbt);
        AlchemyBrewState restored = AlchemyBrewState.read(nbt);
        assertEquals(original, restored);
        assertTrue(restored.active());
    }

    @Test
    void inactiveStatePersistsOwnerWithoutInventingARecipe() {
        UUID owner = UUID.randomUUID();
        NbtCompound nbt = new NbtCompound();
        new AlchemyBrewState(owner, 0.0D, 1.0D, "").write(nbt);
        AlchemyBrewState restored = AlchemyBrewState.read(nbt);
        assertEquals(owner, restored.owner());
        assertFalse(restored.active());
    }
}
