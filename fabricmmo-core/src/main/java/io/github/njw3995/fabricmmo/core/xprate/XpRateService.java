package io.github.njw3995.fabricmmo.core.xprate;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Thread-safe implementation of mcMMO 2.3.000 global and per-skill XP rates. */
public final class XpRateService {
    private final double baselineGlobalRate;
    private final Map<NamespacedId, Double> baselineSkillRates;
    private final Clock clock;
    private double globalRate;
    private boolean globalEvent;
    private Instant globalStartedAt;
    private final Map<NamespacedId, Double> skillRates = new HashMap<>();
    private final Map<NamespacedId, Instant> skillStartedAt = new HashMap<>();

    public XpRateService(
            double baselineGlobalRate,
            Map<NamespacedId, Double> baselineSkillRates,
            Clock clock) {
        requireRate(baselineGlobalRate, "baselineGlobalRate");
        this.baselineGlobalRate = baselineGlobalRate;
        this.baselineSkillRates = Map.copyOf(Objects.requireNonNull(baselineSkillRates, "baselineSkillRates"));
        this.baselineSkillRates.forEach((id, rate) -> requireRate(rate, "baseline skill rate for " + id));
        this.clock = Objects.requireNonNull(clock, "clock");
        this.globalRate = baselineGlobalRate;
    }

    public synchronized XpRateSnapshot snapshot() {
        return new XpRateSnapshot(
                baselineGlobalRate,
                globalRate,
                globalEvent,
                Optional.ofNullable(globalStartedAt),
                baselineSkillRates,
                skillRates,
                skillStartedAt);
    }

    public synchronized double effectiveRate(NamespacedId skillId) {
        Objects.requireNonNull(skillId, "skillId");
        double skillRate = skillRates.getOrDefault(
                skillId, baselineSkillRates.getOrDefault(skillId, 1.0D));
        return Math.max(globalRate, skillRate);
    }

    public synchronized Change setGlobal(double rate, boolean event) {
        requireRate(rate, "rate");
        if (rate < baselineGlobalRate) {
            return Change.rejected("Rate is below the configured global XP rate");
        }
        globalRate = rate;
        globalEvent = event;
        globalStartedAt = rate == baselineGlobalRate ? null : clock.instant();
        int removed = 0;
        for (NamespacedId skillId : java.util.List.copyOf(skillRates.keySet())) {
            if (skillRates.get(skillId) <= rate) {
                skillRates.remove(skillId);
                skillStartedAt.remove(skillId);
                removed++;
            }
        }
        return Change.applied(removed);
    }

    public synchronized Change setSkill(NamespacedId skillId, double rate, boolean event) {
        Objects.requireNonNull(skillId, "skillId");
        requireRate(rate, "rate");
        if (event) {
            globalEvent = true;
        }
        double baseline = baselineSkillRates.getOrDefault(skillId, 1.0D);
        if (rate < baseline) {
            return Change.rejected("Rate is below the configured skill XP rate");
        }
        if (rate == baseline) {
            skillRates.remove(skillId);
            skillStartedAt.remove(skillId);
        } else {
            skillRates.put(skillId, rate);
            skillStartedAt.put(skillId, clock.instant());
        }
        return Change.applied(0);
    }

    public synchronized void reset() {
        globalRate = baselineGlobalRate;
        globalEvent = false;
        globalStartedAt = null;
        skillRates.clear();
        skillStartedAt.clear();
    }

    public synchronized boolean clearSkill(NamespacedId skillId) {
        skillStartedAt.remove(skillId);
        return skillRates.remove(skillId) != null;
    }

    private static void requireRate(double value, String label) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(label + " must be finite and positive");
        }
    }

    public record Change(boolean applied, String detail, int clearedSkillRates) {
        public Change {
            Objects.requireNonNull(detail, "detail");
            if (clearedSkillRates < 0) {
                throw new IllegalArgumentException("clearedSkillRates must be non-negative");
            }
        }
        public static Change applied(int cleared) { return new Change(true, "", cleared); }
        public static Change rejected(String detail) { return new Change(false, detail, 0); }
    }
}
