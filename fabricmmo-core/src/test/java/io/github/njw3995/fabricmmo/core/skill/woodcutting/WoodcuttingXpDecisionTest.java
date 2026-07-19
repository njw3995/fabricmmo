package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WoodcuttingXpDecisionTest {
    @Test
    void naturalAxeBreakAwardsConfiguredXp() {
        assertEquals(
                new WoodcuttingXpDecision(WoodcuttingXpDecision.Status.AWARD, 70),
                WoodcuttingXpDecision.evaluate(70, false, true, true, true, false));
    }

    @Test
    void placedWoodIsRejected() {
        assertEquals(
                WoodcuttingXpDecision.Status.PLAYER_PLACED,
                WoodcuttingXpDecision.evaluate(70, false, true, true, true, true).status());
    }
}
