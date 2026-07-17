package io.github.njw3995.fabricmmo.core.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MiningPermissionNodesTest {
    @Test
    void matchesPinnedUpstreamMiningPermissionNodes() {
        assertEquals("mcmmo.skills.mining", PermissionNodes.MINING);
        assertEquals("mcmmo.commands.mining", PermissionNodes.MINING_COMMAND);
        assertEquals("mcmmo.ability.mining.superbreaker", PermissionNodes.MINING_SUPER_BREAKER);
        assertEquals("mcmmo.ability.mining.doubledrops", PermissionNodes.MINING_DOUBLE_DROPS);
        assertEquals("mcmmo.ability.mining.motherlode", PermissionNodes.MINING_MOTHER_LODE);
        assertEquals("mcmmo.ability.mining.blastmining.detonate", PermissionNodes.MINING_BLAST_MINING);
        assertEquals("mcmmo.ability.mining.blastmining.biggerbombs", PermissionNodes.MINING_BIGGER_BOMBS);
        assertEquals("mcmmo.ability.mining.blastmining.demolitionsexpertise",
                PermissionNodes.MINING_DEMOLITIONS_EXPERTISE);
    }
}
