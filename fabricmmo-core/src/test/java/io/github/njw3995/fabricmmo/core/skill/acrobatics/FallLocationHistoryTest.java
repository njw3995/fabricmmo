package io.github.njw3995.fabricmmo.core.skill.acrobatics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FallLocationHistoryTest {
    @Test
    void remembersExactlyTheFiftyMostRecentUniqueLocations() {
        FallLocationHistory history = new FallLocationHistory();
        FallLocationHistory.FallLocation first = location(0);
        history.add(first);
        for (int index = 1; index <= FallLocationHistory.CAPACITY; index++) {
            history.add(location(index));
        }
        assertFalse(history.contains(first));
        assertTrue(history.contains(location(FallLocationHistory.CAPACITY)));
    }

    @Test
    void duplicateFallsOccupySeparateHistorySlotsLikeUpstreamMultiset() {
        FallLocationHistory history = new FallLocationHistory();
        FallLocationHistory.FallLocation repeated = location(0);
        history.add(repeated);
        history.add(repeated);
        for (int index = 1; index < FallLocationHistory.CAPACITY; index++) {
            history.add(location(index));
        }
        assertTrue(history.contains(repeated));
        history.add(location(FallLocationHistory.CAPACITY));
        assertFalse(history.contains(repeated));
    }

    private static FallLocationHistory.FallLocation location(int x) {
        return new FallLocationHistory.FallLocation("minecraft:overworld", x, 64, 0);
    }
}
