package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import org.junit.jupiter.api.Test;

class WoodcuttingXpDefaultsTest {
    @Test
    void defaultsExcludePost1211PaleOakAndKeepPinnedValues() {
        var values = WoodcuttingXpDefaults.values();
        assertEquals(70, values.get(new NamespacedId("minecraft", "oak_log")).intValue());
        assertEquals(110, values.get(new NamespacedId("minecraft", "stripped_mangrove_log")).intValue());
        assertEquals(1, values.get(new NamespacedId("minecraft", "nether_wart_block")).intValue());
        assertFalse(values.containsKey(new NamespacedId("minecraft", "pale_oak_log")));
    }
}
