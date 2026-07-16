package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class MiningXpTable {
    private static final String DEFAULT_RESOURCE = "/defaults/mining-xp-1.21.1.properties";
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
                int xp;
                try {
                    xp = Integer.parseInt(trimmed.substring(separator + 1).trim());
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException(
                            "Invalid Mining XP value for " + id + " at line " + lineNumber,
                            exception);
                }
                if (xp < 0) {
                    throw new IllegalArgumentException("Mining XP must not be negative for " + id);
                }
                if (values.putIfAbsent(id, xp) != null) {
                    throw new IllegalArgumentException("Duplicate Mining XP entry for " + id);
                }
            }
        }
        return new MiningXpTable(values);
    }

    public int xpFor(NamespacedId blockId) {
        return xpByBlock.getOrDefault(blockId, 0);
    }

    public Map<NamespacedId, Integer> entries() {
        return xpByBlock;
    }
}
