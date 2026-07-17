package io.github.njw3995.fabricmmo.core.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlacedBlockSettingsTest {
    @Test
    void defaultsRegionSystemOnAndReadsUpstreamSwitch(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("persistent_data.yml");
        Files.writeString(file, "Unrelated:\n  Value: true\n");
        assertTrue(PlacedBlockSettings.load(file).regionSystemEnabled());

        Files.writeString(file, "mcMMO_Region_System:\n  Enabled: false\n");
        assertFalse(PlacedBlockSettings.load(file).regionSystemEnabled());
    }
}
