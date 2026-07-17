package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MiningDropSettingsTest {
    @Test
    void loadsPackagedUpstreamMiningDropDefaults() throws Exception {
        Path defaults = Path.of("src", "main", "resources", "defaults");

        MiningDropSettings settings = MiningDropSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));

        assertEquals(67, settings.enabledMaterials().size());
        assertTrue(settings.materialEnabled(NamespacedId.parse("minecraft:diamond_ore")));
        assertTrue(settings.materialEnabled(NamespacedId.parse("minecraft:diamond")));
        assertTrue(settings.materialEnabled(NamespacedId.parse("minecraft:amethyst_block")));
        assertTrue(settings.materialEnabled(NamespacedId.parse("minecraft:redstone")));
        assertTrue(settings.silkTouchEnabled());
        assertTrue(settings.allowSuperBreakerTripleDrops());
        assertEquals(100.0D, settings.doubleDropsChanceMaxPercent());
        assertEquals(50.0D, settings.motherLodeChanceMaxPercent());
        assertTrue(settings.doubleDropsUnlocked(1, ProgressionMode.RETRO));
        assertFalse(settings.motherLodeUnlocked(999, ProgressionMode.RETRO));
        assertTrue(settings.motherLodeUnlocked(1000, ProgressionMode.RETRO));
    }

    @Test
    void honorsAdminOverridesAndDisabledMaterials(@TempDir Path directory) throws Exception {
        Path config = directory.resolve("config.yml");
        Path advanced = directory.resolve("advanced.yml");
        Path ranks = directory.resolve("skillranks.yml");
        Files.writeString(config, """
                Bonus_Drops:
                  Mining:
                    Stone: false
                    minecraft:diamond_ore: true
                """);
        Files.writeString(advanced, """
                Skills:
                  Mining:
                    MotherLode:
                      MaxBonusLevel:
                        Standard: 10
                        RetroMode: 20
                      ChanceMax: 25.0
                    SuperBreaker:
                      AllowTripleDrops: false
                    DoubleDrops:
                      SilkTouch: false
                      ChanceMax: 75.0
                      MaxBonusLevel:
                        Standard: 5
                        RetroMode: 50
                """);
        Files.writeString(ranks, """
                Mining:
                  MotherLode:
                    Standard:
                      Rank_1: 7
                    RetroMode:
                      Rank_1: 70
                  DoubleDrops:
                    Standard:
                      Rank_1: 2
                    RetroMode:
                      Rank_1: 20
                """);

        MiningDropSettings settings = MiningDropSettings.load(config, advanced, ranks);

        assertEquals(1, settings.enabledMaterials().size());
        assertFalse(settings.materialEnabled(NamespacedId.parse("minecraft:stone")));
        assertTrue(settings.materialEnabled(NamespacedId.parse("minecraft:diamond_ore")));
        assertFalse(settings.silkTouchEnabled());
        assertFalse(settings.allowSuperBreakerTripleDrops());
        assertEquals(75.0D, settings.doubleDropsChanceMaxPercent());
        assertEquals(25.0D, settings.motherLodeChanceMaxPercent());
        assertFalse(settings.doubleDropsUnlocked(19, ProgressionMode.RETRO));
        assertTrue(settings.doubleDropsUnlocked(20, ProgressionMode.RETRO));
    }

    @Test
    void rejectsMissingRequiredConfiguration(@TempDir Path directory) throws Exception {
        Path config = directory.resolve("config.yml");
        Path advanced = directory.resolve("advanced.yml");
        Path ranks = directory.resolve("skillranks.yml");
        Files.writeString(config, "Bonus_Drops:\n  Mining:\n    Stone: true\n");
        Files.writeString(advanced, "Skills:\n  Mining:\n    DoubleDrops:\n      SilkTouch: true\n");
        Files.writeString(ranks, "Mining:\n  DoubleDrops:\n    Standard:\n      Rank_1: 1\n");

        assertThrows(IllegalArgumentException.class,
                () -> MiningDropSettings.load(config, advanced, ranks));
    }
}
