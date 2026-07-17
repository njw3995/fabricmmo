package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record MiningDropSettings(
        Set<NamespacedId> enabledMaterials,
        boolean silkTouchEnabled,
        boolean allowSuperBreakerTripleDrops,
        double doubleDropsChanceMaxPercent,
        int doubleDropsMaxLevelStandard,
        int doubleDropsMaxLevelRetro,
        int doubleDropsUnlockLevelStandard,
        int doubleDropsUnlockLevelRetro,
        double motherLodeChanceMaxPercent,
        int motherLodeMaxLevelStandard,
        int motherLodeMaxLevelRetro,
        int motherLodeUnlockLevelStandard,
        int motherLodeUnlockLevelRetro) {
    private static final Map<String, String> MATERIAL_ALIASES = Map.of(
            "block_of_amethyst", "amethyst_block",
            "lapis_lazuli_ore", "lapis_ore",
            "redstone_dust", "redstone");

    public MiningDropSettings {
        enabledMaterials = Collections.unmodifiableSet(new TreeSet<>(
                Objects.requireNonNull(enabledMaterials, "enabledMaterials")));
        requirePercent(doubleDropsChanceMaxPercent, "doubleDropsChanceMaxPercent");
        requirePercent(motherLodeChanceMaxPercent, "motherLodeChanceMaxPercent");
        requireNonNegative(doubleDropsMaxLevelStandard, "doubleDropsMaxLevelStandard");
        requireNonNegative(doubleDropsMaxLevelRetro, "doubleDropsMaxLevelRetro");
        requireNonNegative(doubleDropsUnlockLevelStandard, "doubleDropsUnlockLevelStandard");
        requireNonNegative(doubleDropsUnlockLevelRetro, "doubleDropsUnlockLevelRetro");
        requireNonNegative(motherLodeMaxLevelStandard, "motherLodeMaxLevelStandard");
        requireNonNegative(motherLodeMaxLevelRetro, "motherLodeMaxLevelRetro");
        requireNonNegative(motherLodeUnlockLevelStandard, "motherLodeUnlockLevelStandard");
        requireNonNegative(motherLodeUnlockLevelRetro, "motherLodeUnlockLevelRetro");
    }

    public static MiningDropSettings load(Path configFile, Path advancedFile, Path skillRanksFile)
            throws IOException {
        FlatYaml config = FlatYaml.load(configFile);
        FlatYaml advanced = FlatYaml.load(advancedFile);
        FlatYaml ranks = FlatYaml.load(skillRanksFile);

        Set<NamespacedId> materials = new TreeSet<>();
        config.valuesWithPrefix("Bonus_Drops.Mining.").forEach((key, value) -> {
            if (parseBoolean(value, key)) {
                materials.add(materialId(key.substring("Bonus_Drops.Mining.".length())));
            }
        });
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("Bonus_Drops.Mining must enable at least one material");
        }

        return new MiningDropSettings(
                materials,
                advanced.requiredBoolean("Skills.Mining.DoubleDrops.SilkTouch"),
                advanced.requiredBoolean("Skills.Mining.SuperBreaker.AllowTripleDrops"),
                advanced.requiredDouble("Skills.Mining.DoubleDrops.ChanceMax"),
                advanced.requiredInt("Skills.Mining.DoubleDrops.MaxBonusLevel.Standard"),
                advanced.requiredInt("Skills.Mining.DoubleDrops.MaxBonusLevel.RetroMode"),
                ranks.requiredInt("Mining.DoubleDrops.Standard.Rank_1"),
                ranks.requiredInt("Mining.DoubleDrops.RetroMode.Rank_1"),
                advanced.requiredDouble("Skills.Mining.MotherLode.ChanceMax"),
                advanced.requiredInt("Skills.Mining.MotherLode.MaxBonusLevel.Standard"),
                advanced.requiredInt("Skills.Mining.MotherLode.MaxBonusLevel.RetroMode"),
                ranks.requiredInt("Mining.MotherLode.Standard.Rank_1"),
                ranks.requiredInt("Mining.MotherLode.RetroMode.Rank_1"));
    }

    public static MiningDropSettings upstreamDefaults() {
        return new MiningDropSettings(
                Set.of(),
                true,
                true,
                100.0D,
                100,
                1000,
                1,
                1,
                50.0D,
                1000,
                10000,
                100,
                1000);
    }

    public boolean materialEnabled(NamespacedId materialId) {
        return enabledMaterials.contains(materialId);
    }

    public boolean doubleDropsUnlocked(int skillLevel, ProgressionMode mode) {
        return skillLevel >= modeValue(
                mode, doubleDropsUnlockLevelStandard, doubleDropsUnlockLevelRetro);
    }

    public boolean motherLodeUnlocked(int skillLevel, ProgressionMode mode) {
        return skillLevel >= modeValue(
                mode, motherLodeUnlockLevelStandard, motherLodeUnlockLevelRetro);
    }

    public int doubleDropsMaxLevel(ProgressionMode mode) {
        return modeValue(mode, doubleDropsMaxLevelStandard, doubleDropsMaxLevelRetro);
    }

    public int motherLodeMaxLevel(ProgressionMode mode) {
        return modeValue(mode, motherLodeMaxLevelStandard, motherLodeMaxLevelRetro);
    }

    private static int modeValue(ProgressionMode mode, int standard, int retro) {
        Objects.requireNonNull(mode, "mode");
        return mode == ProgressionMode.RETRO ? retro : standard;
    }

    private static NamespacedId materialId(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.indexOf(':') >= 0) {
            return NamespacedId.parse(normalized);
        }
        return new NamespacedId(
                "minecraft", MATERIAL_ALIASES.getOrDefault(normalized, normalized));
    }

    private static void requirePercent(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0D || value > 100.0D) {
            throw new IllegalArgumentException(name + " must be in [0,100]");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static boolean parseBoolean(String value, String key) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean for " + key + ": " + value);
    }

    private static final class FlatYaml {
        private final Map<String, String> values;

        private FlatYaml(Map<String, String> values) {
            this.values = Map.copyOf(values);
        }

        static FlatYaml load(Path file) throws IOException {
            if (!Files.isRegularFile(file)) {
                throw new IOException("Missing FabricMMO configuration: " + file);
            }
            try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                return parse(reader, file.toString());
            }
        }

        static FlatYaml parse(Reader source, String description) throws IOException {
            BufferedReader reader = source instanceof BufferedReader buffered
                    ? buffered
                    : new BufferedReader(source);
            Map<String, String> values = new HashMap<>();
            Deque<YamlParent> parents = new ArrayDeque<>();
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String withoutComment = stripComment(line);
                if (withoutComment.isBlank()) {
                    continue;
                }
                if (withoutComment.indexOf('\t') >= 0) {
                    throw new IllegalArgumentException(
                            "Tabs are not supported in " + description + " at line " + lineNumber);
                }
                int indent = leadingSpaces(withoutComment);
                String trimmed = withoutComment.trim();
                int separator = trimmed.lastIndexOf(':');
                if (separator <= 0) {
                    throw new IllegalArgumentException(
                            "Invalid YAML mapping in " + description + " at line " + lineNumber);
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                while (!parents.isEmpty() && indent <= parents.peekLast().indent()) {
                    parents.removeLast();
                }
                String path = path(parents, key);
                if (value.isEmpty()) {
                    parents.addLast(new YamlParent(indent, key));
                } else {
                    String parsedValue = unquote(value);
                    String existing = values.putIfAbsent(path, parsedValue);
                    if (existing != null && !existing.equals(parsedValue)) {
                        throw new IllegalArgumentException(
                                "Duplicate YAML value for " + path + " in " + description);
                    }
                }
            }
            return new FlatYaml(values);
        }

        Map<String, String> valuesWithPrefix(String prefix) {
            Map<String, String> matches = new HashMap<>();
            values.forEach((key, value) -> {
                if (key.startsWith(prefix)) {
                    matches.put(key, value);
                }
            });
            return matches;
        }

        boolean requiredBoolean(String path) {
            return parseBoolean(required(path), path);
        }

        int requiredInt(String path) {
            String value = required(path);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid integer for " + path + ": " + value,
                        exception);
            }
        }

        double requiredDouble(String path) {
            String value = required(path);
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid decimal for " + path + ": " + value,
                        exception);
            }
        }

        private String required(String path) {
            String value = values.get(path);
            if (value == null) {
                throw new IllegalArgumentException("Missing required configuration value: " + path);
            }
            return value;
        }

        private static String path(Deque<YamlParent> parents, String key) {
            StringBuilder builder = new StringBuilder();
            for (YamlParent parent : parents) {
                if (!builder.isEmpty()) {
                    builder.append('.');
                }
                builder.append(parent.key());
            }
            if (!builder.isEmpty()) {
                builder.append('.');
            }
            return builder.append(key).toString();
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

        private static String unquote(String value) {
            if (value.length() >= 2) {
                char first = value.charAt(0);
                char last = value.charAt(value.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    return value.substring(1, value.length() - 1);
                }
            }
            return value;
        }
    }

    private record YamlParent(int indent, String key) {
    }
}
