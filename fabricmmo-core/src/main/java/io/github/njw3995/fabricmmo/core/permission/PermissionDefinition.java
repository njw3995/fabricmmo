package io.github.njw3995.fabricmmo.core.permission;

import java.util.List;
import java.util.Objects;

public record PermissionDefinition(
        String node,
        PermissionDefault defaultValue,
        List<String> children) {
    public PermissionDefinition {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(defaultValue, "defaultValue");
        children = List.copyOf(Objects.requireNonNull(children, "children"));
        if (node.isBlank()) {
            throw new IllegalArgumentException("Permission node must not be blank");
        }
    }
}
