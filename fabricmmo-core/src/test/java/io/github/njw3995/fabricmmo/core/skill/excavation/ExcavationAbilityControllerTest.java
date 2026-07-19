package io.github.njw3995.fabricmmo.core.skill.excavation;

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

class ExcavationAbilityControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preparationActivationAndCooldownPersistLikeUpstream() throws Exception {
        ExcavationSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (ExcavationAbilityController controller = new ExcavationAbilityController(
                new PropertiesExcavationAbilityStore(temporaryDirectory), clock)) {
            assertSame(ExcavationAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));
            var active = assertInstanceOf(
                    ExcavationAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(3, active.durationSeconds());
            assertTrue(controller.isActive(playerId));
            assertEquals(243, controller.cooldownRemaining(playerId, 240));
            clock.advanceSeconds(3);
            assertFalse(controller.isActive(playerId));
            assertEquals(240, controller.cooldownRemaining(playerId, 240));
        }
        try (ExcavationAbilityController reopened = new ExcavationAbilityController(
                new PropertiesExcavationAbilityStore(temporaryDirectory), clock)) {
            assertEquals(240, reopened.cooldownRemaining(playerId, 240));
        }
    }

    @Test
    void preparationChecksUnlockAndCooldownBeforeReadying() throws Exception {
        ExcavationSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (ExcavationAbilityController controller = new ExcavationAbilityController(
                new PropertiesExcavationAbilityStore(temporaryDirectory), clock)) {
            var locked = assertInstanceOf(
                    ExcavationAbilityController.Preparation.Locked.class,
                    controller.prepare(playerId, 49, settings, 240));
            assertEquals(1, locked.levelsRequired());
            assertFalse(controller.isPrepared(playerId));

            controller.prepare(playerId, 50, settings, 240);
            controller.activate(playerId, 50, settings, 240, 0);
            clock.advanceSeconds(3);
            var cooldown = assertInstanceOf(
                    ExcavationAbilityController.Preparation.Cooldown.class,
                    controller.prepare(playerId, 50, settings, 240));
            assertEquals(240, cooldown.secondsRemaining());
        }
    }

    private static ExcavationSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return ExcavationSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
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
