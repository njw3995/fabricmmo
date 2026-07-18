package io.github.njw3995.fabricmmo.core.persistence;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Pinned mcMMO-shaped MySQL/MariaDB settings from config.yml. */
public record MySqlSettings(
        boolean enabled,
        boolean debug,
        String address,
        int port,
        String database,
        String username,
        String password,
        String tablePrefix,
        boolean ssl,
        boolean allowPublicKeyRetrieval,
        int maxPoolSize) {
    public MySqlSettings {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(tablePrefix, "tablePrefix");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid MySQL port: " + port);
        if (!tablePrefix.matches("[A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("MySQL table prefix may contain only letters, digits, and underscores");
        }
        if (maxPoolSize < 1) throw new IllegalArgumentException("MySQL max pool size must be positive");
    }

    public static MySqlSettings load(Path configFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        int pool = Math.max(1, Math.max(
                config.integer("MySQL.Database.MaxPoolSize.Load", 20),
                Math.max(config.integer("MySQL.Database.MaxPoolSize.Save", 20),
                        config.integer("MySQL.Database.MaxPoolSize.Misc", 10))));
        return new MySqlSettings(
                config.bool("MySQL.Enabled", false),
                config.bool("MySQL.Debug", false),
                config.string("MySQL.Server.Address", "localhost"),
                config.integer("MySQL.Server.Port", 3306),
                config.string("MySQL.Database.Name", "DataBaseName"),
                config.string("MySQL.Database.User_Name", "UserName"),
                config.string("MySQL.Database.User_Password", "UserPassword"),
                config.string("MySQL.Database.TablePrefix", "mcmmo_"),
                config.bool("MySQL.Server.SSL", true),
                config.bool("MySQL.Server.allowPublicKeyRetrieval", true),
                pool);
    }

    public String jdbcUrl() {
        return "jdbc:mariadb://" + address + ':' + port + '/' + database
                + "?useSsl=" + ssl
                + "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval
                + "&useUnicode=true&characterEncoding=utf8";
    }

    public String playersTable() { return tablePrefix + "fabricmmo_players"; }
    public String skillsTable() { return tablePrefix + "fabricmmo_skills"; }
}
