package io.github.njw3995.fabricmmo.core.persistence;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Transactional MariaDB/MySQL persistence preserving arbitrary namespaced skills. */
public final class JdbcProgressionStore implements ManagedProgressionStore {
    private final MySqlSettings settings;
    private final JdbcConnectionPool pool;

    public JdbcProgressionStore(MySqlSettings settings) throws IOException {
        this.settings = settings;
        this.pool = new JdbcConnectionPool(settings);
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            initializeSchema();
        } catch (ClassNotFoundException exception) {
            pool.close();
            throw new IOException("MariaDB JDBC driver is not available", exception);
        } catch (IOException exception) {
            pool.close();
            throw exception;
        }
    }

    @Override
    public String backendName() { return "mysql"; }

    @Override
    public PlayerProgressionData load(UUID playerId) throws IOException {
        String playerSql = "SELECT revision FROM " + settings.playersTable() + " WHERE uuid=?";
        String skillSql = "SELECT skill_id, level, xp FROM " + settings.skillsTable() + " WHERE uuid=?";
        try (Connection connection = connection()) {
            long revision = 0L;
            try (PreparedStatement statement = connection.prepareStatement(playerSql)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return PlayerProgressionData.empty(playerId);
                    revision = result.getLong(1);
                }
            }
            Map<NamespacedId, StoredSkillProgress> skills = new TreeMap<>();
            try (PreparedStatement statement = connection.prepareStatement(skillSql)) {
                statement.setString(1, playerId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        NamespacedId id = NamespacedId.parse(result.getString(1));
                        skills.put(id, new StoredSkillProgress(result.getInt(2), result.getDouble(3)));
                    }
                }
            }
            return new PlayerProgressionData(playerId, revision, skills);
        } catch (SQLException exception) {
            throw io("load", exception);
        }
    }

    @Override
    public void save(PlayerProgressionData data) throws IOException {
        String upsertPlayer = "INSERT INTO " + settings.playersTable()
                + " (uuid, revision, last_seen) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE revision=VALUES(revision), last_seen=VALUES(last_seen)";
        String deleteSkills = "DELETE FROM " + settings.skillsTable() + " WHERE uuid=?";
        String insertSkill = "INSERT INTO " + settings.skillsTable() + " (uuid, skill_id, level, xp) VALUES (?, ?, ?, ?)";
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertPlayer)) {
                    statement.setString(1, data.playerId().toString());
                    statement.setLong(2, data.revision());
                    statement.setLong(3, Instant.now().toEpochMilli());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(deleteSkills)) {
                    statement.setString(1, data.playerId().toString());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(insertSkill)) {
                    for (Map.Entry<NamespacedId, StoredSkillProgress> entry : data.skills().entrySet()) {
                        statement.setString(1, data.playerId().toString());
                        statement.setString(2, entry.getKey().toString());
                        statement.setInt(3, entry.getValue().level());
                        statement.setDouble(4, entry.getValue().xp());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw io("save", exception);
        }
    }

    @Override
    public Set<UUID> playerIds() throws IOException {
        TreeSet<UUID> ids = new TreeSet<>();
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT uuid FROM " + settings.playersTable())) {
            while (result.next()) ids.add(UUID.fromString(result.getString(1)));
            return Set.copyOf(ids);
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IOException("Unable to list MySQL progression players", exception);
        }
    }

    @Override
    public boolean delete(UUID playerId) throws IOException {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement skills = connection.prepareStatement(
                        "DELETE FROM " + settings.skillsTable() + " WHERE uuid=?")) {
                    skills.setString(1, playerId.toString());
                    skills.executeUpdate();
                }
                int changed;
                try (PreparedStatement player = connection.prepareStatement(
                        "DELETE FROM " + settings.playersTable() + " WHERE uuid=?")) {
                    player.setString(1, playerId.toString());
                    changed = player.executeUpdate();
                }
                connection.commit();
                return changed > 0;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw io("delete", exception);
        }
    }

    @Override
    public Instant lastSeen(UUID playerId) throws IOException {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT last_seen FROM " + settings.playersTable() + " WHERE uuid=?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Instant.ofEpochMilli(result.getLong(1)) : Instant.EPOCH;
            }
        } catch (SQLException exception) {
            throw io("read last_seen", exception);
        }
    }

    @Override
    public void touch(UUID playerId, Instant instant) throws IOException {
        String sql = "INSERT INTO " + settings.playersTable()
                + " (uuid, revision, last_seen) VALUES (?, 0, ?) ON DUPLICATE KEY UPDATE last_seen=VALUES(last_seen)";
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setLong(2, instant.toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw io("touch", exception);
        }
    }

    private void initializeSchema() throws IOException {
        String players = "CREATE TABLE IF NOT EXISTS " + settings.playersTable() + " ("
                + "uuid CHAR(36) NOT NULL PRIMARY KEY, revision BIGINT NOT NULL DEFAULT 0, last_seen BIGINT NOT NULL DEFAULT 0)";
        String skills = "CREATE TABLE IF NOT EXISTS " + settings.skillsTable() + " ("
                + "uuid CHAR(36) NOT NULL, skill_id VARCHAR(191) NOT NULL, level INT NOT NULL, xp DOUBLE NOT NULL, "
                + "PRIMARY KEY (uuid, skill_id), INDEX idx_skill_level (skill_id, level), "
                + "CONSTRAINT " + foreignKeyName() + " FOREIGN KEY (uuid) REFERENCES "
                + settings.playersTable() + " (uuid) ON DELETE CASCADE)";
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute(players);
            statement.execute(skills);
        } catch (SQLException exception) {
            throw io("initialize schema", exception);
        }
    }


    private String foreignKeyName() {
        String value = settings.tablePrefix() + "fabricmmo_player_fk";
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private Connection connection() throws SQLException {
        return pool.borrow();
    }

    @Override
    public void close() {
        pool.close();
    }

    int pooledConnections() { return pool.totalConnections(); }
    int idleConnections() { return pool.idleConnections(); }

    private IOException io(String operation, SQLException exception) {
        return new IOException("Unable to " + operation + " FabricMMO MySQL data: " + exception.getMessage(), exception);
    }
}
