package io.github.njw3995.fabricmmo.core.skill.repair;

/** Pure per-enchantment Arcane Salvage extraction outcome from mcMMO 2.3.000. */
public final class ArcaneSalvageFormula {
    public enum Outcome {
        FULL,
        PARTIAL,
        LOST
    }

    public record Result(Outcome outcome, int level) {
        public Result {
            if (level < 0 || (outcome == Outcome.LOST && level != 0)
                    || (outcome != Outcome.LOST && level == 0)) {
                throw new IllegalArgumentException("Invalid Arcane Salvage result");
            }
        }
    }

    private ArcaneSalvageFormula() {
    }

    public static Result resolve(
            int originalLevel,
            int configuredMaximumLevel,
            boolean unsafeEnchantments,
            boolean lossEnabled,
            boolean downgradeEnabled,
            boolean bypass,
            boolean fullRoll,
            boolean partialRoll) {
        if (originalLevel <= 0 || configuredMaximumLevel <= 0) {
            throw new IllegalArgumentException("Invalid Arcane Salvage input");
        }
        int level = unsafeEnchantments
                ? originalLevel
                : Math.min(originalLevel, configuredMaximumLevel);
        if (!lossEnabled || bypass || fullRoll) {
            return new Result(Outcome.FULL, level);
        }
        if (level > 1 && downgradeEnabled && partialRoll) {
            return new Result(Outcome.PARTIAL, level - 1);
        }
        return new Result(Outcome.LOST, 0);
    }
}
