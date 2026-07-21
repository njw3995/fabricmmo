package io.github.njw3995.fabricmmo.core.skill.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlchemySettingsTest {
    @TempDir Path temp;

    @Test
    void packagedDefaultsMatchPinnedMcMmoValues() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        AlchemySettings settings = AlchemySettings.load(
                defaults.resolve("config.yml"), defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"), defaults.resolve("experience.yml"),
                ProgressionMode.RETRO);
        assertTrue(settings.enabledForHoppers());
        assertFalse(settings.preventHopperIngredients());
        assertFalse(settings.preventHopperBottles());
        assertEquals(1.0D, settings.catalysisMinSpeed());
        assertEquals(4.0D, settings.catalysisMaxSpeed());
        assertEquals(1000, settings.catalysisMaxBonusLevel());
        assertEquals(8, settings.concoctionsTier(1000));
        assertEquals(666.0D, settings.xpForStage(1));
        assertEquals(2250.0D, settings.xpForStage(4));
        assertEquals(0.0D, settings.xpForStage(5));
    }

    @Test
    void malformedAlchemyScalarIsReportedInsteadOfReset() throws Exception {
        Path config = temp.resolve("config.yml");
        Path advanced = temp.resolve("advanced.yml");
        Path ranks = temp.resolve("skillranks.yml");
        Path experience = temp.resolve("experience.yml");
        Files.writeString(config, "Skills:\n  Alchemy:\n    Enabled_for_Hoppers: maybe\n");
        Files.writeString(advanced, "Skills:\n  Alchemy:\n    Catalysis:\n      MinSpeed: 1\n");
        Files.writeString(ranks, "Alchemy:\n  Catalysis:\n    Standard:\n      Rank_1: 0\n");
        Files.writeString(experience, "Experience_Values:\n  Alchemy:\n    Potion_Brewing:\n      Stage_1: 666\n");
        assertThrows(IllegalArgumentException.class, () -> AlchemySettings.load(
                config, advanced, ranks, experience, ProgressionMode.STANDARD));
    }
}
