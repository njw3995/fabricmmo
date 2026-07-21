package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** The ANVIL and ITEM_BREAK entries from upstream sounds.yml. */
public record UtilityAnvilSoundSettings(Sound anvil, Sound itemBreak) {
    public UtilityAnvilSoundSettings {
        Objects.requireNonNull(anvil, "anvil");
        Objects.requireNonNull(itemBreak, "itemBreak");
    }

    public static UtilityAnvilSoundSettings load(Path soundsFile) throws IOException {
        FlatYamlConfig sounds = FlatYamlConfig.load(soundsFile);
        double master = sounds.decimal("Sounds.MasterVolume", 1.0D);
        return new UtilityAnvilSoundSettings(
                sound(sounds, master, "ANVIL", "minecraft:block.anvil.place", 1.0D, 0.3D),
                sound(sounds, master, "ITEM_BREAK", "minecraft:entity.item.break", 1.0D, 1.0D));
    }

    private static Sound sound(
            FlatYamlConfig config,
            double master,
            String key,
            String defaultId,
            double defaultVolume,
            double defaultPitch) {
        String prefix = "Sounds." + key + ".";
        String custom = unquote(config.string(prefix + "CustomSoundId", ""));
        return new Sound(
                config.bool(prefix + "Enable", true),
                custom.isBlank() ? defaultId : custom,
                Math.max(0.0D, master * config.decimal(prefix + "Volume", defaultVolume)),
                Math.max(0.0D, config.decimal(prefix + "Pitch", defaultPitch)));
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    public record Sound(boolean enabled, String id, double volume, double pitch) {
        public Sound {
            Objects.requireNonNull(id, "id");
        }
    }
}
