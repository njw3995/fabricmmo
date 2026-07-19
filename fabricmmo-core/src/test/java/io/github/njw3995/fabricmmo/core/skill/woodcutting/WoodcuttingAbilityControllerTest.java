package io.github.njw3995.fabricmmo.core.skill.woodcutting;

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

class WoodcuttingAbilityControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void preparationActivationAndCooldownPersist() throws Exception {
        WoodcuttingSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (WoodcuttingAbilityController controller = new WoodcuttingAbilityController(
                new PropertiesWoodcuttingAbilityStore(temporaryDirectory), clock)) {
            assertSame(WoodcuttingAbilityController.Preparation.READY,
                    controller.prepare(playerId, settings));
            assertSame(WoodcuttingAbilityController.Preparation.LOWERED,
                    controller.prepare(playerId, settings));
            assertSame(WoodcuttingAbilityController.Preparation.READY,
                    controller.prepare(playerId, settings));
            var active = assertInstanceOf(
                    WoodcuttingAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(3, active.durationSeconds());
            assertTrue(controller.isActive(playerId));
            assertEquals(243, controller.cooldownRemaining(playerId, 240));
            clock.advanceSeconds(3);
            assertEquals(240, controller.cooldownRemaining(playerId, 240));
        }
        try (WoodcuttingAbilityController reopened = new WoodcuttingAbilityController(
                new PropertiesWoodcuttingAbilityStore(temporaryDirectory), clock)) {
            assertEquals(240, reopened.cooldownRemaining(playerId, 240));
        }
    }

    @Test
    void axePreparationDefersUnlockAndCooldownChecksUntilContact() throws Exception {
        WoodcuttingSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (WoodcuttingAbilityController controller = new WoodcuttingAbilityController(
                new PropertiesWoodcuttingAbilityStore(temporaryDirectory), clock)) {
            assertSame(WoodcuttingAbilityController.Preparation.READY,
                    controller.prepare(playerId, settings));
            var locked = assertInstanceOf(
                    WoodcuttingAbilityController.Activation.Locked.class,
                    controller.activate(playerId, 49, settings, 240, 0));
            assertEquals(1, locked.levelsRequired());
            assertTrue(controller.isPrepared(playerId));

            var active = assertInstanceOf(
                    WoodcuttingAbilityController.Activation.Activated.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(3, active.durationSeconds());
            clock.advanceSeconds(3);
            assertSame(WoodcuttingAbilityController.Preparation.READY,
                    controller.prepare(playerId, settings));
            var cooldown = assertInstanceOf(
                    WoodcuttingAbilityController.Activation.Cooldown.class,
                    controller.activate(playerId, 50, settings, 240, 0));
            assertEquals(240, cooldown.secondsRemaining());
            assertTrue(controller.isPrepared(playerId));
        }
    }

    @Test
    void resetClearsPersistentAndTransientState() throws Exception {
        WoodcuttingSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (WoodcuttingAbilityController controller = new WoodcuttingAbilityController(
                new PropertiesWoodcuttingAbilityStore(temporaryDirectory), clock)) {
            controller.prepare(playerId, settings);
            controller.activate(playerId, 50, settings, 240, 0);
            controller.reset(playerId);
            assertFalse(controller.isPrepared(playerId));
            assertFalse(controller.isActive(playerId));
            assertEquals(0, controller.cooldownRemaining(playerId, 240));
        }
    }

    private static WoodcuttingSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return WoodcuttingSettings.load(
                defaults.resolve("config.yml"),
                defaults.resolve("advanced.yml"),
                defaults.resolve("skillranks.yml"),
                defaults.resolve("experience.yml"));
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
