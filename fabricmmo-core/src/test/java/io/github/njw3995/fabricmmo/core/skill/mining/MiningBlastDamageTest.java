package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MiningBlastDamageTest {
    @Test
    void appliesDemolitionsExpertiseAndBystanderCap() {
        assertEquals(15.0F, MiningBlastDamage.ownerDamage(20.0F, 25.0D));
        assertEquals(0.0F, MiningBlastDamage.ownerDamage(20.0F, 100.0D));
        assertEquals(24.0F, MiningBlastDamage.bystanderDamage(40.0F));
        assertEquals(10.0F, MiningBlastDamage.bystanderDamage(10.0F));
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> MiningBlastDamage.ownerDamage(-1.0F, 25.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MiningBlastDamage.ownerDamage(1.0F, 101.0D));
    }
}
