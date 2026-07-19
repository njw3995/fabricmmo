package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Atomic file-backed Tree Feller cooldown persistence. */
public final class PropertiesWoodcuttingAbilityStore implements WoodcuttingAbilityStore {
    private static final String TREE_FELLER = "treeFellerLastUsed";
    private final Path directory;
    private boolean closed;

    public PropertiesWoodcuttingAbilityStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    @Override
    public synchronized WoodcuttingAbilityData load(UUID playerId) throws IOException {
        requireOpen();
        Path file = file(playerId);
        if (!Files.isRegularFile(file)) {
            return WoodcuttingAbilityData.EMPTY;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        return new WoodcuttingAbilityData(parseLong(properties, TREE_FELLER, file));
    }

    @Override
    public synchronized void save(UUID playerId, WoodcuttingAbilityData data) throws IOException {
        requireOpen();
        Properties properties = new Properties();
        properties.setProperty(TREE_FELLER, Long.toString(data.treeFellerLastUsed()));
        Path target = file(playerId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO Woodcutting ability cooldowns");
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
            throw new IllegalStateException("Woodcutting ability store is closed");
        }
    }
}
