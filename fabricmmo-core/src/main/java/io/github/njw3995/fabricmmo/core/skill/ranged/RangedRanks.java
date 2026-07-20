package io.github.njw3995.fabricmmo.core.skill.ranged;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;

/** Shared rank-table helpers for the pinned mcMMO ranged skills. */
public final class RangedRanks {
    private RangedRanks() {
    }

    public static int rank(int level, int[] unlocks) {
        int result = 0;
        int normalized = Math.max(0, level);
        for (int index = 0; index < unlocks.length; index++) {
            if (normalized >= unlocks[index]) {
                result = index + 1;
            }
        }
        return result;
    }

    public static int rank(
            int level,
            ProgressionMode mode,
            int[] standardUnlocks,
            int[] retroUnlocks) {
        return rank(level, mode == ProgressionMode.RETRO ? retroUnlocks : standardUnlocks);
    }

    public static int[] load(
            FlatYamlConfig config,
            String skill,
            String subskill,
            ProgressionMode mode,
            int ranks) {
        String modeKey = mode == ProgressionMode.RETRO ? "RetroMode" : "Standard";
        int[] values = new int[ranks];
        for (int rank = 1; rank <= ranks; rank++) {
            values[rank - 1] = config.integer(
                    skill + "." + subskill + "." + modeKey + ".Rank_" + rank,
                    0);
        }
        return values;
    }

    public static int[] copy(int[] values, int expected, String name) {
        if (values == null || values.length != expected) {
            throw new IllegalArgumentException(name + " must contain " + expected + " values");
        }
        return values.clone();
    }
}
