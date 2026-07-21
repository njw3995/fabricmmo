package io.github.njw3995.fabricmmo.core.skill.unarmed;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Atomic file-backed Berserk cooldown persistence. */
public final class PropertiesUnarmedAbilityStore implements UnarmedAbilityStore {
    private static final String KEY = "berserkLastUsed";
    private final Path directory;
    private boolean closed;

    public PropertiesUnarmedAbilityStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    @Override
    public synchronized UnarmedAbilityData load(UUID playerId) throws IOException {
        requireOpen();
        Path file = file(playerId);
        if (!Files.isRegularFile(file)) return UnarmedAbilityData.EMPTY;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        String raw = properties.getProperty(KEY, "0");
        try {
            long value = Long.parseLong(raw);
            if (value < 0L) throw new NumberFormatException("negative");
            return new UnarmedAbilityData(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid " + KEY + " in " + file + ": " + raw, exception);
        }
    }

    @Override
    public synchronized void save(UUID playerId, UnarmedAbilityData data) throws IOException {
        requireOpen();
        Properties properties = new Properties();
        properties.setProperty(KEY, Long.toString(data.berserkLastUsed()));
        Path target = file(playerId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO Unarmed ability cooldowns");
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
        if (closed) throw new IllegalStateException("Unarmed ability store is closed");
    }
}
