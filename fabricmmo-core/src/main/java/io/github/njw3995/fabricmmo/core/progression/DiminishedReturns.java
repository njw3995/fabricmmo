package io.github.njw3995.fabricmmo.core.progression;

/** Exact port of mcMMO's float-based diminished-return XP calculation. */
public final class DiminishedReturns {
    private DiminishedReturns() {
    }

    public static Result apply(
            float rawXp,
            float registeredXp,
            int threshold,
            double skillMultiplier,
            double globalMultiplier,
            float minimumFraction) {
        if (rawXp <= 0.0F || threshold <= 0) {
            return new Result(rawXp, false, false);
        }
        float guaranteedMinimum = minimumFraction * rawXp;
        float modifiedThreshold = (float) (threshold / skillMultiplier * globalMultiplier);
        float difference = (registeredXp - modifiedThreshold) / modifiedThreshold;
        if (difference <= 0.0F) {
            return new Result(rawXp, false, false);
        }
        float newValue = rawXp - rawXp * difference;
        if (guaranteedMinimum > 0.0F && newValue <= guaranteedMinimum) {
            return new Result(guaranteedMinimum, true, false);
        }
        if (newValue > 0.0F) {
            return new Result(newValue, true, false);
        }
        return new Result(0.0F, false, true);
    }

    public record Result(float xp, boolean changed, boolean cancelled) {
    }
}
