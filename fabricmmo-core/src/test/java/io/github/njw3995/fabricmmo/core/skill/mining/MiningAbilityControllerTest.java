package io.github.njw3995.fabricmmo.core.skill.mining;

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

class MiningAbilityControllerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void superBreakerPreparationActivationAndCooldownMatchUpstreamTiming() throws Exception {
        MiningSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (MiningAbilityController controller = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            assertSame(MiningAbilityController.SuperBreakerPreparation.READY,
                    controller.prepareSuperBreaker(playerId, 50, settings));
            assertSame(MiningAbilityController.SuperBreakerPreparation.LOWERED,
                    controller.prepareSuperBreaker(playerId, 50, settings));
            assertSame(MiningAbilityController.SuperBreakerPreparation.READY,
                    controller.prepareSuperBreaker(playerId, 50, settings));
            var activation = assertInstanceOf(
                    MiningAbilityController.SuperBreakerActivation.Activated.class,
                    controller.activateSuperBreaker(playerId, 50, settings));
            assertEquals(3, activation.durationSeconds());
            assertTrue(controller.isSuperBreakerActive(playerId));
            assertEquals(243, controller.superBreakerCooldownRemaining(playerId, settings));
            clock.advanceSeconds(3);
            assertEquals(240, controller.superBreakerCooldownRemaining(playerId, settings));
        }

        try (MiningAbilityController reopened = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            assertEquals(240, reopened.superBreakerCooldownRemaining(playerId, settings));
            var cooldown = assertInstanceOf(
                    MiningAbilityController.SuperBreakerPreparation.Cooldown.class,
                    reopened.prepareSuperBreaker(playerId, 50, settings));
            assertEquals(240, cooldown.secondsRemaining());
        }
    }


    @Test
    void activationAndCooldownPerkValuesAreAppliedByTheController() throws Exception {
        MiningSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (MiningAbilityController controller = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            assertSame(MiningAbilityController.SuperBreakerPreparation.READY,
                    controller.prepareSuperBreaker(playerId, 50, settings, 120));
            var activation = assertInstanceOf(
                    MiningAbilityController.SuperBreakerActivation.Activated.class,
                    controller.activateSuperBreaker(playerId, 50, settings, 12));
            assertEquals(15, activation.durationSeconds());
            assertEquals(135, controller.superBreakerCooldownRemaining(playerId, 120));
            clock.advanceSeconds(15);
            assertEquals(120, controller.superBreakerCooldownRemaining(playerId, 120));
        }
    }

    @Test
    void refreshClearsPersistedCooldownsAndTransientState() throws Exception {
        MiningSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (MiningAbilityController controller = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            assertSame(MiningAbilityController.SuperBreakerPreparation.READY,
                    controller.prepareSuperBreaker(playerId, 50, settings));
            controller.activateSuperBreaker(playerId, 50, settings);
            controller.activateBlastMining(playerId, 100, settings);

            controller.reset(playerId);

            assertEquals(0, controller.superBreakerCooldownRemaining(playerId, settings));
            assertEquals(0, controller.blastCooldownRemaining(playerId, settings));
            assertFalse(controller.isSuperBreakerActive(playerId));
            assertFalse(controller.isSuperBreakerPrepared(playerId));
        }
        try (MiningAbilityController reopened = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            assertEquals(0, reopened.superBreakerCooldownRemaining(playerId, settings));
            assertEquals(0, reopened.blastCooldownRemaining(playerId, settings));
        }
    }

    @Test
    void blastMiningRankAndCooldownPersist() throws Exception {
        MiningSettings settings = settings();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-17T00:00:00Z"));
        UUID playerId = UUID.randomUUID();
        try (MiningAbilityController controller = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            var activation = assertInstanceOf(
                    MiningAbilityController.BlastActivation.Activated.class,
                    controller.activateBlastMining(playerId, 100, settings));
            assertEquals(1, activation.rank());
            assertEquals(60, controller.blastCooldownRemaining(playerId, settings));
        }
        try (MiningAbilityController reopened = new MiningAbilityController(
                new PropertiesMiningAbilityStore(temporaryDirectory), clock)) {
            assertEquals(60, reopened.blastCooldownRemaining(playerId, settings));
        }
    }

    private static MiningSettings settings() throws Exception {
        Path defaults = Path.of("src/main/resources/defaults");
        return MiningSettings.load(
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
