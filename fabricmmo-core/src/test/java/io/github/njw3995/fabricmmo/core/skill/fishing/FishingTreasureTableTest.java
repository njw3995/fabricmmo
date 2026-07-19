package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FishingTreasureTableTest {
    @Test
    void loadsTheUpstreamPlayerInventoryShakeSentinel() throws Exception {
        FishingTreasureTable.ShakeInventoryDefinition definition =
                FishingTreasureTable.loadPlayerInventoryShakeDefinition(
                        Path.of("src/main/resources/defaults/fishing_treasures.yml"));

        assertEquals(0.0D, definition.chancePercent());
        assertEquals(0, definition.dropLevel());
        assertFalse(definition.wholeStacks());
    }
}
