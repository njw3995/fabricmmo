package io.github.njw3995.fabricmmo.core.skill.repair;

/** Pure per-enchantment Arcane Forging outcome from mcMMO 2.3.000. */
public final class ArcaneForgingFormula {
    public enum Outcome {
        KEPT,
        DOWNGRADED,
        LOST
    }

    public record Result(Outcome outcome, int level) {
        public Result {
            if (level < 0 || (outcome == Outcome.LOST && level != 0)
                    || (outcome != Outcome.LOST && level == 0)) {
                throw new IllegalArgumentException("Invalid Arcane Forging result");
            }
        }
    }

    private ArcaneForgingFormula() {
    }

    public static Result resolve(
            int originalLevel,
            int vanillaMaximumLevel,
            int configuredMaximumLevel,
            boolean unsafeEnchantments,
            boolean bypass,
            boolean enchantLossEnabled,
            int arcaneRank,
            boolean arcanePermission,
            boolean keepRoll,
            boolean downgradesEnabled,
            boolean avoidDowngradeRoll) {
        if (originalLevel <= 0 || vanillaMaximumLevel <= 0 || configuredMaximumLevel <= 0
                || arcaneRank < 0) {
            throw new IllegalArgumentException("Invalid Arcane Forging input");
        }
        if (!enchantLossEnabled || bypass) {
            return new Result(Outcome.KEPT, originalLevel);
        }
        if (arcaneRank == 0 || !arcanePermission) {
            return new Result(Outcome.LOST, 0);
        }
        int level = unsafeEnchantments
                ? originalLevel
                : Math.min(originalLevel, configuredMaximumLevel);
        // Upstream writes an over-vanilla clamped level before rolling. A successful perfect keep
        // therefore retains that value, even when it remains above the vanilla maximum.
        if (!keepRoll) {
            return new Result(Outcome.LOST, 0);
        }
        if (downgradesEnabled && level > 1 && !avoidDowngradeRoll) {
            return new Result(Outcome.DOWNGRADED, level - 1);
        }
        return new Result(Outcome.KEPT, level);
    }
}
