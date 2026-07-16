package io.github.njw3995.fabricmmo.api.progression;

public record XpAwardResult(Status status, int appliedXp, int oldLevel, int newLevel, String detail) {
    public enum Status {
        APPLIED,
        CANCELLED,
        REJECTED
    }

    public XpAwardResult {
        if (appliedXp < 0 || oldLevel < 0 || newLevel < 0) {
            throw new IllegalArgumentException("Negative result values are invalid");
        }
        detail = detail == null ? "" : detail;
    }
}
