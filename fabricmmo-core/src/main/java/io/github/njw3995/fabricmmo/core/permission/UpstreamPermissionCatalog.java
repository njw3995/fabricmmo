package io.github.njw3995.fabricmmo.core.permission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Complete permission graph from mcMMO 2.3.000's plugin.yml.
 *
 * <p>Fabric has no plugin.yml permission registration phase, so this catalog preserves Bukkit's
 * parent/child implications and default grants while the actual decision still comes from the
 * Fabric Permissions API when a provider supplies an explicit value.</p>
 */
public final class UpstreamPermissionCatalog {
    private static final String RESOURCE = "/fabricmmo/upstream-permissions-2.3.000.tsv";
    private static final UpstreamPermissionCatalog INSTANCE = loadResource();

    private final Map<String, PermissionDefinition> definitions;
    private final Map<String, List<String>> ancestors;

    private UpstreamPermissionCatalog(Map<String, PermissionDefinition> definitions) {
        this.definitions = Map.copyOf(new TreeMap<>(definitions));
        this.ancestors = buildAncestors(this.definitions.values());
    }

    public static UpstreamPermissionCatalog instance() {
        return INSTANCE;
    }

    public Optional<PermissionDefinition> find(String node) {
        return Optional.ofNullable(definitions.get(node));
    }

    public List<PermissionDefinition> definitions() {
        return List.copyOf(definitions.values());
    }

    /** Returns transitive parents ordered from most-specific to least-specific. */
    public List<String> ancestors(String node) {
        return ancestors.getOrDefault(node, List.of());
    }

    public boolean effectiveDefault(String node, boolean operator) {
        PermissionDefinition direct = definitions.get(node);
        if (direct == null) {
            return false;
        }
        if (direct.defaultValue().grants(operator)) {
            return true;
        }
        for (String ancestor : ancestors(node)) {
            PermissionDefinition parent = definitions.get(ancestor);
            if (parent != null && parent.defaultValue().grants(operator)) {
                return true;
            }
        }
        return false;
    }

    private static UpstreamPermissionCatalog loadResource() {
        InputStream stream = UpstreamPermissionCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing FabricMMO permission resource " + RESOURCE);
        }
        Map<String, PermissionDefinition> definitions = new TreeMap<>();
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
                    throw new IOException("Invalid permission catalog line " + lineNumber);
                }
                List<String> children = columns[2].isBlank()
                        ? List.of()
                        : List.of(columns[2].split(","));
                PermissionDefinition definition = new PermissionDefinition(
                        columns[0], PermissionDefault.parse(columns[1]), children);
                if (definitions.putIfAbsent(definition.node(), definition) != null) {
                    throw new IOException("Duplicate permission node " + definition.node());
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load FabricMMO permission catalog", exception);
        }
        return new UpstreamPermissionCatalog(definitions);
    }

    private static Map<String, List<String>> buildAncestors(
            Collection<PermissionDefinition> definitions) {
        Map<String, Set<String>> directParents = new HashMap<>();
        for (PermissionDefinition definition : definitions) {
            for (String child : definition.children()) {
                directParents.computeIfAbsent(child, ignored -> new LinkedHashSet<>())
                        .add(definition.node());
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        Set<String> allNodes = new HashSet<>();
        definitions.forEach(definition -> {
            allNodes.add(definition.node());
            allNodes.addAll(definition.children());
        });
        for (String node : allNodes) {
            LinkedHashSet<String> found = new LinkedHashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>(
                    directParents.getOrDefault(node, Set.of()));
            while (!queue.isEmpty()) {
                String parent = queue.removeFirst();
                if (!found.add(parent)) {
                    continue;
                }
                queue.addAll(directParents.getOrDefault(parent, Set.of()));
            }
            ArrayList<String> ordered = new ArrayList<>(found);
            ordered.sort(Comparator
                    .comparingInt(UpstreamPermissionCatalog::specificity)
                    .reversed()
                    .thenComparing(Comparator.naturalOrder()));
            result.put(node, List.copyOf(ordered));
        }
        return Map.copyOf(result);
    }

    private static int specificity(String node) {
        Objects.requireNonNull(node, "node");
        int parts = 1;
        for (int i = 0; i < node.length(); i++) {
            if (node.charAt(i) == '.') {
                parts++;
            }
        }
        return parts;
    }
}
