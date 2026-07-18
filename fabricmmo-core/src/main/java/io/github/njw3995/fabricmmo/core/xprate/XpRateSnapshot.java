package io.github.njw3995.fabricmmo.core.xprate;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record XpRateSnapshot(
        double baselineGlobalRate,
        double globalRate,
        boolean globalEvent,
        Optional<Instant> globalStartedAt,
        Map<NamespacedId, Double> baselineSkillRates,
        Map<NamespacedId, Double> skillRates,
        Map<NamespacedId, Instant> skillStartedAt) {
    public XpRateSnapshot {
        if (!Double.isFinite(baselineGlobalRate) || baselineGlobalRate <= 0.0D
                || !Double.isFinite(globalRate) || globalRate <= 0.0D) {
            throw new IllegalArgumentException("XP rates must be finite and positive");
        }
        globalStartedAt = Objects.requireNonNull(globalStartedAt, "globalStartedAt");
        baselineSkillRates = Map.copyOf(Objects.requireNonNull(baselineSkillRates, "baselineSkillRates"));
        skillRates = Map.copyOf(Objects.requireNonNull(skillRates, "skillRates"));
        skillStartedAt = Map.copyOf(Objects.requireNonNull(skillStartedAt, "skillStartedAt"));
    }

    public double effectiveRate(NamespacedId skillId) {
        double skill = skillRates.getOrDefault(skillId, baselineSkillRates.getOrDefault(skillId, 1.0D));
        return Math.max(globalRate, skill);
    }

    public boolean active() {
        return globalRate != baselineGlobalRate || !skillRates.isEmpty();
    }
}
