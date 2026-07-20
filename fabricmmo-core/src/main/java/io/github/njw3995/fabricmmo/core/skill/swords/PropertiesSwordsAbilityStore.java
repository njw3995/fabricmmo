package io.github.njw3995.fabricmmo.core.skill.swords;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Atomic file-backed Serrated Strikes cooldown persistence. */
public final class PropertiesSwordsAbilityStore implements SwordsAbilityStore {
    private static final String KEY = "serratedStrikesLastUsed";
    private final Path directory;
    private boolean closed;

    public PropertiesSwordsAbilityStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    @Override
    public synchronized SwordsAbilityData load(UUID playerId) throws IOException {
        requireOpen();
        Path file = file(playerId);
        if (!Files.isRegularFile(file)) return SwordsAbilityData.EMPTY;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        String raw = properties.getProperty(KEY, "0");
        try {
            long value = Long.parseLong(raw);
            if (value < 0L) throw new NumberFormatException("negative");
            return new SwordsAbilityData(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid " + KEY + " in " + file + ": " + raw, exception);
        }
    }

    @Override
    public synchronized void save(UUID playerId, SwordsAbilityData data) throws IOException {
        requireOpen();
        Properties properties = new Properties();
        properties.setProperty(KEY, Long.toString(data.serratedStrikesLastUsed()));
        Path target = file(playerId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO Swords ability cooldowns");
        }
        try {
            Files.move(temporary, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    private Path file(UUID playerId) { return directory.resolve(playerId + ".properties"); }
    private void requireOpen() {
        if (closed) throw new IllegalStateException("Swords ability store is closed");
    }
}
