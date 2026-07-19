package io.github.njw3995.fabricmmo.core.skill.herbalism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import org.junit.jupiter.api.Test;

class HerbalismXpDefaultsTest {
    @Test
    void minecraft1211DefaultsMatchApplicableUpstreamValues() {
        var values = HerbalismXpDefaults.values();
        assertEquals(50, values.get(NamespacedId.parse("minecraft:wheat")));
        assertEquals(50, values.get(NamespacedId.parse("minecraft:sweet_berry_bush")));
        assertEquals(30, values.get(NamespacedId.parse("minecraft:cactus")));
        assertEquals(500, values.get(NamespacedId.parse("minecraft:wither_rose")));
        assertFalse(values.containsKey(NamespacedId.parse("minecraft:cactus_flower")));
        assertFalse(values.containsKey(NamespacedId.parse("minecraft:firefly_bush")));
    }
}
