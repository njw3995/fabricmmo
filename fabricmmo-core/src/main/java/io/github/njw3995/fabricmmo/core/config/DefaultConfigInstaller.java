package io.github.njw3995.fabricmmo.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DefaultConfigInstaller {
    public static final List<String> UPSTREAM_COMPATIBLE_FILES = List.of(
            "config.yml",
            "experience.yml",
            "advanced.yml",
            "skillranks.yml",
            "treasures.yml",
            "fishing_treasures.yml",
            "repair.vanilla.yml",
            "salvage.vanilla.yml",
            "sounds.yml",
            "potions.yml",
            "level_up_commands.yml");
    public static final List<String> FABRIC_ONLY_FILES = List.of("fabric.yml", "addons.yml");

    private DefaultConfigInstaller() {
    }

    public static void installMissingDefaults(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        for (String fileName : allFiles()) {
            Path target = configDirectory.resolve(fileName);
            if (Files.exists(target)) {
                continue;
            }
            String resourceName = "/defaults/" + fileName;
            try (InputStream input = DefaultConfigInstaller.class.getResourceAsStream(resourceName)) {
                if (input == null) {
                    throw new IOException("Missing packaged config default " + resourceName);
                }
                Files.copy(input, target);
            }
        }
        Files.createDirectories(configDirectory.resolve("locales"));
    }

    public static List<String> allFiles() {
        return java.util.stream.Stream.concat(UPSTREAM_COMPATIBLE_FILES.stream(),
                        FABRIC_ONLY_FILES.stream())
                .toList();
    }
}
