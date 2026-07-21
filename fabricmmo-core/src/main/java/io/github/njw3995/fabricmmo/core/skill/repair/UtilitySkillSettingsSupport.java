package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.util.Objects;

final class UtilitySkillSettingsSupport {
    private UtilitySkillSettingsSupport() {
    }

    static int[] ranks(FlatYamlConfig yaml, String prefix, int count, int[] defaults) {
        int[] result = new int[count];
        for (int index = 0; index < count; index++) {
            result[index] = yaml.integer(prefix + (index + 1), defaults[index]);
        }
        return ordered(result, prefix);
    }

    static double[] decimals(FlatYamlConfig yaml, String prefix, int count, double[] defaults) {
        double[] result = new double[count];
        for (int index = 0; index < count; index++) {
            result[index] = yaml.decimal(prefix + (index + 1), defaults[index]);
        }
        return copy(result, count, prefix);
    }

    static int rank(int level, int[] thresholds) {
        int rank = 0;
        for (int index = 0; index < thresholds.length; index++) {
            if (level >= thresholds[index]) {
                rank = index + 1;
            }
        }
        return rank;
    }

    static int[] mode(ProgressionMode mode, int[] standard, int[] retro) {
        return mode == ProgressionMode.RETRO ? retro : standard;
    }

    static int mode(ProgressionMode mode, int standard, int retro) {
        return mode == ProgressionMode.RETRO ? retro : standard;
    }

    static double lucky(double base, boolean lucky) {
        return Math.min(100.0D, lucky ? base * 1.333D : base);
    }

    static int[] ordered(int[] values, String name) {
        int[] result = Objects.requireNonNull(values, name).clone();
        int previous = -1;
        for (int value : result) {
            if (value < 0 || value < previous) {
                throw new IllegalArgumentException(name + " must be non-negative and ordered");
            }
            previous = value;
        }
        return result;
    }

    static double[] copy(double[] values, int count, String name) {
        Objects.requireNonNull(values, name);
        if (values.length != count) {
            throw new IllegalArgumentException(name + " must contain " + count + " values");
        }
        double[] result = values.clone();
        for (double value : result) {
            if (!Double.isFinite(value) || value < 0.0D) {
                throw new IllegalArgumentException(name + " values must be finite and non-negative");
            }
        }
        return result;
    }
}
