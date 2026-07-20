package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DodgeXpTrackerTest {
    @Test
    void aMobOnlyPaysSixTimesInsideTheIdleWindow() {
        DodgeXpTracker tracker = new DodgeXpTracker();
        UUID mob = UUID.randomUUID();
        for (int index = 0; index < DodgeXpTracker.MAX_XP_REWARDS_PER_MOB; index++) {
            assertTrue(tracker.tryConsume(mob, 1_000L + index));
        }
        assertFalse(tracker.tryConsume(mob, 2_000L));
    }

    @Test
    void aMobResetsAfterSixtySecondsWithoutADodge() {
        DodgeXpTracker tracker = new DodgeXpTracker();
        UUID mob = UUID.randomUUID();
        for (int index = 0; index <= DodgeXpTracker.MAX_XP_REWARDS_PER_MOB; index++) {
            tracker.tryConsume(mob, 1_000L);
        }
        assertTrue(tracker.tryConsume(mob, 1_000L + DodgeXpTracker.IDLE_RESET_MILLIS));
    }

    @Test
    void freshMobsAreTrackedIndependently() {
        DodgeXpTracker tracker = new DodgeXpTracker();
        for (int index = 0; index < 100; index++) {
            assertTrue(tracker.tryConsume(UUID.randomUUID(), 1_000L + index));
        }
        assertEquals(100, tracker.trackedMobCount());
    }
    @Test
    void deniedDodgesRefreshIdleTimeWithoutIncreasingTheCappedCount() {
        DodgeXpTracker tracker = new DodgeXpTracker();
        UUID mob = UUID.randomUUID();
        for (int index = 0; index < DodgeXpTracker.MAX_XP_REWARDS_PER_MOB; index++) {
            assertTrue(tracker.tryConsume(mob, 1_000L + index));
        }
        assertFalse(tracker.tryConsume(mob, 2_000L));
        assertEquals(DodgeXpTracker.MAX_XP_REWARDS_PER_MOB, tracker.rewardsFor(mob));
        assertFalse(tracker.tryConsume(mob, 2_000L + DodgeXpTracker.IDLE_RESET_MILLIS - 1L));
        assertTrue(tracker.tryConsume(mob, 2_000L + DodgeXpTracker.IDLE_RESET_MILLIS * 2L));
    }

}
