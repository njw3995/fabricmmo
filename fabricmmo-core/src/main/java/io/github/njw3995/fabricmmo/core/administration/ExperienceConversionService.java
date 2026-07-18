package io.github.njw3995.fabricmmo.core.administration;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.core.persistence.ManagedProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.StoredSkillProgress;
import io.github.njw3995.fabricmmo.core.progression.ProgressionFormula;
import io.github.njw3995.fabricmmo.core.progression.ProgressionSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Converts stored levels and partial XP between mcMMO's linear and exponential curves.
 *
 * <p>The implementation mirrors the pinned upstream conversion: reconstruct total XP using the
 * previously recorded curve, remove the configured global XP modifier, then replay that XP through
 * the requested curve while respecting each skill's configured level cap. Unknown addon skills are
 * preserved and converted with the default unlimited cap.</p>
 */
public final class ExperienceConversionService {
    private static final String PREVIOUS_FORMULA_KEY = "Previous_Formula";

    private final ManagedProgressionStore store;
    private final ProgressionSettings settings;
    private final ProgressionFormula formula;
    private final Path formulaFile;
    private final Path playerDataDirectory;
    private final Clock clock;

    public ExperienceConversionService(
            ManagedProgressionStore store,
            ProgressionSettings settings,
            Path formulaFile,
            Path playerDataDirectory,
            Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.formula = new ProgressionFormula(settings.curve());
        this.formulaFile = formulaFile.toAbsolutePath().normalize();
        this.playerDataDirectory = playerDataDirectory.toAbsolutePath().normalize();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public FormulaType previousFormula() throws IOException {
        if (!Files.isRegularFile(formulaFile)) {
            // Upstream treats UNKNOWN as LINEAR while reconstructing total experience.
            return FormulaType.LINEAR;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(formulaFile)) {
            properties.load(input);
        }
        String configured = properties.getProperty(PREVIOUS_FORMULA_KEY, "LINEAR");
        try {
            return FormulaType.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FormulaType.LINEAR;
        }
    }

    public ConversionResult convert(FormulaType targetFormula) throws IOException {
        Objects.requireNonNull(targetFormula, "targetFormula");
        FormulaType sourceFormula = previousFormula();
        if (sourceFormula == targetFormula) {
            return new ConversionResult(
                    0, sourceFormula, targetFormula, null,
                    "Already using formula type " + targetFormula + '.');
        }

        Path backup = store.backendName().equals("flatfile") ? backupFlatFiles() : null;
        int converted = 0;
        for (UUID playerId : store.playerIds()) {
            PlayerProgressionData existing = store.load(playerId);
            TreeMap<NamespacedId, StoredSkillProgress> skills = new TreeMap<>();
            for (var entry : existing.skills().entrySet()) {
                skills.put(entry.getKey(), convertSkill(
                        entry.getKey(), entry.getValue(), sourceFormula, targetFormula));
            }
            store.save(new PlayerProgressionData(
                    playerId, existing.revision() + 1L, skills));
            converted++;
        }
        savePreviousFormula(targetFormula);
        return new ConversionResult(
                converted, sourceFormula, targetFormula, backup,
                "Formula conversion complete; now using " + targetFormula + " XP curve.");
    }

    private StoredSkillProgress convertSkill(
            NamespacedId skillId,
            StoredSkillProgress old,
            FormulaType sourceFormula,
            FormulaType targetFormula) {
        double totalOldXp = old.xp();
        for (int level = 0; level < old.level(); level++) {
            totalOldXp += formula.xpToNextLevel(level, settings.mode(), sourceFormula);
        }
        // This is the same global-modifier normalization performed by upstream FormulaConversionTask.
        double experience = Math.floor(totalOldXp / settings.globalXpMultiplier());
        int level = 0;
        int cap = settings.levelCap(skillId);
        while (experience > 0.0D && level < cap) {
            int required = formula.xpToNextLevel(level, settings.mode(), targetFormula);
            if (experience < required) {
                break;
            }
            experience -= required;
            level++;
        }
        double remainder = level >= cap ? 0.0D : Math.max(0.0D, experience);
        return new StoredSkillProgress(level, remainder);
    }

    private Path backupFlatFiles() throws IOException {
        Path backup = playerDataDirectory.resolveSibling(
                "players-formula-backup-" + clock.instant().toEpochMilli());
        Files.createDirectories(backup);
        if (Files.isDirectory(playerDataDirectory)) {
            try (var paths = Files.list(playerDataDirectory)) {
                for (Path source : paths.filter(Files::isRegularFile).toList()) {
                    Files.copy(
                            source,
                            backup.resolve(source.getFileName()),
                            StandardCopyOption.COPY_ATTRIBUTES,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return backup;
    }

    private void savePreviousFormula(FormulaType formulaType) throws IOException {
        Files.createDirectories(formulaFile.getParent());
        Properties properties = new Properties();
        properties.setProperty(PREVIOUS_FORMULA_KEY, formulaType.name());
        Path temporary = formulaFile.resolveSibling(formulaFile.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO experience formula history");
        }
        try {
            Files.move(
                    temporary,
                    formulaFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            Files.move(temporary, formulaFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record ConversionResult(
            int convertedPlayers,
            FormulaType sourceFormula,
            FormulaType targetFormula,
            Path backup,
            String detail) {
    }
}
