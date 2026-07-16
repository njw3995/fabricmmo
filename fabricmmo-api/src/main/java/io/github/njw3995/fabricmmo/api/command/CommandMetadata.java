package io.github.njw3995.fabricmmo.api.command;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CommandMetadata(NamespacedId id, String literal, List<String> aliases, String permission) {
    public CommandMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(literal, "literal");
        Objects.requireNonNull(permission, "permission");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        validateLiteral(literal);
        if (permission.isBlank()) {
            throw new IllegalArgumentException("Permission must not be blank");
        }
        Set<String> normalized = new HashSet<>();
        normalized.add(literal.toLowerCase(Locale.ROOT));
        for (String alias : aliases) {
            validateLiteral(alias);
            if (!normalized.add(alias.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate command literal or alias: " + alias);
            }
        }
    }

    private static void validateLiteral(String value) {
        if (value.isBlank() || value.contains(" ") || value.contains(":")) {
            throw new IllegalArgumentException("Invalid command literal: " + value);
        }
    }
}
