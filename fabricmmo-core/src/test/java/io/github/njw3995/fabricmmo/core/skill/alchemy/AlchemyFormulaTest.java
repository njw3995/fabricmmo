package io.github.njw3995.fabricmmo.core.skill.alchemy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class AlchemyFormulaTest {
    @Test
    void catalysisMatchesPinnedCurveAndLuckyBehavior() {
        assertEquals(1.0D, AlchemyFormula.catalysisSpeed(0, 0, 100, 1.0D, 4.0D, false));
        assertEquals(2.5D, AlchemyFormula.catalysisSpeed(50, 0, 100, 1.0D, 4.0D, false));
        assertEquals(4.0D, AlchemyFormula.catalysisSpeed(100, 0, 100, 1.0D, 4.0D, false));
        assertEquals(16.0D / 3.0D,
                AlchemyFormula.catalysisSpeed(100, 0, 100, 1.0D, 4.0D, true), 1.0E-12D);
    }

    @Test
    void concoctionRanksUseAllEightUpstreamUnlocks() {
        List<Integer> unlocks = List.of(0, 10, 20, 35, 50, 75, 90, 100);
        assertEquals(1, AlchemyFormula.concoctionsTier(0, unlocks));
        assertEquals(2, AlchemyFormula.concoctionsTier(10, unlocks));
        assertEquals(4, AlchemyFormula.concoctionsTier(35, unlocks));
        assertEquals(8, AlchemyFormula.concoctionsTier(100, unlocks));
    }

    @Test
    void potionStagesMatchUpstreamStageRules() {
        var water = new AlchemyFormula.PotionShape(false, false, false, false, false);
        var effect = new AlchemyFormula.PotionShape(true, false, false, false, false);
        var strong = new AlchemyFormula.PotionShape(true, true, false, false, false);
        var extendedSplash = new AlchemyFormula.PotionShape(true, false, false, true, true);
        assertEquals(1, AlchemyFormula.stage(water));
        assertEquals(2, AlchemyFormula.stage(effect));
        assertEquals(3, AlchemyFormula.stage(strong));
        assertEquals(4, AlchemyFormula.stage(extendedSplash));
        assertEquals(5, AlchemyFormula.potionStage(false, effect, effect));
        assertEquals(2, AlchemyFormula.potionStage(true, water, effect));
    }
}
