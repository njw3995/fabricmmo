package io.github.njw3995.fabricmmo.core.skill.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FishingXpTableTest {
    @Test
    void loadsOnlyCatchItemsAndKeepsShakeAsGlobalXp() throws Exception {
        FishingXpTable table = FishingXpTable.load(
                Path.of("src/main/resources/defaults/experience.yml"));

        assertEquals(4, table.values().size());
        assertEquals(600, table.values().get(
                NamespacedId.parse("minecraft:salmon")).intValue());
    }
}
