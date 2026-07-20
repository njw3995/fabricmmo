package io.github.njw3995.fabricmmo.core.skill.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import net.minecraft.entity.SpawnReason;
import org.junit.jupiter.api.Test;

class CombatMobOriginTest {
    @Test
    void resolvesEveryPersistedOriginAndNaturalFallback() {
        assertEquals(CombatXpSettings.Origin.NATURAL,
                CombatMobOrigin.origin(Set.of(), false));
        assertEquals(CombatXpSettings.Origin.PLAYER_TAMED,
                CombatMobOrigin.origin(Set.of(), true));
        assertEquals(CombatXpSettings.Origin.EGG,
                CombatMobOrigin.origin(Set.of(CombatMobOrigin.EGG_TAG), false));
        assertEquals(CombatXpSettings.Origin.SPAWNER,
                CombatMobOrigin.origin(Set.of(CombatMobOrigin.SPAWNER_TAG), false));
        assertEquals(CombatXpSettings.Origin.NETHER_PORTAL,
                CombatMobOrigin.origin(Set.of(CombatMobOrigin.NETHER_PORTAL_TAG), false));
        assertEquals(CombatXpSettings.Origin.BRED,
                CombatMobOrigin.origin(Set.of(CombatMobOrigin.BRED_TAG), false));
    }

    @Test
    void mapsVanillaSpawnReasonsToPinnedBukkitOriginSemantics() {
        assertEquals(CombatXpSettings.Origin.SPAWNER,
                CombatMobOrigin.fromSpawnReason(SpawnReason.SPAWNER));
        assertEquals(CombatXpSettings.Origin.SPAWNER,
                CombatMobOrigin.fromSpawnReason(SpawnReason.SPAWN_EGG));
        assertEquals(CombatXpSettings.Origin.EGG,
                CombatMobOrigin.fromSpawnReason(SpawnReason.DISPENSER));
        assertEquals(CombatXpSettings.Origin.BRED,
                CombatMobOrigin.fromSpawnReason(SpawnReason.BREEDING));
        assertEquals(CombatXpSettings.Origin.NATURAL,
                CombatMobOrigin.fromSpawnReason(SpawnReason.TRIAL_SPAWNER));
        assertEquals(CombatXpSettings.Origin.NATURAL,
                CombatMobOrigin.fromSpawnReason(SpawnReason.NATURAL));
    }

    @Test
    void preservesPinnedUpstreamFlagPrecedence() {
        assertEquals(CombatXpSettings.Origin.SPAWNER,
                CombatMobOrigin.origin(Set.of(
                        CombatMobOrigin.SPAWNER_TAG,
                        CombatMobOrigin.NETHER_PORTAL_TAG,
                        CombatMobOrigin.EGG_TAG,
                        CombatMobOrigin.BRED_TAG), true));
        assertEquals(CombatXpSettings.Origin.NETHER_PORTAL,
                CombatMobOrigin.origin(Set.of(
                        CombatMobOrigin.NETHER_PORTAL_TAG,
                        CombatMobOrigin.EGG_TAG,
                        CombatMobOrigin.BRED_TAG), true));
        assertEquals(CombatXpSettings.Origin.EGG,
                CombatMobOrigin.origin(Set.of(
                        CombatMobOrigin.EGG_TAG,
                        CombatMobOrigin.BRED_TAG), true));
        assertEquals(CombatXpSettings.Origin.BRED,
                CombatMobOrigin.origin(Set.of(CombatMobOrigin.BRED_TAG), true));
    }
}
