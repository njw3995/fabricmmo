package io.github.njw3995.fabricmmo.core.skill.excavation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExcavationTreasureRollerTest {
    private static final NamespacedId SAND = NamespacedId.parse("minecraft:sand");
    private static final NamespacedId CAKE = NamespacedId.parse("minecraft:cake");

    @Test
    void gigaDrillPerformsThreeIndependentTreasureRolls() throws Exception {
        ExcavationTreasure treasure = new ExcavationTreasure(
                "CAKE", CAKE, 1, 3, 100.0D, 1, 1, Set.of(SAND));
        ExcavationTreasureTable table = ExcavationTreasureTable.of(Map.of(SAND, List.of(treasure)));
        ExcavationTreasureRoller.Outcome outcome = new ExcavationTreasureRoller(() -> 0.0D)
                .roll(SAND, 1000, true, false, true, settings(), table);

        assertEquals(3, outcome.treasureDrops().size());
        assertEquals(List.of("CAKE", "CAKE", "CAKE"), outcome.treasureDrops().stream()
                .map(ExcavationTreasure::key)
                .toList());
        assertEquals(List.of(3, 3, 3), outcome.treasureXpAwards());
        assertEquals(List.of(8, 8, 8), outcome.vanillaOrbAwards());
        assertEquals(9, outcome.totalTreasureXp());
        assertEquals(24, outcome.totalVanillaOrbXp());
    }

    @Test
    void lowLevelAndMissingPermissionProduceNoTreasure() throws Exception {
        ExcavationTreasure treasure = new ExcavationTreasure(
                "CAKE", CAKE, 1, 3, 100.0D, 75, 750, Set.of(SAND));
        ExcavationTreasureTable table = ExcavationTreasureTable.of(Map.of(SAND, List.of(treasure)));
        ExcavationTreasureRoller roller = new ExcavationTreasureRoller(() -> 0.0D);

        assertTrue(roller.roll(SAND, 749, false, false, true, settings(), table)
                .treasureDrops().isEmpty());
        assertTrue(roller.roll(SAND, 1000, false, false, false, settings(), table)
                .treasureDrops().isEmpty());
    }

    private static ExcavationSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return ExcavationSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));
    }
}
