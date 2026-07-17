package io.github.njw3995.fabricmmo.core.skill.mining;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Atomic file-backed Mining cooldown persistence. */
public final class PropertiesMiningAbilityStore implements MiningAbilityStore {
    private static final String SUPER_BREAKER = "superBreakerLastUsed";
    private static final String BLAST_MINING = "blastMiningLastUsed";
    private final Path directory;
    private boolean closed;

    public PropertiesMiningAbilityStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    @Override
    public synchronized MiningAbilityData load(UUID playerId) throws IOException {
        requireOpen();
        Path file = file(playerId);
        if (!Files.isRegularFile(file)) {
            return MiningAbilityData.EMPTY;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return new MiningAbilityData(
                parseLong(properties, SUPER_BREAKER, file),
                parseLong(properties, BLAST_MINING, file));
    }

    @Override
    public synchronized void save(UUID playerId, MiningAbilityData data) throws IOException {
        requireOpen();
        Properties properties = new Properties();
        properties.setProperty(SUPER_BREAKER, Long.toString(data.superBreakerLastUsed()));
        properties.setProperty(BLAST_MINING, Long.toString(data.blastMiningLastUsed()));
        Path target = file(playerId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO Mining ability cooldowns");
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

    private Path file(UUID playerId) {
        return directory.resolve(playerId + ".properties");
    }

    private static long parseLong(Properties properties, String key, Path file) throws IOException {
        String value = properties.getProperty(key, "0");
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0L) {
                throw new NumberFormatException("negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid " + key + " in " + file + ": " + value, exception);
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Mining ability store is closed");
        }
    }
}
