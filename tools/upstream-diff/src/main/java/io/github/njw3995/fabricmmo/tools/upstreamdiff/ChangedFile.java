package io.github.njw3995.fabricmmo.tools.upstreamdiff;

public record ChangedFile(ChangeType changeType, String path, Category category) {
    public enum ChangeType { ADDED, MODIFIED, DELETED, RENAMED }
    public enum Category {
        JAVA,
        CONFIG,
        LOCALE,
        COMMAND_OR_PERMISSION,
        PERSISTENCE,
        TEST,
        RESOURCE,
        LIKELY_BUKKIT_ONLY,
        OTHER
    }
}
