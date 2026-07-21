package io.github.njw3995.fabricmmo.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.config.ConfigContribution;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultConfigRegistryServiceTest {
    private static final NamespacedId OWNER = NamespacedId.parse("test:addon");

    @TempDir
    Path temporaryDirectory;

    @Test
    void materializesDefaultsPreservesAdminValuesAndAddsNewKeys() throws Exception {
        DefaultConfigRegistry registry = new DefaultConfigRegistry();
        registry.registerConfig(new ConfigContribution(
                OWNER, "settings.properties", Map.of("Enabled", "true", "Xp", "25")));
        registry.freeze();
        registry.bind(temporaryDirectory);

        assertTrue(registry.available());
        assertTrue(registry.booleanValue(OWNER, "settings.properties", "Enabled", false));
        assertEquals(25, registry.intValue(OWNER, "settings.properties", "Xp", 0));

        Path file = temporaryDirectory.resolve("test/addon/settings.properties");
        Files.writeString(file, "\nEnabled=false\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        registry.reload();
        assertFalse(registry.booleanValue(OWNER, "settings.properties", "Enabled", true));
        assertEquals(25, registry.intValue(OWNER, "settings.properties", "Xp", 0));
    }

    @Test
    void invalidTypedValuesAreReportedInsteadOfReset() throws Exception {
        DefaultConfigRegistry registry = new DefaultConfigRegistry();
        registry.registerConfig(new ConfigContribution(
                OWNER, "settings.properties", Map.of("Xp", "25")));
        registry.freeze();
        registry.bind(temporaryDirectory);
        Path file = temporaryDirectory.resolve("test/addon/settings.properties");
        Files.writeString(file, "\nXp=not-a-number\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND);
        registry.reload();

        assertThrows(IllegalArgumentException.class,
                () -> registry.intValue(OWNER, "settings.properties", "Xp", 0));
        assertEquals("not-a-number", registry.value(
                OWNER, "settings.properties", "Xp").orElseThrow());
    }
}
