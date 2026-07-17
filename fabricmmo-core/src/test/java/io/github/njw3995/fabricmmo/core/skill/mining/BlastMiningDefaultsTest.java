package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BlastMiningDefaultsTest {
    @Test
    void dropMultiplierMatchesCurrentUpstreamRankBehavior() {
        assertEquals(0, BlastMiningDefaults.dropMultiplier(0));
        assertEquals(1, BlastMiningDefaults.dropMultiplier(1));
        assertEquals(1, BlastMiningDefaults.dropMultiplier(2));
        assertEquals(2, BlastMiningDefaults.dropMultiplier(3));
        assertEquals(2, BlastMiningDefaults.dropMultiplier(6));
        assertEquals(3, BlastMiningDefaults.dropMultiplier(7));
        assertEquals(3, BlastMiningDefaults.dropMultiplier(8));
    }
}
