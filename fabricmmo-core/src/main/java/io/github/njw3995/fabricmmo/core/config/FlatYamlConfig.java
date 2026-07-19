package io.github.njw3995.fabricmmo.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal strict reader for the scalar YAML configuration used by FabricMMO defaults. */
public final class FlatYamlConfig {
    private final Map<String, String> values;

    private FlatYamlConfig(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static FlatYamlConfig load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Missing FabricMMO configuration: " + file);
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(reader, file.toString());
        }
    }

    public static FlatYamlConfig parse(Reader source, String description) throws IOException {
        BufferedReader reader = source instanceof BufferedReader buffered
                ? buffered
                : new BufferedReader(source);
        Map<String, String> values = new LinkedHashMap<>();
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
            if (trimmed.startsWith("- ")) {
                // Scalar consumers intentionally ignore YAML sequence members. Specialized
                // loaders may read those entries directly while retaining this flat map.
                continue;
            }
            int separator = trimmed.indexOf(':');
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
        return new FlatYamlConfig(values);
    }

    public Map<String, String> valuesWithPrefix(String prefix) {
        Map<String, String> matches = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                matches.put(key, value);
            }
        });
        return Collections.unmodifiableMap(matches);
    }

    public boolean contains(String path) {
        return values.containsKey(path);
    }

    public String requiredString(String path) {
        String value = values.get(path);
        if (value == null) {
            throw new IllegalArgumentException("Missing required configuration value: " + path);
        }
        return value;
    }

    public String string(String path, String fallback) {
        return values.getOrDefault(path, fallback);
    }

    public boolean requiredBoolean(String path) {
        return parseBoolean(requiredString(path), path);
    }

    public boolean bool(String path, boolean fallback) {
        String value = values.get(path);
        return value == null ? fallback : parseBoolean(value, path);
    }

    public int requiredInt(String path) {
        return parseInt(requiredString(path), path);
    }

    public int integer(String path, int fallback) {
        String value = values.get(path);
        return value == null ? fallback : parseInt(value, path);
    }

    public double requiredDouble(String path) {
        return parseDouble(requiredString(path), path);
    }

    public double decimal(String path, double fallback) {
        String value = values.get(path);
        return value == null ? fallback : parseDouble(value, path);
    }

    private static int parseInt(String value, String path) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer for " + path + ": " + value,
                    exception);
        }
    }

    private static double parseDouble(String value, String path) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal for " + path + ": " + value,
                    exception);
        }
    }

    private static boolean parseBoolean(String value, String path) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean for " + path + ": " + value);
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

    private record YamlParent(int indent, String key) {
    }
}
