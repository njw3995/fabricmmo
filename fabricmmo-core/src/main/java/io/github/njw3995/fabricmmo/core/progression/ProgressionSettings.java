package io.github.njw3995.fabricmmo.core.progression;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.FormulaType;
import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.api.progression.XpCurve;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Config-backed mcMMO progression settings used by the XP hot path. */
public record ProgressionSettings(
        ProgressionMode mode,
        FormulaType formulaType,
        XpCurve curve,
        boolean cumulativeCurve,
        double globalXpMultiplier,
        Map<NamespacedId, Double> skillXpMultipliers,
        Map<NamespacedId, Integer> levelCaps,
        int powerLevelCap,
        boolean truncateSkills,
        boolean earlyGameBoostEnabled,
        double customXpPerkBoost,
        boolean diminishedReturnsEnabled,
        double diminishedReturnsMinimumFraction,
        Map<NamespacedId, Integer> diminishedReturnsThresholds,
        Duration diminishedReturnsInterval) {
    public ProgressionSettings {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(formulaType, "formulaType");
        Objects.requireNonNull(curve, "curve");
        requirePositiveFinite(globalXpMultiplier, "globalXpMultiplier");
        requirePositiveFinite(customXpPerkBoost, "customXpPerkBoost");
        if (!Double.isFinite(diminishedReturnsMinimumFraction)
                || diminishedReturnsMinimumFraction < 0.0D
                || diminishedReturnsMinimumFraction > 1.0D) {
            throw new IllegalArgumentException(
                    "diminishedReturnsMinimumFraction must be finite and in [0,1]");
        }
        skillXpMultipliers = Map.copyOf(Objects.requireNonNull(
                skillXpMultipliers, "skillXpMultipliers"));
        for (Map.Entry<NamespacedId, Double> entry : skillXpMultipliers.entrySet()) {
            requirePositiveFinite(entry.getValue(), "XP multiplier for " + entry.getKey());
        }
        levelCaps = Map.copyOf(Objects.requireNonNull(levelCaps, "levelCaps"));
        for (Map.Entry<NamespacedId, Integer> entry : levelCaps.entrySet()) {
            if (entry.getValue() <= 0) {
                throw new IllegalArgumentException(
                        "Level cap for " + entry.getKey() + " must be positive");
            }
        }
        if (powerLevelCap <= 0) {
            throw new IllegalArgumentException("powerLevelCap must be positive");
        }
        diminishedReturnsThresholds = Map.copyOf(Objects.requireNonNull(
                diminishedReturnsThresholds, "diminishedReturnsThresholds"));
        for (Map.Entry<NamespacedId, Integer> entry : diminishedReturnsThresholds.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException(
                        "Diminished returns threshold for " + entry.getKey()
                                + " must be non-negative");
            }
        }
        diminishedReturnsInterval = Objects.requireNonNull(
                diminishedReturnsInterval, "diminishedReturnsInterval");
        if (diminishedReturnsInterval.isNegative() || diminishedReturnsInterval.isZero()) {
            throw new IllegalArgumentException("diminishedReturnsInterval must be positive");
        }
    }

    public static ProgressionSettings load(Path configFile, Path experienceFile)
            throws IOException {
        FlatYamlConfig config = FlatYamlConfig.load(configFile);
        FlatYamlConfig experience = FlatYamlConfig.load(experienceFile);

        ProgressionMode mode = config.bool("General.RetroMode.Enabled", true)
                ? ProgressionMode.RETRO
                : ProgressionMode.STANDARD;
        FormulaType formulaType = parseFormulaType(
                experience.string("Experience_Formula.Curve", "LINEAR"));
        XpCurve curve = new XpCurve(
                experience.integer("Experience_Formula.Linear_Values.base", 1020),
                experience.decimal("Experience_Formula.Linear_Values.multiplier", 20.0D),
                experience.integer("Experience_Formula.Exponential_Values.base", 2000),
                experience.decimal("Experience_Formula.Exponential_Values.multiplier", 0.1D),
                experience.decimal("Experience_Formula.Exponential_Values.exponent", 1.80D));

        Map<NamespacedId, Integer> levelCaps = new HashMap<>();
        Map<NamespacedId, Double> skillMultipliers = new HashMap<>();
        Map<NamespacedId, Integer> diminishedThresholds = new HashMap<>();
        for (NamespacedId skillId : CoreSkills.primarySkillIds()) {
            String configName = capitalized(skillId.path());
            levelCaps.put(skillId, normalizeCap(config.integer(
                    "Skills." + configName + ".Level_Cap", 0)));
            skillMultipliers.put(skillId, experience.decimal(
                    "Experience_Formula.Skill_Multiplier." + configName, 1.0D));
            diminishedThresholds.put(skillId, experience.integer(
                    "Diminished_Returns.Threshold." + configName, 20_000));
        }

        return new ProgressionSettings(
                mode,
                formulaType,
                curve,
                experience.bool("Experience_Formula.Cumulative_Curve", false),
                experience.decimal("Experience_Formula.Multiplier.Global", 1.0D),
                skillMultipliers,
                levelCaps,
                normalizeCap(config.integer("General.Power_Level_Cap", 0)),
                config.bool("General.TruncateSkills", true),
                experience.bool("EarlyGameBoost.Enabled", true),
                experience.decimal("Experience_Formula.Custom_XP_Perk.Boost", 1.25D),
                experience.bool("Diminished_Returns.Enabled", false),
                experience.decimal(
                        "Diminished_Returns.Guaranteed_Minimum_Percentage", 0.05D),
                diminishedThresholds,
                Duration.ofMinutes(experience.integer("Diminished_Returns.Time_Interval", 10)));
    }

    public static ProgressionSettings upstreamDefaults() {
        Map<NamespacedId, Integer> caps = new HashMap<>();
        Map<NamespacedId, Double> multipliers = new HashMap<>();
        Map<NamespacedId, Integer> thresholds = new HashMap<>();
        for (NamespacedId skillId : CoreSkills.primarySkillIds()) {
            caps.put(skillId, Integer.MAX_VALUE);
            multipliers.put(skillId, 1.0D);
            thresholds.put(skillId, 20_000);
        }
        return new ProgressionSettings(
                ProgressionMode.RETRO,
                FormulaType.LINEAR,
                XpCurve.upstreamDefaults(),
                false,
                1.0D,
                multipliers,
                caps,
                Integer.MAX_VALUE,
                true,
                true,
                1.25D,
                false,
                0.05D,
                thresholds,
                Duration.ofMinutes(10));
    }

    public int levelCap(NamespacedId skillId) {
        return levelCaps.getOrDefault(skillId, Integer.MAX_VALUE);
    }

    public double skillXpMultiplier(NamespacedId skillId) {
        return skillXpMultipliers.getOrDefault(skillId, 1.0D);
    }

    public int diminishedReturnsThreshold(NamespacedId skillId) {
        return diminishedReturnsThresholds.getOrDefault(skillId, 20_000);
    }

    private static int normalizeCap(int configured) {
        return configured <= 0 ? Integer.MAX_VALUE : configured;
    }

    private static FormulaType parseFormulaType(String configured) {
        try {
            return FormulaType.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FormulaType.LINEAR;
        }
    }

    private static String capitalized(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
