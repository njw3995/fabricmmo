package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.progression.XpCurve;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact port of mcMMO 2.3.000 FormulaManager's XP-to-next-level math.
 */
public final class ProgressionFormula {
    private final XpCurve curve;
    private final Map<CacheKey, Integer> cache = new ConcurrentHashMap<>();

    public ProgressionFormula(XpCurve curve) {
        this.curve = curve;
    }

    public int xpToNextLevel(int level, ProgressionMode mode, FormulaType formulaType) {
        if (level < 0) {
            throw new IllegalArgumentException("level must be non-negative");
        }
        return cache.computeIfAbsent(new CacheKey(level, mode, formulaType), this::calculate);
    }

    public int totalExperienceAtLevel(int level, ProgressionMode mode, FormulaType formulaType) {
        if (level < 0) {
            throw new IllegalArgumentException("level must be non-negative");
        }
        long total = 0L;
        for (int current = 0; current < level; current++) {
            total += xpToNextLevel(current, mode, formulaType);
            if (total > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    private int calculate(CacheKey key) {
        if (key.mode == ProgressionMode.RETRO) {
            return calculateRetroLevel(key.level, key.formulaType);
        }
        int sum = 0;
        int retroIndex = (key.level * 10) + 1;
        for (int current = retroIndex; current < retroIndex + 10; current++) {
            sum = Math.addExact(sum, calculateRetroLevel(current, key.formulaType));
        }
        return sum;
    }

    private int calculateRetroLevel(int level, FormulaType formulaType) {
        return switch (formulaType) {
            case LINEAR -> (int) Math.floor(curve.linearBase() + level * curve.linearMultiplier());
            case EXPONENTIAL -> (int) Math.floor(
                    curve.exponentialMultiplier() * Math.pow(level, curve.exponentialExponent())
                            + curve.exponentialBase());
        };
    }

    private record CacheKey(int level, ProgressionMode mode, FormulaType formulaType) {
    }
}
