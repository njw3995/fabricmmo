package io.github.njw3995.fabricmmo.core.skill.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.njw3995.fabricmmo.api.progression.ProgressionMode;
import org.junit.jupiter.api.Test;

class MiningBonusDropDecisionTest {
    private static final MiningDropSettings SETTINGS = MiningDropSettings.upstreamDefaults();

    @Test
    void enablesMotherLodeOnlyAfterUnlockAndPermission() {
        MiningBonusDropDecision belowUnlock = decision(999, true, true, false);
        MiningBonusDropDecision unlocked = decision(1000, true, true, false);

        assertTrue(belowUnlock.eligible());
        assertFalse(belowUnlock.context().motherLodeEnabled());
        assertTrue(unlocked.context().motherLodeEnabled());
    }

    @Test
    void mirrorsUpstreamEligibilityGuards() {
        assertEquals(MiningBonusDropDecision.Status.CREATIVE_MODE,
                evaluate(1000, true, true, true, true, false, true, false, true).status());
        assertEquals(MiningBonusDropDecision.Status.INVALID_TOOL,
                evaluate(1000, false, false, true, true, false, true, false, true).status());
        assertEquals(MiningBonusDropDecision.Status.MISSING_SKILL_PERMISSION,
                evaluate(1000, false, true, false, true, false, true, false, true).status());
        assertEquals(MiningBonusDropDecision.Status.PROTECTION_DENIED,
                evaluate(1000, false, true, true, false, false, true, false, true).status());
        assertEquals(MiningBonusDropDecision.Status.PLAYER_PLACED,
                evaluate(1000, false, true, true, true, true, true, false, true).status());
        assertEquals(MiningBonusDropDecision.Status.SOURCE_DISABLED,
                evaluate(1000, false, true, true, true, false, false, false, true).status());
        assertEquals(MiningBonusDropDecision.Status.MISSING_DOUBLE_DROPS_PERMISSION,
                evaluate(1000, false, true, true, true, false, true, false, false).status());
    }

    @Test
    void configuredSilkTouchGuardRejectsEnchantedTools() {
        MiningDropSettings noSilk = new MiningDropSettings(
                SETTINGS.enabledMaterials(),
                false,
                true,
                100.0D,
                100,
                1000,
                1,
                1,
                50.0D,
                1000,
                10000,
                100,
                1000);

        MiningBonusDropDecision decision = MiningBonusDropDecision.evaluate(
                1000,
                ProgressionMode.RETRO,
                noSilk,
                false,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                false,
                false);

        assertEquals(MiningBonusDropDecision.Status.SILK_TOUCH_DISABLED, decision.status());
    }

    private static MiningBonusDropDecision decision(
            int level, boolean doublePermission, boolean motherPermission, boolean lucky) {
        return MiningBonusDropDecision.evaluate(
                level,
                ProgressionMode.RETRO,
                SETTINGS,
                false,
                true,
                true,
                true,
                false,
                true,
                false,
                doublePermission,
                motherPermission,
                false,
                lucky);
    }

    private static MiningBonusDropDecision evaluate(
            int level,
            boolean creative,
            boolean validTool,
            boolean skillPermission,
            boolean protectionAllowed,
            boolean playerPlaced,
            boolean sourceEnabled,
            boolean silkTouch,
            boolean doublePermission) {
        return MiningBonusDropDecision.evaluate(
                level,
                ProgressionMode.RETRO,
                SETTINGS,
                creative,
                validTool,
                skillPermission,
                protectionAllowed,
                playerPlaced,
                sourceEnabled,
                silkTouch,
                doublePermission,
                true,
                false,
                false);
    }
}
