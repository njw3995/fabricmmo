package io.github.njw3995.fabricmmo.core.combat;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Config-backed mcMMO 2.3.000 mob-healthbar settings. */
public record MobHealthbarSettings(boolean enabled, DisplayType displayType, int displaySeconds) {
    public enum DisplayType {
        HEARTS,
        BAR,
        DISABLED
    }

    public MobHealthbarSettings {
        Objects.requireNonNull(displayType, "displayType");
        displaySeconds = normalizeDisplaySeconds(displaySeconds);
    }

    public static MobHealthbarSettings load(Path configFile) throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        String configured = config.string("Mob_Healthbar.Display_Type", "HEARTS")
                .trim()
                .toUpperCase(Locale.ENGLISH);
        DisplayType type;
        try {
            type = DisplayType.valueOf(configured);
        } catch (IllegalArgumentException exception) {
            type = DisplayType.HEARTS;
        }
        return new MobHealthbarSettings(
                config.bool("Mob_Healthbar.Enabled", true),
                type,
                config.integer("Mob_Healthbar.Display_Time", 3));
    }

    static int normalizeDisplaySeconds(int configured) {
        if (configured < 0) {
            return 60;
        }
        return Math.max(1, configured);
    }
}
