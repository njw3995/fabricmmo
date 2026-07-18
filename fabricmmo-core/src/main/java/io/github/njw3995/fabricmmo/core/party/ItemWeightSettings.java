package io.github.njw3995.fabricmmo.core.party;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Upstream itemweights.yml values used by EQUAL item sharing and MISC classification. */
public record ItemWeightSettings(int defaultWeight, Map<String, Integer> weights, Set<String> miscItems) {
    public ItemWeightSettings {
        if (defaultWeight <= 0) {
            throw new IllegalArgumentException("defaultWeight must be positive");
        }
        weights = Map.copyOf(weights);
        miscItems = Set.copyOf(miscItems);
    }

    public static ItemWeightSettings load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Missing FabricMMO item weight configuration: " + file);
        }
        int fallback = 5;
        Map<String, Integer> weights = new HashMap<>();
        Set<String> misc = new HashSet<>();
        Section section = Section.NONE;
        int sectionIndent = -1;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String noComment = stripComment(raw);
            if (noComment.isBlank()) {
                continue;
            }
            if (noComment.indexOf('\t') >= 0) {
                throw new IllegalArgumentException("Tabs are not supported in " + file);
            }
            int indent = leadingSpaces(noComment);
            String trimmed = noComment.trim();
            if (indent == 0 && trimmed.equals("Item_Weights:")) {
                section = Section.WEIGHTS;
                sectionIndent = indent;
                continue;
            }
            if (trimmed.equals("Misc_Items:")) {
                section = Section.MISC;
                sectionIndent = indent;
                continue;
            }
            if (indent <= sectionIndent && !trimmed.startsWith("-")) {
                section = Section.NONE;
            }
            if (section == Section.MISC && trimmed.startsWith("-")) {
                String value = unquote(trimmed.substring(1).trim());
                if (!value.isEmpty()) {
                    misc.add(normalize(value));
                }
                continue;
            }
            if (section != Section.WEIGHTS) {
                continue;
            }
            int separator = trimmed.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid item weight mapping in " + file + ": " + raw);
            }
            String key = normalize(trimmed.substring(0, separator));
            String value = unquote(trimmed.substring(separator + 1).trim());
            int parsed;
            try {
                parsed = Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid item weight for " + key + " in " + file, exception);
            }
            if (parsed <= 0) {
                throw new IllegalArgumentException("Item weight for " + key + " must be positive");
            }
            if (key.equals("default")) {
                fallback = parsed;
            } else {
                weights.put(key, parsed);
            }
        }
        return new ItemWeightSettings(fallback, weights, misc);
    }

    public static ItemWeightSettings upstreamDefaults() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("quartz", 200);
        weights.put("nether_quartz_ore", 200);
        weights.put("emerald", 150);
        weights.put("emerald_ore", 150);
        weights.put("diamond", 100);
        weights.put("diamond_ore", 100);
        weights.put("gold_ingot", 50);
        weights.put("gold_ore", 50);
        weights.put("iron_ingot", 40);
        weights.put("iron_ore", 40);
        weights.put("lapis_ore", 30);
        weights.put("redstone", 30);
        weights.put("redstone_ore", 30);
        weights.put("glowstone_dust", 20);
        weights.put("coal", 10);
        weights.put("coal_ore", 10);
        return new ItemWeightSettings(5, weights, Set.of());
    }

    public int weight(String itemPath) {
        return Math.max(1, weights.getOrDefault(normalize(itemPath), defaultWeight));
    }

    public boolean misc(String itemPath) {
        return miscItems.contains(normalize(itemPath));
    }

    private static String stripComment(String line) {
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '\'' && !doubleQuoted) {
                singleQuoted = !singleQuoted;
            } else if (current == '"' && !singleQuoted) {
                doubleQuoted = !doubleQuoted;
            } else if (current == '#' && !singleQuoted && !doubleQuoted) {
                return line.substring(0, index);
            }
        }
        return line;
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

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private enum Section {
        NONE,
        WEIGHTS,
        MISC
    }
}
