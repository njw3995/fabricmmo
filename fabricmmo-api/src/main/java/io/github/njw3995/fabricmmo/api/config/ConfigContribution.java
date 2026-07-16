package io.github.njw3995.fabricmmo.api.config;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.Map;
import java.util.Objects;

public record ConfigContribution(NamespacedId owner, String fileName, Map<String, String> defaults) {
    public ConfigContribution {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isBlank() || fileName.contains("..") || fileName.startsWith("/")) {
            throw new IllegalArgumentException("Unsafe config file name: " + fileName);
        }
        defaults = Map.copyOf(Objects.requireNonNull(defaults, "defaults"));
    }
}
