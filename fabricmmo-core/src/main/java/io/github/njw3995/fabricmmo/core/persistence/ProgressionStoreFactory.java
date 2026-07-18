package io.github.njw3995.fabricmmo.core.persistence;

import java.io.IOException;
import java.nio.file.Path;

public final class ProgressionStoreFactory {
    private ProgressionStoreFactory() { }

    public static ManagedProgressionStore open(Path playerDirectory, MySqlSettings mysql) throws IOException {
        return mysql.enabled() ? new JdbcProgressionStore(mysql) : new PropertiesProgressionStore(playerDirectory);
    }

    public static ManagedProgressionStore openNamed(
            String backend, Path playerDirectory, MySqlSettings mysql) throws IOException {
        return switch (backend.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "flatfile", "flat", "properties" -> new PropertiesProgressionStore(playerDirectory);
            case "mysql", "mariadb", "sql" -> new JdbcProgressionStore(mysql);
            default -> throw new IllegalArgumentException("Unknown storage backend: " + backend);
        };
    }
}
