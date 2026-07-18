package io.github.njw3995.fabricmmo.core.command;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Command declaration copied from the pinned mcMMO 2.3.000 plugin descriptor. */
public record UpstreamCommandDefinition(
        String literal,
        List<String> aliases,
        Optional<String> permission) {
    public UpstreamCommandDefinition {
        Objects.requireNonNull(literal, "literal");
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        permission = Objects.requireNonNull(permission, "permission");
        if (literal.isBlank()) {
            throw new IllegalArgumentException("Command literal must not be blank");
        }
    }
}
