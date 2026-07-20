package io.github.njw3995.fabricmmo.core.skill.swords;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SwordsAbilityControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preparationActivationAndCooldownMatchUpstreamTiming() throws Exception {
        SwordsSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        UUID playerId = UUID.randomUUID();

        try (SwordsAbilityController controller = new SwordsAbilityController(
                new PropertiesSwordsAbilityStore(temporaryDirectory), clock)) {
            var locked = assertInstanceOf(SwordsAbilityController.Preparation.Locked.class,
                    controller.prepare(playerId, 49, settings, 240));
            assertEquals(1, locked.levelsRequired());
            assertSame(SwordsAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));
            assertSame(SwordsAbilityController.Preparation.LOWERED,
                    controller.prepare(playerId, 50, settings, 240));
            assertSame(SwordsAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));

            var activation = assertInstanceOf(
                    SwordsAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(3, activation.durationSeconds());
            assertTrue(controller.isActive(playerId));
            assertEquals(243, controller.cooldownRemaining(playerId, 240));
            clock.advanceSeconds(3);
            assertFalse(controller.isActive(playerId));
            assertEquals(240, controller.cooldownRemaining(playerId, 240));
        }

        try (SwordsAbilityController reopened = new SwordsAbilityController(
                new PropertiesSwordsAbilityStore(temporaryDirectory), clock)) {
            assertEquals(240, reopened.cooldownRemaining(playerId, 240));
            var cooldown = assertInstanceOf(SwordsAbilityController.Preparation.Cooldown.class,
                    reopened.prepare(playerId, 50, settings, 240));
            assertEquals(240, cooldown.secondsRemaining());
        }
    }

    @Test
    void perksAndResetAreAppliedAndPersisted() throws Exception {
        SwordsSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        UUID playerId = UUID.randomUUID();

        try (SwordsAbilityController controller = new SwordsAbilityController(
                new PropertiesSwordsAbilityStore(temporaryDirectory), clock)) {
            assertSame(SwordsAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 120));
            var activation = assertInstanceOf(
                    SwordsAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 120, 12));
            assertEquals(15, activation.durationSeconds());
            assertEquals(135, controller.cooldownRemaining(playerId, 120));

            controller.reset(playerId);
            assertFalse(controller.isPrepared(playerId));
            assertFalse(controller.isActive(playerId));
            assertEquals(0, controller.cooldownRemaining(playerId, 120));
        }
    }

    private static SwordsSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return SwordsSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("sounds.yml"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
