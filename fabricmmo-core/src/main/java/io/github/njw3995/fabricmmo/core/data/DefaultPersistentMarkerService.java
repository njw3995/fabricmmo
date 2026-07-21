package io.github.njw3995.fabricmmo.core.data;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.data.PersistentMarkerService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Atomic world-scoped marker persistence used by external addons. */
public final class DefaultPersistentMarkerService implements PersistentMarkerService, AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger("FabricMMO/PersistentMarkers");
    private static final String FORMAT_VERSION = "1";
    private static final String ENTRY_PREFIX = "entry.";

    private final Map<NamespacedId, Map<String, Set<String>>> markers = new TreeMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "FabricMMO-Persistent-Markers");
        thread.setDaemon(true);
        return thread;
    });
    private Path path;
    private long revision;
    private long savedRevision;
    private Future<?> pendingSave;
    private IOException asyncFailure;
    private boolean closed;

    public synchronized void bind(Path path) throws IOException {
        requireOpen();
        Path normalized = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (this.path != null) {
            if (this.path.equals(normalized)) return;
            throw new IllegalStateException("Persistent marker service is already bound to " + this.path);
        }
        this.path = normalized;
        load();
    }

    @Override
    public synchronized boolean available() {
        return path != null && !closed;
    }

    @Override
    public synchronized boolean markOnce(NamespacedId markerType, String subject, String value) {
        requireAvailable();
        validate(markerType, subject, value);
        boolean added = markers
                .computeIfAbsent(markerType, ignored -> new TreeMap<>())
                .computeIfAbsent(subject, ignored -> new TreeSet<>())
                .add(value);
        if (added) revision++;
        return added;
    }

    @Override
    public synchronized boolean contains(NamespacedId markerType, String subject, String value) {
        requireAvailable();
        validate(markerType, subject, value);
        return markers.getOrDefault(markerType, Map.of())
                .getOrDefault(subject, Set.of())
                .contains(value);
    }

    @Override
    public synchronized Set<String> values(NamespacedId markerType, String subject) {
        requireAvailable();
        if (markerType == null) throw new NullPointerException("markerType");
        validateText(subject, "subject");
        return Set.copyOf(markers.getOrDefault(markerType, Map.of())
                .getOrDefault(subject, Set.of()));
    }

    @Override
    public synchronized boolean remove(NamespacedId markerType, String subject, String value) {
        requireAvailable();
        validate(markerType, subject, value);
        Map<String, Set<String>> subjects = markers.get(markerType);
        if (subjects == null) return false;
        Set<String> values = subjects.get(subject);
        if (values == null || !values.remove(value)) return false;
        if (values.isEmpty()) subjects.remove(subject);
        if (subjects.isEmpty()) markers.remove(markerType);
        revision++;
        return true;
    }

    public synchronized boolean dirty() {
        return revision != savedRevision;
    }

    public synchronized int markerCount() {
        return markers.values().stream()
                .flatMap(subjects -> subjects.values().stream())
                .mapToInt(Set::size)
                .sum();
    }

    public synchronized void flushAsync() {
        requireAvailable();
        if (!dirty() || (pendingSave != null && !pendingSave.isDone())) {
            return;
        }
        Snapshot snapshot = snapshot();
        pendingSave = executor.submit(() -> {
            try {
                write(snapshot);
                synchronized (DefaultPersistentMarkerService.this) {
                    asyncFailure = null;
                }
            } catch (IOException exception) {
                synchronized (DefaultPersistentMarkerService.this) {
                    asyncFailure = exception;
                }
                LOGGER.log(System.Logger.Level.ERROR,
                        "Unable to save FabricMMO persistent addon markers to " + snapshot.path(),
                        exception);
            }
        });
    }

    public void flush() throws IOException {
        Future<?> pending;
        synchronized (this) {
            if (path == null) return;
            pending = pendingSave;
        }
        await(pending);

        Snapshot snapshot;
        IOException previousFailure;
        synchronized (this) {
            previousFailure = asyncFailure;
            if (!dirty()) {
                if (previousFailure != null) throw previousFailure;
                return;
            }
            snapshot = snapshot();
        }
        try {
            write(snapshot);
            synchronized (this) {
                asyncFailure = null;
            }
        } catch (IOException exception) {
            if (previousFailure != null && previousFailure != exception) {
                exception.addSuppressed(previousFailure);
            }
            throw exception;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        IOException failure = null;
        try {
            flush();
        } catch (IOException exception) {
            failure = exception;
        } finally {
            executor.shutdown();
        }
        if (failure != null) throw failure;
    }

    private synchronized void load() throws IOException {
        markers.clear();
        revision = 0L;
        savedRevision = 0L;
        if (!Files.isRegularFile(path)) return;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        String version = properties.getProperty("format.version");
        if (!FORMAT_VERSION.equals(version)) {
            throw new IOException("Unsupported marker format version " + version + " in " + path);
        }
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(ENTRY_PREFIX) || !Boolean.parseBoolean(properties.getProperty(key))) continue;
            String[] parts = key.substring(ENTRY_PREFIX.length()).split("\\.", -1);
            if (parts.length != 3) {
                throw new IOException("Malformed marker key " + key + " in " + path);
            }
            try {
                NamespacedId type = NamespacedId.parse(decode(parts[0]));
                String subject = decode(parts[1]);
                String value = decode(parts[2]);
                validate(type, subject, value);
                markers.computeIfAbsent(type, ignored -> new TreeMap<>())
                        .computeIfAbsent(subject, ignored -> new TreeSet<>())
                        .add(value);
            } catch (RuntimeException exception) {
                throw new IOException("Invalid marker key " + key + " in " + path, exception);
            }
        }
    }

    private synchronized Snapshot snapshot() {
        Properties properties = new Properties();
        properties.setProperty("format.version", FORMAT_VERSION);
        for (var type : markers.entrySet()) {
            for (var subject : type.getValue().entrySet()) {
                for (String value : subject.getValue()) {
                    properties.setProperty(
                            ENTRY_PREFIX + encode(type.getKey().toString()) + "."
                                    + encode(subject.getKey()) + "." + encode(value),
                            "true");
                }
            }
        }
        return new Snapshot(path, properties, revision);
    }

    private void write(Snapshot snapshot) throws IOException {
        Files.createDirectories(snapshot.path().getParent());
        Path temporary = snapshot.path().resolveSibling(snapshot.path().getFileName() + ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            snapshot.properties().store(output, "FabricMMO addon persistent markers");
        }
        try {
            Files.move(temporary, snapshot.path(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, snapshot.path(), StandardCopyOption.REPLACE_EXISTING);
        }
        synchronized (this) {
            savedRevision = Math.max(savedRevision, snapshot.revision());
        }
    }

    private static void await(Future<?> future) throws IOException {
        if (future == null) return;
        try {
            future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while saving persistent markers", exception);
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IOException("Unable to save persistent markers", exception.getCause());
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) throws IOException {
        try {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid base64 marker component", exception);
        }
    }

    private static void validate(NamespacedId markerType, String subject, String value) {
        if (markerType == null) throw new NullPointerException("markerType");
        validateText(subject, "subject");
        validateText(value, "value");
    }

    private static void validateText(String value, String name) {
        if (value == null) throw new NullPointerException(name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        if (value.length() > 1024) throw new IllegalArgumentException(name + " exceeds 1024 characters");
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Persistent marker service is closed");
    }

    private void requireAvailable() {
        requireOpen();
        if (path == null) {
            throw new IllegalStateException("Persistent marker service is not bound to a world");
        }
    }

    private record Snapshot(Path path, Properties properties, long revision) {}
}
