package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiningXpDecisionTest {
    @Test
    void awardsConfiguredXpForNaturalPermittedToolBreak() {
        MiningXpDecision decision = MiningXpDecision.evaluate(
                2400, false, true, true, true, false);

        assertTrue(decision.awardsXp());
        assertEquals(MiningXpDecision.Status.AWARD, decision.status());
        assertEquals(2400, decision.xp());
    }

    @Test
    void mirrorsUpstreamEligibilityGuards() {
        assertEquals(MiningXpDecision.Status.CREATIVE_MODE,
                MiningXpDecision.evaluate(15, true, true, true, true, false).status());
        assertEquals(MiningXpDecision.Status.INVALID_TOOL,
                MiningXpDecision.evaluate(15, false, false, true, true, false).status());
        assertEquals(MiningXpDecision.Status.MISSING_PERMISSION,
                MiningXpDecision.evaluate(15, false, true, false, true, false).status());
        assertEquals(MiningXpDecision.Status.PROTECTION_DENIED,
                MiningXpDecision.evaluate(15, false, true, true, false, false).status());
        assertEquals(MiningXpDecision.Status.PLAYER_PLACED,
                MiningXpDecision.evaluate(15, false, true, true, true, true).status());
        assertEquals(MiningXpDecision.Status.NO_CONFIGURED_XP,
                MiningXpDecision.evaluate(0, false, true, true, true, false).status());
    }
}
