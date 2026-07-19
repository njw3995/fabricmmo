package io.github.njw3995.fabricmmo.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultConfigInstallerTest {
    @Test
    void installsEveryRequiredDefaultWithoutOverwritingAdminFiles(@TempDir Path directory)
            throws Exception {
        Path existing = directory.resolve("config.yml");
        Files.writeString(existing, "admin-value: keep\n");

        DefaultConfigInstaller.installMissingDefaults(directory);

        String updated = Files.readString(existing);
        assertEquals("admin-value: keep", Files.readAllLines(existing).get(0));
        assertTrue(updated.contains("Scoreboard:"));
        assertTrue(updated.contains("  UseScoreboards: false"));
        for (String fileName : DefaultConfigInstaller.allFiles()) {
            assertTrue(Files.isRegularFile(directory.resolve(fileName)), fileName);
        }
        assertTrue(Files.isDirectory(directory.resolve("locales")));
    }

    @Test
    void recursivelyAddsNewSkillDefaultsAndCreatesBackup(@TempDir Path directory)
            throws Exception {
        Path experience = directory.resolve("experience.yml");
        Files.writeString(experience, """
                Experience_Values:
                  Mining:
                    Coal_Ore: 9999
                """);

        DefaultConfigInstaller.installMissingDefaults(directory);

        String updated = Files.readString(experience);
        assertTrue(updated.contains("Coal_Ore: 9999"));
        assertTrue(updated.contains("  Excavation:"));
        assertTrue(updated.contains("    Sand: 40"));
        assertTrue(Files.isRegularFile(directory.resolve("experience.yml.pre-update.bak")));
        assertEquals("""
                Experience_Values:
                  Mining:
                    Coal_Ore: 9999
                """, Files.readString(directory.resolve("experience.yml.pre-update.bak")));
    }

    @Test
    void replacesLegacyEmptyFishingTreasurePlaceholderWithBackup(@TempDir Path directory)
            throws Exception {
        Path fishingTreasures = directory.resolve("fishing_treasures.yml");
        Files.writeString(fishingTreasures, "{}\n");

        DefaultConfigInstaller.installMissingDefaults(directory);

        String installed = Files.readString(fishingTreasures);
        assertTrue(installed.contains("Fishing:"));
        assertTrue(installed.contains("Item_Drop_Rates:"));
        assertTrue(installed.contains("Shake:"));
        assertEquals("{}\n", Files.readString(
                directory.resolve("fishing_treasures.yml.pre-update.bak")));
    }
}
