package io.github.njw3995.fabricmmo.core.administration;

import io.github.njw3995.fabricmmo.core.persistence.ManagedProgressionStore;
import io.github.njw3995.fabricmmo.core.persistence.MySqlSettings;
import io.github.njw3995.fabricmmo.core.persistence.PlayerProgressionData;
import io.github.njw3995.fabricmmo.core.persistence.ProgressionStoreFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Backup-first storage conversion and destructive maintenance operations. */
public final class ProgressionMaintenanceService {
    private final ManagedProgressionStore activeStore;
    private final Path playerDirectory;
    private final MySqlSettings mysql;
    private final Clock clock;

    public ProgressionMaintenanceService(
            ManagedProgressionStore activeStore,
            Path playerDirectory,
            MySqlSettings mysql,
            Clock clock) {
        this.activeStore = Objects.requireNonNull(activeStore, "activeStore");
        this.playerDirectory = playerDirectory.toAbsolutePath().normalize();
        this.mysql = Objects.requireNonNull(mysql, "mysql");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public String backendName() { return activeStore.backendName(); }

    public ManagedProgressionStore activeStore() { return activeStore; }

    public ConversionResult convert(String sourceBackend, String targetBackend) throws IOException {
        String sourceName = normalize(sourceBackend);
        String targetName = normalize(targetBackend);
        if (sourceName.equals(targetName)) {
            return new ConversionResult(0, null, "Source and target storage are identical.");
        }
        Path backup = sourceName.equals("flatfile") ? backupFlatFiles() : null;
        int converted = 0;
        try (ManagedProgressionStore source = sourceName.equals(activeStore.backendName())
                ? new NonClosingManagedStore(activeStore)
                : ProgressionStoreFactory.openNamed(sourceName, playerDirectory, mysql);
             ManagedProgressionStore target = targetName.equals(activeStore.backendName())
                     ? new NonClosingManagedStore(activeStore)
                     : ProgressionStoreFactory.openNamed(targetName, playerDirectory, mysql)) {
            for (UUID id : source.playerIds()) {
                target.save(source.load(id));
                Instant seen = source.lastSeen(id);
                if (!seen.equals(Instant.EPOCH)) target.touch(id, seen);
                converted++;
            }
        }
        return new ConversionResult(converted, backup,
                "Converted " + converted + " player records from " + sourceName + " to " + targetName + '.');
    }

    public PurgeResult purgePowerless() throws IOException {
        List<UUID> removed = new ArrayList<>();
        for (UUID id : activeStore.playerIds()) {
            PlayerProgressionData data = activeStore.load(id);
            boolean powerless = data.skills().values().stream().allMatch(value -> value.level() <= 0);
            if (powerless && activeStore.delete(id)) removed.add(id);
        }
        return new PurgeResult(removed.size(), List.copyOf(removed), "powerless");
    }

    public PurgeResult purgeOldUsers(int cutoffMonths) throws IOException {
        if (cutoffMonths < 0) return new PurgeResult(0, List.of(), "disabled");
        Instant cutoff = clock.instant().minus(Math.max(1L, cutoffMonths) * 30L, ChronoUnit.DAYS);
        List<UUID> removed = new ArrayList<>();
        for (UUID id : activeStore.playerIds()) {
            Instant lastSeen = activeStore.lastSeen(id);
            if (!lastSeen.equals(Instant.EPOCH) && lastSeen.isBefore(cutoff) && activeStore.delete(id)) {
                removed.add(id);
            }
        }
        return new PurgeResult(removed.size(), List.copyOf(removed), "older than " + cutoffMonths + " months");
    }

    public boolean delete(UUID playerId) throws IOException { return activeStore.delete(playerId); }

    public void touch(UUID playerId) throws IOException { activeStore.touch(playerId, clock.instant()); }

    public Path backupFlatFiles() throws IOException {
        Path backup = playerDirectory.resolveSibling("players-backup-" + clock.instant().toEpochMilli());
        Files.createDirectories(backup);
        if (Files.isDirectory(playerDirectory)) {
            try (var paths = Files.list(playerDirectory)) {
                for (Path source : paths.filter(Files::isRegularFile).toList()) {
                    Files.copy(source, backup.resolve(source.getFileName()),
                            StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return backup;
    }

    private static String normalize(String value) {
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "flat", "flatfile", "properties" -> "flatfile";
            case "sql", "mysql", "mariadb" -> "mysql";
            default -> throw new IllegalArgumentException("Unknown storage backend: " + value);
        };
    }

    public record ConversionResult(int convertedPlayers, Path backup, String detail) { }
    public record PurgeResult(int removedPlayers, List<UUID> removed, String reason) { }

    private static final class NonClosingManagedStore implements ManagedProgressionStore {
        private final ManagedProgressionStore delegate;
        private NonClosingManagedStore(ManagedProgressionStore delegate) { this.delegate = delegate; }
        @Override public String backendName() { return delegate.backendName(); }
        @Override public java.util.Set<UUID> playerIds() throws IOException { return delegate.playerIds(); }
        @Override public boolean delete(UUID id) throws IOException { return delegate.delete(id); }
        @Override public Instant lastSeen(UUID id) throws IOException { return delegate.lastSeen(id); }
        @Override public void touch(UUID id, Instant instant) throws IOException { delegate.touch(id, instant); }
        @Override public PlayerProgressionData load(UUID id) throws IOException { return delegate.load(id); }
        @Override public void save(PlayerProgressionData data) throws IOException { delegate.save(data); }
        @Override public void close() { }
    }
}
