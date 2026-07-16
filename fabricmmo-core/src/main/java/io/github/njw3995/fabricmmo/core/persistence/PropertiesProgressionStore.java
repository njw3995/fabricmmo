package io.github.njw3995.fabricmmo.core.persistence;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Versioned atomic persistence that preserves unknown namespaced skill data.
 */
public final class PropertiesProgressionStore implements ProgressionStore {
    private static final String FORMAT_VERSION = "1";
    private final Path directory;

    public PropertiesProgressionStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }

    @Override
    public synchronized PlayerProgressionData load(UUID playerId) throws IOException {
        Path path = path(playerId);
        if (!Files.exists(path)) {
            return PlayerProgressionData.empty(playerId);
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        String version = properties.getProperty("format.version");
        if (!FORMAT_VERSION.equals(version)) {
            throw new IOException("Unsupported progression format version " + version + " in " + path);
        }
        UUID storedPlayerId = UUID.fromString(required(properties, "player.uuid", path));
        if (!playerId.equals(storedPlayerId)) {
            throw new IOException("Player UUID mismatch in " + path);
        }
        long revision = Long.parseLong(required(properties, "revision", path));
        Map<NamespacedId, StoredSkillProgress> skills = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("skill.") || !key.endsWith(".level")) {
                continue;
            }
            String encodedId = key.substring("skill.".length(), key.length() - ".level".length());
            NamespacedId id = NamespacedId.parse(encodedId.replace('|', ':'));
            int level = Integer.parseInt(properties.getProperty(key));
            int xp = Integer.parseInt(required(properties, "skill." + encodedId + ".xp", path));
            skills.put(id, new StoredSkillProgress(level, xp));
        }
        return new PlayerProgressionData(playerId, revision, skills);
    }

    @Override
    public synchronized void save(PlayerProgressionData data) throws IOException {
        Path target = path(data.playerId());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Properties properties = new Properties();
        properties.setProperty("format.version", FORMAT_VERSION);
        properties.setProperty("player.uuid", data.playerId().toString());
        properties.setProperty("revision", Long.toString(data.revision()));
        for (Map.Entry<NamespacedId, StoredSkillProgress> entry : data.skills().entrySet()) {
            String encodedId = entry.getKey().toString().replace(':', '|');
            properties.setProperty("skill." + encodedId + ".level",
                    Integer.toString(entry.getValue().level()));
            properties.setProperty("skill." + encodedId + ".xp",
                    Integer.toString(entry.getValue().xp()));
        }
        try (OutputStream output = Files.newOutputStream(temporary)) {
            properties.store(output, "FabricMMO player progression; modified format version 1");
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path path(UUID playerId) {
        return directory.resolve(playerId + ".properties");
    }

    private static String required(Properties properties, String key, Path path) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IOException("Missing required key " + key + " in " + path);
        }
        return value;
    }
}
