package io.github.njw3995.fabricmmo.core.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressionSettingsTest {
    @TempDir
    Path tempDirectory;

    @Test
    void zeroCapsAreUncappedAndMissingTruncateUsesUpstreamDefault() throws Exception {
        Path config = tempDirectory.resolve("config.yml");
        Path experience = tempDirectory.resolve("experience.yml");
        Files.writeString(config, """
                General:
                  RetroMode:
                    Enabled: true
                  Power_Level_Cap: 0
                Skills:
                  Mining:
                    Level_Cap: 0
                """);
        Files.writeString(experience, minimumExperienceConfig());

        ProgressionSettings settings = ProgressionSettings.load(config, experience);

        assertEquals(Integer.MAX_VALUE, settings.levelCap(CoreSkills.MINING));
        assertEquals(Integer.MAX_VALUE, settings.powerLevelCap());
        assertTrue(settings.truncateSkills());
    }

    @Test
    void positiveCapsAreLoadedLiterally() throws Exception {
        Path config = tempDirectory.resolve("config.yml");
        Path experience = tempDirectory.resolve("experience.yml");
        Files.writeString(config, """
                General:
                  RetroMode:
                    Enabled: true
                  Power_Level_Cap: 250
                  TruncateSkills: false
                Skills:
                  Mining:
                    Level_Cap: 75
                """);
        Files.writeString(experience, minimumExperienceConfig());

        ProgressionSettings settings = ProgressionSettings.load(config, experience);

        assertEquals(75, settings.levelCap(CoreSkills.MINING));
        assertEquals(250, settings.powerLevelCap());
        assertTrue(!settings.truncateSkills());
    }

    private static String minimumExperienceConfig() {
        return """
                Experience_Formula:
                  Curve: LINEAR
                  Linear_Values:
                    base: 1020
                    multiplier: 20
                  Exponential_Values:
                    base: 2000
                    multiplier: 0.1
                    exponent: 1.8
                  Multiplier:
                    Global: 1.0
                  Skill_Multiplier:
                    Mining: 1.0
                  Custom_XP_Perk:
                    Boost: 1.25
                """;
    }
}
