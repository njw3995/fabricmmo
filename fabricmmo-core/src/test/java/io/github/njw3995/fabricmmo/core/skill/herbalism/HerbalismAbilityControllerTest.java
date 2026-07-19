package io.github.njw3995.fabricmmo.core.skill.herbalism;

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

class HerbalismAbilityControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void greenTerraDurationAndCooldownPersistLikeUpstream() throws Exception {
        HerbalismSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-19T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (HerbalismAbilityController controller = new HerbalismAbilityController(
                new PropertiesHerbalismAbilityStore(temporaryDirectory), clock)) {
            assertSame(HerbalismAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));
            var active = assertInstanceOf(
                    HerbalismAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(3, active.durationSeconds());
            assertTrue(controller.isActive(playerId));
            assertEquals(243, controller.cooldownRemaining(playerId, 240));
            clock.advanceSeconds(3);
            assertFalse(controller.isActive(playerId));
            assertEquals(240, controller.cooldownRemaining(playerId, 240));
        }
        try (HerbalismAbilityController reopened = new HerbalismAbilityController(
                new PropertiesHerbalismAbilityStore(temporaryDirectory), clock)) {
            assertEquals(240, reopened.cooldownRemaining(playerId, 240));
        }
    }


    @Test
    void repeatedPreparationDoesNotLowerTheHoeBeforeTheTimeout() throws Exception {
        HerbalismSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-19T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (HerbalismAbilityController controller = new HerbalismAbilityController(
                new PropertiesHerbalismAbilityStore(temporaryDirectory), clock)) {
            assertSame(HerbalismAbilityController.Preparation.READY,
                    controller.prepare(playerId, 50, settings, 240));
            assertSame(HerbalismAbilityController.Preparation.ALREADY_PREPARED,
                    controller.prepare(playerId, 50, settings, 240));
            assertTrue(controller.isPrepared(playerId));

            clock.advanceSeconds(4);
            HerbalismAbilityController.TickResult result = controller.tick(playerId);
            assertTrue(result.preparationExpired());
            assertFalse(controller.isPrepared(playerId));
        }
    }

    @Test
    void levelFortyNineIsLockedInRetroMode() throws Exception {
        HerbalismSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-19T00:00:00Z"));
        try (HerbalismAbilityController controller = new HerbalismAbilityController(
                new PropertiesHerbalismAbilityStore(temporaryDirectory), clock)) {
            var locked = assertInstanceOf(HerbalismAbilityController.Preparation.Locked.class,
                    controller.prepare(UUID.randomUUID(), 49, settings, 240));
            assertEquals(1, locked.levelsRequired());
        }
    }

    private static HerbalismSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return HerbalismSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
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
