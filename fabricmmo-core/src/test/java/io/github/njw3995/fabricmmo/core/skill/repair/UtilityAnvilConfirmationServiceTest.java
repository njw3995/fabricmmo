package io.github.njw3995.fabricmmo.core.skill.repair;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UtilityAnvilConfirmationServiceTest {
    @Test
    void confirmationRequiresSameItemAndComponents() {
        MutableClock clock = new MutableClock();
        TimedIdentityConfirmation<UtilityAnvilConfirmationService.Kind, TestItem, TestItem>
                confirmations = confirmations(clock);
        UUID player = UUID.randomUUID();
        TestItem prompted = new TestItem("diamond_pickaxe", 12, 0);
        assertFalse(confirmations.confirmOrPrompt(
                player, UtilityAnvilConfirmationService.Kind.REPAIR, prompted));
        assertTrue(confirmations.confirmOrPrompt(
                player, UtilityAnvilConfirmationService.Kind.REPAIR, prompted.copy()));

        TestItem changed = new TestItem("diamond_pickaxe", 13, 0);
        assertFalse(confirmations.confirmOrPrompt(
                player, UtilityAnvilConfirmationService.Kind.REPAIR, changed));
    }

    @Test
    void rebindAllowsConsecutivePartialRepairsWithoutReprompt() {
        MutableClock clock = new MutableClock();
        TimedIdentityConfirmation<UtilityAnvilConfirmationService.Kind, TestItem, TestItem>
                confirmations = confirmations(clock);
        UUID player = UUID.randomUUID();
        TestItem item = new TestItem("iron_pickaxe", 100, 0);
        assertFalse(confirmations.confirmOrPrompt(
                player, UtilityAnvilConfirmationService.Kind.REPAIR, item));
        assertTrue(confirmations.confirmOrPrompt(
                player, UtilityAnvilConfirmationService.Kind.REPAIR, item));
        TestItem repaired = new TestItem("iron_pickaxe", 50, 0);
        confirmations.rebind(player, UtilityAnvilConfirmationService.Kind.REPAIR, repaired);
        assertTrue(confirmations.isAwaiting(
                player, UtilityAnvilConfirmationService.Kind.REPAIR, repaired.copy()));
    }

    @Test
    void pendingItemCannotBeUsedAndExpiresAtThreeSeconds() {
        MutableClock clock = new MutableClock();
        TimedIdentityConfirmation<UtilityAnvilConfirmationService.Kind, TestItem, TestItem>
                confirmations = confirmations(clock);
        UUID player = UUID.randomUUID();
        TestItem item = new TestItem("diamond_chestplate", 0, 7);
        assertFalse(confirmations.confirmOrPrompt(
                player, UtilityAnvilConfirmationService.Kind.SALVAGE, item));
        assertTrue(confirmations.blocksItemUse(player, item.copy()));
        clock.advanceSeconds(3);
        assertFalse(confirmations.blocksItemUse(player, item));
    }

    private static TimedIdentityConfirmation<UtilityAnvilConfirmationService.Kind,
            TestItem, TestItem> confirmations(Clock clock) {
        return new TimedIdentityConfirmation<>(clock, Duration.ofSeconds(3),
                TestItem::copy, TestItem::equals);
    }

    private record TestItem(String itemId, int damage, int customModelData) {
        private TestItem copy() {
            return new TestItem(itemId, damage, customModelData);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-07-20T12:00:00Z");

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
