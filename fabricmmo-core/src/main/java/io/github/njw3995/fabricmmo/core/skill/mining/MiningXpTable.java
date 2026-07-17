package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class MiningXpTable {
    private static final String DEFAULT_RESOURCE = "/defaults/mining-xp-1.21.1.properties";
    private static final Map<String, String> UPSTREAM_MATERIAL_ALIASES = Map.of(
            "end_bricks", "end_stone_bricks",
            "lapis_lazuli_ore", "lapis_ore",
            "deepslate_lapis_lazuli_ore", "deepslate_lapis_ore");

    private final Map<NamespacedId, Integer> xpByBlock;

    private MiningXpTable(Map<NamespacedId, Integer> xpByBlock) {
        this.xpByBlock = Collections.unmodifiableMap(new TreeMap<>(xpByBlock));
    }

    public static MiningXpTable upstreamDefaultsForMinecraft1211() {
        try (InputStream stream = MiningXpTable.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing packaged Mining XP defaults: " + DEFAULT_RESOURCE);
            }
            return load(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public static MiningXpTable loadConfigured(Path experienceFile) throws IOException {
        if (!Files.isRegularFile(experienceFile)) {
            throw new IOException("Missing FabricMMO experience configuration: " + experienceFile);
        }
        try (BufferedReader reader = Files.newBufferedReader(experienceFile, StandardCharsets.UTF_8)) {
            return loadExperienceYaml(reader);
        }
    }

    public static MiningXpTable load(InputStream stream) throws IOException {
        Map<NamespacedId, Integer> values = new TreeMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator <= 0 || separator == trimmed.length() - 1) {
                    throw new IllegalArgumentException("Invalid Mining XP entry at line " + lineNumber);
                }
                NamespacedId id = NamespacedId.parse(trimmed.substring(0, separator).trim());
                int xp = parseXp(trimmed.substring(separator + 1).trim(), id, lineNumber);
                putValue(values, id, xp, lineNumber);
            }
        }
        return new MiningXpTable(values);
    }

    static MiningXpTable loadExperienceYaml(Reader source) throws IOException {
        Map<NamespacedId, Integer> values = new TreeMap<>();
        BufferedReader reader = source instanceof BufferedReader buffered
                ? buffered
                : new BufferedReader(source);
        boolean inExperienceValues = false;
        boolean inMining = false;
        int experienceIndent = -1;
        int miningIndent = -1;
        int lineNumber = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String withoutComment = stripComment(line);
            if (withoutComment.isBlank()) {
                continue;
            }
            if (withoutComment.indexOf('\t') >= 0) {
                throw new IllegalArgumentException(
                        "Tabs are not supported in experience.yml at line " + lineNumber);
            }
            int indent = leadingSpaces(withoutComment);
            String trimmed = withoutComment.trim();

            if (!inExperienceValues) {
                if (trimmed.equals("Experience_Values:")) {
                    inExperienceValues = true;
                    experienceIndent = indent;
                }
                continue;
            }

            if (!inMining) {
                if (indent <= experienceIndent) {
                    break;
                }
                if (trimmed.equals("Mining:")) {
                    inMining = true;
                    miningIndent = indent;
                }
                continue;
            }

            if (indent <= miningIndent) {
                break;
            }
            int separator = trimmed.lastIndexOf(':');
            if (separator <= 0 || separator == trimmed.length() - 1) {
                throw new IllegalArgumentException(
                        "Invalid Mining XP mapping at line " + lineNumber);
            }
            NamespacedId id = blockIdForConfigKey(trimmed.substring(0, separator).trim());
            int xp = parseXp(trimmed.substring(separator + 1).trim(), id, lineNumber);
            putValue(values, id, xp, lineNumber);
        }

        if (!inExperienceValues) {
            throw new IllegalArgumentException("experience.yml is missing Experience_Values");
        }
        if (!inMining) {
            throw new IllegalArgumentException("experience.yml is missing Experience_Values.Mining");
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Experience_Values.Mining must contain at least one entry");
        }
        return new MiningXpTable(values);
    }

    public int xpFor(NamespacedId blockId) {
        return xpByBlock.getOrDefault(blockId, 0);
    }

    public Map<NamespacedId, Integer> entries() {
        return xpByBlock;
    }

    private static NamespacedId blockIdForConfigKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.indexOf(':') >= 0) {
            return NamespacedId.parse(normalized);
        }
        String path = UPSTREAM_MATERIAL_ALIASES.getOrDefault(normalized, normalized);
        return new NamespacedId("minecraft", path);
    }

    private static int parseXp(String value, NamespacedId id, int lineNumber) {
        int xp;
        try {
            xp = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid Mining XP value for " + id + " at line " + lineNumber,
                    exception);
        }
        if (xp < 0) {
            throw new IllegalArgumentException("Mining XP must not be negative for " + id);
        }
        return xp;
    }

    private static void putValue(
            Map<NamespacedId, Integer> values,
            NamespacedId id,
            int xp,
            int lineNumber) {
        Integer existing = values.putIfAbsent(id, xp);
        if (existing != null && existing != xp) {
            throw new IllegalArgumentException(
                    "Conflicting Mining XP entries for " + id + " at line " + lineNumber);
        }
    }

    private static String stripComment(String line) {
        int comment = line.indexOf('#');
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }
}
