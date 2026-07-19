package io.github.njw3995.fabricmmo.core.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SubSkillRankCatalogTest {
    @Test
    void packagedRanksCoverEveryApplicableMinecraft1211Skill() throws Exception {
        SubSkillRankCatalog ranks = SubSkillRankCatalog.load(
                Path.of("src/main/resources/defaults/skillranks.yml"),
                ProgressionMode.RETRO);
        var applicable = SubSkillCatalog.instance().entries().stream()
                .filter(SubSkillInfo::applicable)
                .toList();
        Set<String> parents = applicable.stream()
                .map(SubSkillInfo::parentSkill)
                .collect(Collectors.toSet());

        assertEquals(18, parents.size());
        assertFalse(parents.contains("Spears"));
        assertEquals(78, applicable.size());

        for (SubSkillInfo subskill : applicable) {
            if (subskill.ranks() == 0) {
                assertEquals(-1, ranks.rank(subskill, 0), subskill.enumName());
                assertTrue(ranks.unlocked(subskill, 0), subskill.enumName());
                continue;
            }
            int previous = -1;
            for (int rank = 1; rank <= subskill.ranks(); rank++) {
                int unlock = ranks.unlockLevel(subskill, rank);
                assertTrue(unlock >= previous, subskill.enumName());
                previous = unlock;
            }
        }
    }

    @Test
    void retroUnlocksMatchPinnedMiningExample() throws Exception {
        SubSkillRankCatalog ranks = SubSkillRankCatalog.load(
                Path.of("src/main/resources/defaults/skillranks.yml"),
                ProgressionMode.RETRO);
        SubSkillInfo superBreaker = SubSkillCatalog.instance().entries().stream()
                .filter(subskill -> subskill.enumName().equals("MINING_SUPER_BREAKER"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, ranks.rank(superBreaker, 49));
        assertEquals(1, ranks.rank(superBreaker, 50));
    }
}
