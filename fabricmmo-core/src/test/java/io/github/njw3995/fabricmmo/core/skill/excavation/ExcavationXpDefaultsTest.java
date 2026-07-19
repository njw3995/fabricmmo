package io.github.njw3995.fabricmmo.core.skill.excavation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import org.junit.jupiter.api.Test;

class ExcavationXpDefaultsTest {
    @Test
    void matchesApplicableMinecraft1211UpstreamDefaults() {
        var values = ExcavationXpDefaults.values();
        assertEquals(16, values.size());
        assertEquals(40, values.get(NamespacedId.parse("minecraft:clay")).intValue());
        assertEquals(60, values.get(NamespacedId.parse("minecraft:rooted_dirt")).intValue());
        assertEquals(20, values.get(NamespacedId.parse("minecraft:snow")).intValue());
        assertEquals(80, values.get(NamespacedId.parse("minecraft:mud")).intValue());
        assertEquals(90, values.get(NamespacedId.parse("minecraft:muddy_mangrove_roots")).intValue());
    }
}
