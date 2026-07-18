package io.github.njw3995.fabricmmo.core.permission;

/** Bukkit-compatible raw permission defaults from the pinned mcMMO plugin descriptor. */
public enum PermissionDefault {
    TRUE,
    FALSE,
    OP,
    NOT_OP;

    static PermissionDefault parse(String value) {
        return switch (value) {
            case "true" -> TRUE;
            case "false" -> FALSE;
            case "op" -> OP;
            case "not_op" -> NOT_OP;
            default -> throw new IllegalArgumentException("Unknown permission default: " + value);
        };
    }

    boolean grants(boolean operator) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            case OP -> operator;
            case NOT_OP -> !operator;
        };
    }
}
