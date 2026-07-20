package io.github.njw3995.fabricmmo.core.skill.ranged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RangedRetrievalDeathCoordinatorTest {
    private final RangedRetrievalDeathCoordinator coordinator =
            new RangedRetrievalDeathCoordinator();

    @AfterEach
    void clearProjectileData() {
        RangedProjectileData.clear();
    }

    @Test
    void lethalArrowIsRecordedBeforeCorpseDropsAreConsumed() {
        UUID target = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        for (int index = 0; index < 7; index++) {
            RangedProjectileData.addRetrieval(target, attacker);
        }

        assertFalse(coordinator.onDeath(target, true));
        RangedProjectileData.addRetrieval(target, attacker);
        assertTrue(coordinator.afterDamage(target));

        assertEquals(8, RangedProjectileData.removeRetrieval(target).orElseThrow().count());
    }

    @Test
    void nonRangedDeathDropsPreviouslyTrackedArrowsImmediately() {
        UUID target = UUID.randomUUID();

        assertTrue(coordinator.onDeath(target, false));
        assertFalse(coordinator.afterDamage(target));
    }
}
