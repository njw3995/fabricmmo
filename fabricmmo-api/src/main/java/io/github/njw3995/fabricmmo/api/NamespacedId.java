package io.github.njw3995.fabricmmo.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** A loader-independent namespaced identifier used by the public API. */
public record NamespacedId(String namespace, String path) implements Comparable<NamespacedId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    public NamespacedId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    public static NamespacedId parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Expected namespace:path, got: " + value);
        }
        return new NamespacedId(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public int compareTo(NamespacedId other) {
        int namespaceOrder = namespace.compareTo(other.namespace);
        return namespaceOrder != 0 ? namespaceOrder : path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }
}
