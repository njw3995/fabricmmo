package io.github.njw3995.fabricmmo.core.persistence;

public record StoredSkillProgress(int level, double xp) {
    public StoredSkillProgress {
        if (level < 0 || !Double.isFinite(xp) || xp < 0.0D) {
            throw new IllegalArgumentException("Stored progression cannot be negative or non-finite");
        }
    }
}
