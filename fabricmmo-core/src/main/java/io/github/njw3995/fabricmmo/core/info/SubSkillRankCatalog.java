package io.github.njw3995.fabricmmo.core.info;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Exact mcMMO-style subskill rank lookup backed by skillranks.yml. */
public final class SubSkillRankCatalog {
    private final FlatYamlConfig ranks;
    private final String modeKey;

    private SubSkillRankCatalog(FlatYamlConfig ranks, ProgressionMode mode) {
        this.ranks = Objects.requireNonNull(ranks, "ranks");
        this.modeKey = Objects.requireNonNull(mode, "mode") == ProgressionMode.RETRO
                ? "RetroMode"
                : "Standard";
    }

    public static SubSkillRankCatalog load(Path skillRanksFile, ProgressionMode mode)
            throws IOException {
        return new SubSkillRankCatalog(FlatYamlConfig.load(skillRanksFile), mode);
    }

    public int rank(SubSkillInfo subskill, int level) {
        Objects.requireNonNull(subskill, "subskill");
        if (subskill.ranks() == 0) {
            return -1;
        }
        for (int rank = subskill.ranks(); rank >= 1; rank--) {
            if (level >= unlockLevel(subskill, rank)) {
                return rank;
            }
        }
        return 0;
    }

    public boolean unlocked(SubSkillInfo subskill, int level) {
        return rank(subskill, level) != 0;
    }

    public int unlockLevel(SubSkillInfo subskill) {
        if (subskill.ranks() == 0) {
            return 0;
        }
        return unlockLevel(subskill, 1);
    }

    public int unlockLevel(SubSkillInfo subskill, int rank) {
        Objects.requireNonNull(subskill, "subskill");
        if (rank < 1 || rank > subskill.ranks()) {
            throw new IllegalArgumentException("Rank " + rank + " is outside 1-"
                    + subskill.ranks() + " for " + subskill.enumName());
        }
        String path = subskill.parentSkill() + '.' + subskill.configName() + '.'
                + modeKey + ".Rank_" + rank;
        return ranks.requiredInt(path);
    }
}
