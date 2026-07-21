package io.github.njw3995.fabricmmo.core.skill.unarmed;

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

class UnarmedAbilityControllerTest {
    @TempDir Path temporaryDirectory;

    @Test
    void preparationActivationAndCooldownUseDeactivationTimestamp() throws Exception {
        UnarmedSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        UUID playerId = UUID.randomUUID();

        try (UnarmedAbilityController controller = new UnarmedAbilityController(
                new PropertiesUnarmedAbilityStore(temporaryDirectory), clock)) {
            var locked = assertInstanceOf(UnarmedAbilityController.Preparation.Locked.class,
                    controller.prepare(playerId, 49, settings, 240));
            assertEquals(1, locked.levelsRequired());
            assertSame(UnarmedAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));
            assertSame(UnarmedAbilityController.Preparation.LOWERED,
                    controller.prepare(playerId, 50, settings, 240));
            assertSame(UnarmedAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));

            var activation = assertInstanceOf(
                    UnarmedAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(3, activation.durationSeconds());
            assertTrue(controller.isActive(playerId));
            assertEquals(243, controller.cooldownRemaining(playerId, 240));
            clock.advanceSeconds(3);
            assertFalse(controller.isActive(playerId));
            assertEquals(240, controller.cooldownRemaining(playerId, 240));
        }

        try (UnarmedAbilityController reopened = new UnarmedAbilityController(
                new PropertiesUnarmedAbilityStore(temporaryDirectory), clock)) {
            assertEquals(240, reopened.cooldownRemaining(playerId, 240));
        }
    }

    private static UnarmedSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return UnarmedSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("sounds.yml"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        private void advanceSeconds(long seconds) { instant = instant.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
