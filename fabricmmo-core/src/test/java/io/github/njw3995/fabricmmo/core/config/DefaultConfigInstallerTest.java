package io.github.njw3995.fabricmmo.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultConfigInstallerTest {
    @Test
    void installsEveryRequiredDefaultWithoutOverwritingAdminFiles(@TempDir Path directory)
            throws Exception {
        Path existing = directory.resolve("config.yml");
        Files.writeString(existing, "admin-value: keep\n");

        DefaultConfigInstaller.installMissingDefaults(directory);

        assertEquals("admin-value: keep\n", Files.readString(existing));
        for (String fileName : DefaultConfigInstaller.allFiles()) {
            assertTrue(Files.isRegularFile(directory.resolve(fileName)), fileName);
        }
        assertTrue(Files.isDirectory(directory.resolve("locales")));
    }
}
