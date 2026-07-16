package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import org.junit.jupiter.api.Test;

class MiningXpTableTest {
    @Test
    void loadsAllApplicablePinnedUpstreamDefaultsForMinecraft1211() {
        MiningXpTable table = MiningXpTable.upstreamDefaultsForMinecraft1211();
        assertEquals(100, table.entries().size());
        assertEquals(15, table.xpFor(NamespacedId.parse("minecraft:stone")));
        assertEquals(900, table.xpFor(NamespacedId.parse("minecraft:iron_ore")));
        assertEquals(3600, table.xpFor(NamespacedId.parse("minecraft:deepslate_diamond_ore")));
        assertEquals(7777, table.xpFor(NamespacedId.parse("minecraft:ancient_debris")));
        assertEquals(50, table.xpFor(NamespacedId.parse("minecraft:end_stone_bricks")));
        assertFalse(table.entries().containsKey(NamespacedId.parse("minecraft:cinnabar")));
        assertFalse(table.entries().containsKey(NamespacedId.parse("minecraft:sulfur")));
    }
}
