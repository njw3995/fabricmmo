package io.github.njw3995.fabricmmo.api.config;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/** Core-owned materialization and reads for registered addon configuration defaults. */
public interface ConfigService {
    boolean available();

    Optional<String> value(NamespacedId owner, String fileName, String key);

    Map<String, String> values(NamespacedId owner, String fileName);

    void reload() throws IOException;

    default boolean booleanValue(
            NamespacedId owner, String fileName, String key, boolean fallback) {
        String value = value(owner, fileName, key).orElse(null);
        if (value == null) return fallback;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException("Invalid boolean for " + owner + '/' + fileName
                + " key " + key + ": " + value);
    }

    default int intValue(NamespacedId owner, String fileName, String key, int fallback) {
        String value = value(owner, fileName, key).orElse(null);
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer for " + owner + '/' + fileName
                    + " key " + key + ": " + value, exception);
        }
    }

    default double doubleValue(
            NamespacedId owner, String fileName, String key, double fallback) {
        String value = value(owner, fileName, key).orElse(null);
        if (value == null) return fallback;
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed)) throw new NumberFormatException("non-finite");
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal for " + owner + '/' + fileName
                    + " key " + key + ": " + value, exception);
        }
    }

    static ConfigService unsupported() {
        return new ConfigService() {
            @Override
            public boolean available() {
                return false;
            }

            @Override
            public Optional<String> value(NamespacedId owner, String fileName, String key) {
                return Optional.empty();
            }

            @Override
            public Map<String, String> values(NamespacedId owner, String fileName) {
                return Map.of();
            }

            @Override
            public void reload() {
                throw new UnsupportedOperationException(
                        "This FabricMMO API implementation does not provide addon config files");
            }
        };
    }
}
