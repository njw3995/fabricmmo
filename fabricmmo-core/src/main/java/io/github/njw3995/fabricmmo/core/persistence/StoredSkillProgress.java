package io.github.njw3995.fabricmmo.core.persistence;

public record StoredSkillProgress(int level, int xp) {
    public StoredSkillProgress {
        if (level < 0 || xp < 0) {
            throw new IllegalArgumentException("Stored progression cannot be negative");
        }
    }
}
