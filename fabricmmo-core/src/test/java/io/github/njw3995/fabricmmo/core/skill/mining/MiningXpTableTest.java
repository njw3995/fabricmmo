package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void readsAdminMiningOverridesFromUpstreamCompatibleYaml(@TempDir Path directory)
            throws Exception {
        Path experience = directory.resolve("experience.yml");
        Files.writeString(experience, """
                Experience_Formula:
                  Curve: LINEAR
                Experience_Values:
                  Mining:
                    Stone: 25
                    End_Bricks: 75
                    minecraft:diamond_ore: 3456
                  Repair:
                    Base: 1000
                """);

        MiningXpTable table = MiningXpTable.loadConfigured(experience);

        assertEquals(3, table.entries().size());
        assertEquals(25, table.xpFor(NamespacedId.parse("minecraft:stone")));
        assertEquals(75, table.xpFor(NamespacedId.parse("minecraft:end_stone_bricks")));
        assertEquals(3456, table.xpFor(NamespacedId.parse("minecraft:diamond_ore")));
    }

    @Test
    void rejectsMissingOrInvalidMiningConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> MiningXpTable.loadExperienceYaml(
                new StringReader("Experience_Values:\n  Repair:\n    Base: 1000\n")));
        assertThrows(IllegalArgumentException.class, () -> MiningXpTable.loadExperienceYaml(
                new StringReader("Experience_Values:\n  Mining:\n    Stone: nope\n")));
        assertThrows(IllegalArgumentException.class, () -> MiningXpTable.loadExperienceYaml(
                new StringReader("Experience_Values:\n  Mining:\n    Stone: -1\n")));
    }
}
