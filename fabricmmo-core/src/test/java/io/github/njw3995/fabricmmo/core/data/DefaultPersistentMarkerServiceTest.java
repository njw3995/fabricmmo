package io.github.njw3995.fabricmmo.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultPersistentMarkerServiceTest {
    private static final NamespacedId TYPE = NamespacedId.parse("test:first_capture");

    @TempDir
    Path tempDirectory;

    @Test
    void requiresWorldBindingAndPersistsAtomically() throws IOException {
        Path file = tempDirectory.resolve("data").resolve("markers.properties");
        DefaultPersistentMarkerService service = new DefaultPersistentMarkerService();
        assertFalse(service.available());

        assertThrows(IllegalStateException.class,
                () -> service.markOnce(TYPE, "player", "species"));

        service.bind(file);
        service.bind(file);
        assertTrue(service.available());
        assertTrue(service.markOnce(TYPE, "player", "species"));
        assertFalse(service.markOnce(TYPE, "player", "species"));
        assertTrue(service.contains(TYPE, "player", "species"));
        assertEquals(java.util.Set.of("species"), service.values(TYPE, "player"));
        service.flush();
        service.close();
        assertFalse(service.available());

        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.exists(file.resolveSibling("markers.properties.tmp")));

        DefaultPersistentMarkerService reloaded = new DefaultPersistentMarkerService();
        reloaded.bind(file);
        assertTrue(reloaded.contains(TYPE, "player", "species"));
        assertTrue(reloaded.remove(TYPE, "player", "species"));
        assertFalse(reloaded.remove(TYPE, "player", "species"));
        reloaded.close();

        DefaultPersistentMarkerService removed = new DefaultPersistentMarkerService();
        removed.bind(file);
        assertFalse(removed.contains(TYPE, "player", "species"));
        removed.close();
    }

    @Test
    void retainsAsyncFailuresAndCanRecoverOnFinalFlush() throws IOException {
        Path blockedParent = tempDirectory.resolve("blocked");
        Files.writeString(blockedParent, "not a directory");
        Path file = blockedParent.resolve("markers.properties");
        DefaultPersistentMarkerService service = new DefaultPersistentMarkerService();
        service.bind(file);
        service.markOnce(TYPE, "player", "species");
        service.flushAsync();

        assertThrows(IOException.class, service::flush);
        Files.delete(blockedParent);
        Files.createDirectories(blockedParent);
        service.flush();
        service.close();

        DefaultPersistentMarkerService reloaded = new DefaultPersistentMarkerService();
        reloaded.bind(file);
        assertTrue(reloaded.contains(TYPE, "player", "species"));
        reloaded.close();
    }

    @Test
    void refusesRebindingToAnotherWorld() throws IOException {
        DefaultPersistentMarkerService service = new DefaultPersistentMarkerService();
        service.bind(tempDirectory.resolve("first.properties"));
        assertThrows(IllegalStateException.class,
                () -> service.bind(tempDirectory.resolve("second.properties")));
        service.close();
    }

    @Test
    void rejectsMalformedValuesAndUnsupportedFormats() throws IOException {
        DefaultPersistentMarkerService service = new DefaultPersistentMarkerService();
        Path file = tempDirectory.resolve("markers.properties");
        service.bind(file);

        assertThrows(IllegalArgumentException.class, () -> service.markOnce(TYPE, " ", "value"));
        assertThrows(IllegalArgumentException.class, () -> service.markOnce(TYPE, "subject", " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.markOnce(TYPE, "subject", "x".repeat(1025)));
        service.close();

        Properties invalid = new Properties();
        invalid.setProperty("format.version", "99");
        try (var output = Files.newOutputStream(file)) {
            invalid.store(output, "invalid");
        }
        DefaultPersistentMarkerService unsupported = new DefaultPersistentMarkerService();
        assertThrows(IOException.class, () -> unsupported.bind(file));
        unsupported.close();

        Properties malformed = new Properties();
        malformed.setProperty("format.version", "1");
        malformed.setProperty("entry." + encoded(TYPE.toString()) + "." + encoded("")
                + "." + encoded("value"), "true");
        try (var output = Files.newOutputStream(file)) {
            malformed.store(output, "malformed");
        }
        DefaultPersistentMarkerService invalidEntry = new DefaultPersistentMarkerService();
        assertThrows(IOException.class, () -> invalidEntry.bind(file));
        invalidEntry.close();
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
