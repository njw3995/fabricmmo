package io.github.njw3995.fabricmmo.tools.upstreamdiff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UpstreamDiffAnalyzer {
    public List<ChangedFile> analyze(Path repository, String fromRevision, String toRevision)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder("git", "diff", "--name-status", "--find-renames",
                fromRevision, toRevision)
                .directory(repository.toFile())
                .redirectErrorStream(true)
                .start();
        List<ChangedFile> changes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                changes.add(parse(line));
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("git diff failed with exit code " + exit);
        }
        return List.copyOf(changes);
    }

    ChangedFile parse(String line) {
        String[] parts = line.split("\\t");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Unexpected git diff line: " + line);
        }
        String status = parts[0];
        String path = status.startsWith("R") && parts.length >= 3 ? parts[2] : parts[1];
        ChangedFile.ChangeType changeType = switch (status.charAt(0)) {
            case 'A' -> ChangedFile.ChangeType.ADDED;
            case 'M' -> ChangedFile.ChangeType.MODIFIED;
            case 'D' -> ChangedFile.ChangeType.DELETED;
            case 'R' -> ChangedFile.ChangeType.RENAMED;
            default -> throw new IllegalArgumentException("Unsupported git status: " + status);
        };
        return new ChangedFile(changeType, path, classify(path));
    }

    private static ChangedFile.Category classify(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.contains("/test/") || lower.endsWith("test.java")) {
            return ChangedFile.Category.TEST;
        }
        if (lower.contains("database") || lower.contains("flatfile") || lower.contains("sql")
                || lower.contains("playerprofile") || lower.contains("persistence")) {
            return ChangedFile.Category.PERSISTENCE;
        }
        if (lower.contains("bukkit") || lower.contains("paper") || lower.contains("folia")
                || lower.endsWith("plugin.yml")) {
            return ChangedFile.Category.LIKELY_BUKKIT_ONLY;
        }
        if (lower.contains("command") || lower.contains("permission")) {
            return ChangedFile.Category.COMMAND_OR_PERMISSION;
        }
        if (lower.contains("locale") || lower.endsWith(".properties")) {
            return ChangedFile.Category.LOCALE;
        }
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return ChangedFile.Category.CONFIG;
        }
        if (lower.endsWith(".java")) {
            return ChangedFile.Category.JAVA;
        }
        if (lower.startsWith("src/main/resources/")) {
            return ChangedFile.Category.RESOURCE;
        }
        return ChangedFile.Category.OTHER;
    }
}
