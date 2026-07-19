package io.github.njw3995.fabricmmo.core.config;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comment-preserving line merger for the mapping-oriented YAML used by FabricMMO configuration.
 * Existing values and custom keys are never rewritten; only missing default mapping entries are
 * inserted.
 */
final class YamlDefaultsMerger {
    private static final Pattern KEY = Pattern.compile("^( *)([A-Za-z0-9_.-]+):(?:\\s*(.*))?$");

    private YamlDefaultsMerger() {
    }

    static MergeResult merge(List<String> existingLines, List<String> defaultLines)
            throws IOException {
        Objects.requireNonNull(existingLines, "existingLines");
        Objects.requireNonNull(defaultLines, "defaultLines");

        ParsedYaml existing = parse(existingLines, "existing configuration");
        ParsedYaml defaults = parse(defaultLines, "packaged defaults");
        Map<String, List<Node>> missingByParent = new LinkedHashMap<>();

        for (Node defaultNode : defaults.nodesInSourceOrder()) {
            if (existing.byPath().containsKey(defaultNode.path())) {
                continue;
            }
            String parentPath = defaultNode.parentPath();
            if (parentPath != null && !existing.byPath().containsKey(parentPath)) {
                continue;
            }
            missingByParent.computeIfAbsent(parentPath, ignored -> new ArrayList<>())
                    .add(defaultNode);
        }

        if (missingByParent.isEmpty()) {
            return new MergeResult(List.copyOf(existingLines), false);
        }

        ArrayList<Insertion> insertions = new ArrayList<>();
        for (Map.Entry<String, List<Node>> entry : missingByParent.entrySet()) {
            String parentPath = entry.getKey();
            int insertionIndex;
            int indentShift;
            if (parentPath == null) {
                insertionIndex = existingLines.size();
                indentShift = 0;
            } else {
                Node existingParent = existing.byPath().get(parentPath);
                Node defaultParent = defaults.byPath().get(parentPath);
                if (existingParent == null || defaultParent == null) {
                    throw new IOException("Unable to resolve YAML merge parent " + parentPath);
                }
                if (!existingParent.section()) {
                    throw new IOException("Cannot add missing defaults below scalar YAML key "
                            + parentPath);
                }
                insertionIndex = existingParent.endLineExclusive();
                indentShift = existingParent.indent() - defaultParent.indent();
            }

            ArrayList<String> lines = new ArrayList<>();
            for (Node node : entry.getValue()) {
                for (String line : defaultLines.subList(node.startLine(), node.endLineExclusive())) {
                    lines.add(shiftIndent(line, indentShift));
                }
            }
            int parentDepth = parentPath == null ? 0 : parentPath.split("\\.").length;
            insertions.add(new Insertion(insertionIndex, parentDepth, List.copyOf(lines)));
        }

        insertions.sort((left, right) -> {
            int byIndex = Integer.compare(right.index(), left.index());
            return byIndex != 0 ? byIndex : Integer.compare(left.parentDepth(), right.parentDepth());
        });
        ArrayList<String> merged = new ArrayList<>(existingLines);
        for (Insertion insertion : insertions) {
            List<String> lines = insertion.lines();
            if (insertion.index() == merged.size()
                    && !merged.isEmpty()
                    && !merged.get(merged.size() - 1).isBlank()
                    && !lines.isEmpty()
                    && !lines.get(0).isBlank()) {
                merged.add("");
            }
            merged.addAll(insertion.index(), lines);
        }
        return new MergeResult(List.copyOf(merged), true);
    }

    private static ParsedYaml parse(List<String> lines, String description) throws IOException {
        ArrayList<NodeBuilder> builders = new ArrayList<>();
        Deque<NodeBuilder> parents = new ArrayDeque<>();
        Map<String, NodeBuilder> byPath = new HashMap<>();

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.indexOf('\t') >= 0) {
                throw new IOException("Tabs are not supported in " + description
                        + " at line " + (index + 1));
            }
            Matcher matcher = KEY.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            int indent = matcher.group(1).length();
            while (!parents.isEmpty() && parents.peekLast().indent >= indent) {
                parents.removeLast();
            }
            NodeBuilder parent = parents.peekLast();
            String key = matcher.group(2);
            String path = parent == null ? key : parent.path + "." + key;
            String value = matcher.group(3);
            boolean section = value == null || value.isBlank() || value.startsWith("#");
            NodeBuilder builder = new NodeBuilder(
                    path,
                    parent == null ? null : parent.path,
                    indent,
                    index,
                    lines.size(),
                    section);
            if (byPath.putIfAbsent(path, builder) != null) {
                throw new IOException("Duplicate YAML key " + path + " in " + description);
            }
            builders.add(builder);
            if (section) {
                parents.addLast(builder);
            }
        }
        for (int current = 0; current < builders.size(); current++) {
            NodeBuilder builder = builders.get(current);
            int endLineExclusive = lines.size();
            for (int next = current + 1; next < builders.size(); next++) {
                NodeBuilder candidate = builders.get(next);
                if (candidate.indent <= builder.indent) {
                    endLineExclusive = candidate.startLine;
                    break;
                }
            }
            builder.endLineExclusive = endLineExclusive;
        }

        ArrayList<Node> nodes = new ArrayList<>(builders.size());
        Map<String, Node> immutableByPath = new HashMap<>();
        for (NodeBuilder builder : builders) {
            Node node = builder.build();
            nodes.add(node);
            immutableByPath.put(node.path(), node);
        }
        return new ParsedYaml(List.copyOf(nodes), Map.copyOf(immutableByPath));
    }

    private static String shiftIndent(String line, int shift) throws IOException {
        if (line.isBlank() || shift == 0) {
            return line;
        }
        int current = 0;
        while (current < line.length() && line.charAt(current) == ' ') {
            current++;
        }
        int shifted = current + shift;
        if (shifted < 0) {
            throw new IOException("Cannot merge YAML defaults with incompatible indentation");
        }
        return " ".repeat(shifted) + line.substring(current);
    }

    record MergeResult(List<String> lines, boolean changed) {
        MergeResult {
            lines = List.copyOf(lines);
        }
    }

    private record ParsedYaml(List<Node> nodesInSourceOrder, Map<String, Node> byPath) {
    }

    private record Node(
            String path,
            String parentPath,
            int indent,
            int startLine,
            int endLineExclusive,
            boolean section) {
    }

    private record Insertion(int index, int parentDepth, List<String> lines) {
    }

    private static final class NodeBuilder {
        private final String path;
        private final String parentPath;
        private final int indent;
        private final int startLine;
        private int endLineExclusive;
        private final boolean section;

        private NodeBuilder(
                String path,
                String parentPath,
                int indent,
                int startLine,
                int endLineExclusive,
                boolean section) {
            this.path = path;
            this.parentPath = parentPath;
            this.indent = indent;
            this.startLine = startLine;
            this.endLineExclusive = endLineExclusive;
            this.section = section;
        }

        private Node build() {
            return new Node(path, parentPath, indent, startLine, endLineExclusive, section);
        }
    }
}
