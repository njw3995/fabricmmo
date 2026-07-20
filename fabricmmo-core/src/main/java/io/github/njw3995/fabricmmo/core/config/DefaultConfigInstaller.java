package io.github.njw3995.fabricmmo.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaultConfigInstaller {
    private static final Pattern TOP_LEVEL_KEY = Pattern.compile("^([A-Za-z0-9_.-]+):(?:\\s.*)?$");
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
            "level_up_commands.yml",
            "persistent_data.yml",
            "custom_item_support.yml",
            "chat.yml",
            "party.yml",
            "itemweights.yml",
            "world_blacklist.txt");
    public static final List<String> FABRIC_ONLY_FILES = List.of("fabric.yml", "addons.yml");
    private static final Set<String> EMPTY_PLACEHOLDER_FILES = Set.of(
            "fishing_treasures.yml",
            "repair.vanilla.yml",
            "salvage.vanilla.yml",
            "sounds.yml");
    private static final Set<String> RECURSIVE_MERGE_FILES = Set.of(
            "config.yml",
            "experience.yml",
            "advanced.yml",
            "skillranks.yml",
            "custom_item_support.yml",
            "fabric.yml",
            "addons.yml");

    private DefaultConfigInstaller() {
    }

    public static void installMissingDefaults(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        for (String fileName : allFiles()) {
            Path target = configDirectory.resolve(fileName);
            String resourceName = "/defaults/" + fileName;
            if (Files.exists(target)) {
                if (isEmptyPackagedPlaceholder(fileName, target)) {
                    replacePlaceholderWithDefault(target, resourceName);
                } else if (RECURSIVE_MERGE_FILES.contains(fileName)) {
                    mergeMissingYamlDefaults(target, resourceName);
                } else if (fileName.endsWith(".yml")) {
                    appendMissingTopLevelSections(target, resourceName);
                }
                continue;
            }
            try (InputStream input = DefaultConfigInstaller.class.getResourceAsStream(resourceName)) {
                if (input == null) {
                    throw new IOException("Missing packaged config default " + resourceName);
                }
                Files.copy(input, target);
            }
        }
        Files.createDirectories(configDirectory.resolve("locales"));
    }


    private static boolean isEmptyPackagedPlaceholder(String fileName, Path target)
            throws IOException {
        return EMPTY_PLACEHOLDER_FILES.contains(fileName)
                && Files.readString(target, StandardCharsets.UTF_8).trim().equals("{}");
    }

    private static void replacePlaceholderWithDefault(Path target, String resourceName)
            throws IOException {
        Path backup = target.resolveSibling(target.getFileName() + ".pre-update.bak");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
        try (InputStream input = DefaultConfigInstaller.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing packaged config default " + resourceName);
            }
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void mergeMissingYamlDefaults(Path target, String resourceName)
            throws IOException {
        List<String> existing = Files.readAllLines(target, StandardCharsets.UTF_8);
        List<String> defaults;
        try (InputStream input = DefaultConfigInstaller.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing packaged config default " + resourceName);
            }
            defaults = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
        YamlDefaultsMerger.MergeResult merged = YamlDefaultsMerger.merge(existing, defaults);
        if (!merged.changed()) {
            return;
        }
        Path backup = target.resolveSibling(target.getFileName() + ".pre-update.bak");
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
        String content = String.join(System.lineSeparator(), merged.lines())
                + System.lineSeparator();
        Files.writeString(target, content, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static void appendMissingTopLevelSections(Path target, String resourceName)
            throws IOException {
        String existing = Files.readString(target, StandardCharsets.UTF_8);
        Set<String> existingKeys = topLevelKeys(existing.lines().toList());
        List<String> defaults;
        try (InputStream input = DefaultConfigInstaller.class.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing packaged config default " + resourceName);
            }
            defaults = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }

        StringBuilder additions = new StringBuilder();
        for (TopLevelBlock block : topLevelBlocks(defaults)) {
            if (existingKeys.add(block.key())) {
                if (!additions.isEmpty()) {
                    additions.append(System.lineSeparator());
                }
                additions.append(String.join(System.lineSeparator(), block.lines()));
                additions.append(System.lineSeparator());
            }
        }
        if (additions.isEmpty()) {
            return;
        }
        String prefix;
        if (existing.isEmpty()) {
            prefix = "";
        } else if (existing.endsWith("\n") || existing.endsWith("\r")) {
            prefix = System.lineSeparator();
        } else {
            prefix = System.lineSeparator() + System.lineSeparator();
        }
        Files.writeString(target, prefix + additions,
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static Set<String> topLevelKeys(List<String> lines) {
        Set<String> keys = new HashSet<>();
        for (String line : lines) {
            Matcher matcher = TOP_LEVEL_KEY.matcher(line);
            if (matcher.matches()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    private static List<TopLevelBlock> topLevelBlocks(List<String> lines) {
        List<TopLevelBlock> blocks = new ArrayList<>();
        String key = null;
        List<String> blockLines = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = TOP_LEVEL_KEY.matcher(line);
            if (matcher.matches()) {
                if (key != null) {
                    blocks.add(new TopLevelBlock(key, List.copyOf(blockLines)));
                }
                key = matcher.group(1);
                blockLines = new ArrayList<>();
            }
            if (key != null) {
                blockLines.add(line);
            }
        }
        if (key != null) {
            blocks.add(new TopLevelBlock(key, List.copyOf(blockLines)));
        }
        return blocks;
    }

    private record TopLevelBlock(String key, List<String> lines) {
    }

    public static List<String> allFiles() {
        return java.util.stream.Stream.concat(UPSTREAM_COMPATIBLE_FILES.stream(),
                        FABRIC_ONLY_FILES.stream())
                .toList();
    }
}
