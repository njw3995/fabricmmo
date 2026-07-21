package io.github.njw3995.fabricmmo.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MobHealthbarSettingsTest {
    @Test
    void loadsPinnedUpstreamDefaults() throws Exception {
        MobHealthbarSettings settings = MobHealthbarSettings.load(
                Path.of("src/main/resources/defaults/config.yml"));

        assertTrue(settings.enabled());
        assertEquals(MobHealthbarSettings.DisplayType.HEARTS, settings.displayType());
        assertEquals(3, settings.displaySeconds());
    }

    @Test
    void invalidDisplayFallsBackAndDisplayTimeMatchesUpstreamNormalization(
            @TempDir Path directory) throws IOException {
        Path config = directory.resolve("config.yml");
        Files.writeString(config, """
                Mob_Healthbar:
                  Enabled: true
                  Display_Type: not-a-mode
                  Display_Time: -1
                """);

        MobHealthbarSettings settings = MobHealthbarSettings.load(config);
        assertEquals(MobHealthbarSettings.DisplayType.HEARTS, settings.displayType());
        assertEquals(60, settings.displaySeconds());
        assertEquals(1, MobHealthbarSettings.normalizeDisplaySeconds(0));
    }
}
