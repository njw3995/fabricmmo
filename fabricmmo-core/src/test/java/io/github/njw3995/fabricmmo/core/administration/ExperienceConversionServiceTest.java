package io.github.njw3995.fabricmmo.core.administration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.PropertiesProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import io.github.njw3995.fabricmmo.core.progression.ProgressionFormula;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperienceConversionServiceTest {
    @TempDir
    Path directory;

    @Test
    void preservesTotalExperienceAndUnknownAddonSkills() throws Exception {
        Path players = directory.resolve("players");
        PropertiesProgressionStore store = new PropertiesProgressionStore(players);
        UUID playerId = UUID.randomUUID();
        NamespacedId mining = NamespacedId.parse("fabricmmo:mining");
        NamespacedId addon = NamespacedId.parse("example:addon_skill");
        PlayerProgressionData before = new PlayerProgressionData(
                playerId,
                3,
                Map.of(
                        mining, new StoredSkillProgress(4, 321.0D),
                        addon, new StoredSkillProgress(2, 111.0D)));
        store.save(before);

        ProgressionSettings settings = ProgressionSettings.upstreamDefaults();
        ProgressionFormula formula = new ProgressionFormula(settings.curve());
        double miningTotal = total(
                formula, before.skills().get(mining), settings.mode(), FormulaType.LINEAR);
        double addonTotal = total(
                formula, before.skills().get(addon), settings.mode(), FormulaType.LINEAR);
        ExperienceConversionService service = new ExperienceConversionService(
                store,
                settings,
                directory.resolve("formula.properties"),
                players,
                Clock.fixed(Instant.parse("2026-07-18T12:00:00Z"), ZoneOffset.UTC));

        ExperienceConversionService.ConversionResult result =
                service.convert(FormulaType.EXPONENTIAL);
        PlayerProgressionData after = store.load(playerId);

        assertEquals(1, result.convertedPlayers());
        assertNotNull(result.backup());
        assertTrue(Files.isDirectory(result.backup()));
        assertEquals(FormulaType.EXPONENTIAL, service.previousFormula());
        assertTrue(after.skills().containsKey(addon));
        assertEquals(
                Math.floor(miningTotal),
                total(formula, after.skills().get(mining), settings.mode(), FormulaType.EXPONENTIAL));
        assertEquals(
                Math.floor(addonTotal),
                total(formula, after.skills().get(addon), settings.mode(), FormulaType.EXPONENTIAL));
        assertEquals(0, service.convert(FormulaType.EXPONENTIAL).convertedPlayers());
    }

    private static double total(
            ProgressionFormula formula,
            StoredSkillProgress progress,
            ProgressionMode mode,
            FormulaType type) {
        double total = progress.xp();
        for (int level = 0; level < progress.level(); level++) {
            total += formula.xpToNextLevel(level, mode, type);
        }
        return total;
    }
}
