package io.github.njw3995.fabricmmo.core.skill.ranged;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RangedProjectileDataTest {
    @AfterEach
    void clear() {
        RangedProjectileData.clear();
    }

    @Test
    void retrievalCanBeConsumedOnlyOncePerProjectile() {
        UUID projectile = UUID.randomUUID();
        RangedProjectileData.create(
                projectile,
                RangedWeaponKind.ARCHERY,
                UUID.randomUUID(),
                "minecraft:overworld",
                Vec3d.ZERO,
                1.0D,
                false,
                true);

        assertTrue(RangedProjectileData.consumeRetrieval(projectile));
        assertFalse(RangedProjectileData.consumeRetrieval(projectile));
    }

    @Test
    void ricochetStatePreservesLaunchDataAndIncrementsBounce() {
        UUID projectile = UUID.randomUUID();
        RangedProjectileData.ProjectileState state = RangedProjectileData.create(
                projectile,
                RangedWeaponKind.CROSSBOWS,
                UUID.randomUUID(),
                "minecraft:overworld",
                new Vec3d(1.0D, 2.0D, 3.0D),
                1.0D,
                true,
                false);

        RangedProjectileData.ProjectileState bounced = state.bounced();
        assertEquals(1, bounced.bounceCount());
        assertEquals(state.launchPosition(), bounced.launchPosition());
        assertEquals(state.ownerId(), bounced.ownerId());
        assertTrue(bounced.infinite());
    }

    @Test
    void corpseTrackerAccumulatesAcceptedArcheryHits() {
        UUID target = UUID.randomUUID();
        UUID firstAttacker = UUID.randomUUID();
        UUID secondAttacker = UUID.randomUUID();
        RangedProjectileData.addRetrieval(target, firstAttacker);
        RangedProjectileData.addRetrieval(target, secondAttacker);

        RangedProjectileData.RetrievalState state =
                RangedProjectileData.removeRetrieval(target).orElseThrow();
        assertEquals(firstAttacker, state.attackerId());
        assertEquals(2, state.count());
    }

    @Test
    void ownerAndKindRefreshDoNotRenewMetadataLifetime() {
        UUID projectile = UUID.randomUUID();
        RangedProjectileData.ProjectileState state = RangedProjectileData.create(
                projectile,
                RangedWeaponKind.ARCHERY,
                UUID.randomUUID(),
                "minecraft:overworld",
                Vec3d.ZERO,
                1.0D,
                false,
                false);

        RangedProjectileData.put(
                projectile,
                state.withOwner(UUID.randomUUID()).withKind(RangedWeaponKind.CROSSBOWS));

        assertEquals(
                state.expiresAtMillis(),
                RangedProjectileData.projectile(projectile).orElseThrow().expiresAtMillis());
    }

    @Test
    void ricochetRenewsMetadataLifetime() {
        UUID projectile = UUID.randomUUID();
        RangedProjectileData.ProjectileState stale = new RangedProjectileData.ProjectileState(
                RangedWeaponKind.CROSSBOWS,
                UUID.randomUUID(),
                "minecraft:overworld",
                Vec3d.ZERO,
                1.0D,
                false,
                false,
                1,
                1L);

        long before = System.currentTimeMillis();
        RangedProjectileData.putRenewed(projectile, stale);

        assertTrue(RangedProjectileData.projectile(projectile)
                .orElseThrow().expiresAtMillis() > before);
    }
}
