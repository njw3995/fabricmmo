package io.github.njw3995.fabricmmo.core.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Complete command and alias inventory from mcMMO 2.3.000's plugin.yml. */
public final class UpstreamCommandCatalog {
    private static final String RESOURCE = "/fabricmmo/upstream-commands-2.3.000.tsv";
    private static final UpstreamCommandCatalog INSTANCE = loadResource();

    private final List<UpstreamCommandDefinition> commands;
    private final Map<String, UpstreamCommandDefinition> byLiteralOrAlias;

    private UpstreamCommandCatalog(List<UpstreamCommandDefinition> commands) {
        this.commands = List.copyOf(commands);
        Map<String, UpstreamCommandDefinition> lookup = new LinkedHashMap<>();
        for (UpstreamCommandDefinition command : commands) {
            putUnique(lookup, command.literal(), command);
            for (String alias : command.aliases()) {
                putUnique(lookup, alias, command);
            }
        }
        byLiteralOrAlias = Map.copyOf(lookup);
    }

    public static UpstreamCommandCatalog instance() {
        return INSTANCE;
    }

    public List<UpstreamCommandDefinition> commands() {
        return commands;
    }

    public Optional<UpstreamCommandDefinition> find(String literalOrAlias) {
        if (literalOrAlias == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byLiteralOrAlias.get(literalOrAlias.toLowerCase(Locale.ROOT)));
    }

    private static UpstreamCommandCatalog loadResource() {
        InputStream stream = UpstreamCommandCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing FabricMMO command resource " + RESOURCE);
        }
        List<UpstreamCommandDefinition> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] columns = line.split("\\t", -1);
                if (columns.length != 3) {
                    throw new IOException("Invalid command catalog line " + lineNumber);
                }
                List<String> aliases = columns[1].isBlank()
                        ? List.of()
                        : List.of(columns[1].split(","));
                Optional<String> permission = columns[2].isBlank()
                        ? Optional.empty()
                        : Optional.of(columns[2]);
                commands.add(new UpstreamCommandDefinition(columns[0], aliases, permission));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load FabricMMO command catalog", exception);
        }
        return new UpstreamCommandCatalog(commands);
    }

    private static void putUnique(
            Map<String, UpstreamCommandDefinition> lookup,
            String literal,
            UpstreamCommandDefinition command) {
        String normalized = literal.toLowerCase(Locale.ROOT);
        UpstreamCommandDefinition previous = lookup.putIfAbsent(normalized, command);
        if (previous != null) {
            throw new IllegalStateException("Duplicate command literal or alias " + literal);
        }
    }
}
