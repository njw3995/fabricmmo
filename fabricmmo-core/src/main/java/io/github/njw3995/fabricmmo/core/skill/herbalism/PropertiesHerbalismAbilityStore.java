package io.github.njw3995.fabricmmo.core.skill.herbalism;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.UUID;

/** Atomic file-backed Green Terra cooldown persistence. */
public final class PropertiesHerbalismAbilityStore implements HerbalismAbilityStore {
    private static final String GREEN_TERRA = "greenTerraLastUsed";
    private final Path directory;
    private boolean closed;

    public PropertiesHerbalismAbilityStore(Path directory) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        Files.createDirectories(this.directory);
    }

    @Override
    public synchronized HerbalismAbilityData load(UUID playerId) throws IOException {
        requireOpen();
        Path file = file(playerId);
        if (!Files.isRegularFile(file)) {
            return HerbalismAbilityData.EMPTY;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        }
        String raw = properties.getProperty(GREEN_TERRA, "0");
        try {
            long value = Long.parseLong(raw);
            if (value < 0L) {
                throw new NumberFormatException("negative");
            }
            return new HerbalismAbilityData(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid " + GREEN_TERRA + " in " + file + ": " + raw,
                    exception);
        }
    }

    @Override
    public synchronized void save(UUID playerId, HerbalismAbilityData data) throws IOException {
        requireOpen();
        Properties properties = new Properties();
        properties.setProperty(GREEN_TERRA, Long.toString(data.greenTerraLastUsed()));
        Path target = file(playerId);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO Herbalism ability cooldowns");
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

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Herbalism ability store is closed");
        }
    }
}
