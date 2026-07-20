package io.github.njw3995.fabricmmo.core.skill.crossbows;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.mixin.PersistentProjectileEntityAccessor;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedProjectileData;
import io.github.njw3995.fabricmmo.core.skill.ranged.RangedWeaponKind;
import java.util.UUID;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Fabric translation of mcMMO 2.3.000 Crossbows Trick Shot. */
public final class CrossbowRicochet {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final double FIRST_BOUNCE_MINIMUM_ANGLE = Math.toRadians(45.0D);

    private CrossbowRicochet() {
    }

    public static boolean tryRicochet(
            PersistentProjectileEntity projectile,
            BlockHitResult hit) {
        if (!FabricMmoFabricRuntime.running()
                || !(projectile.getWorld() instanceof ServerWorld world)
                || !(projectile instanceof ArrowEntity)
                || projectile instanceof SpectralArrowEntity
                || !projectile.isShotFromCrossbow()) {
            return false;
        }
        RangedProjectileData.ProjectileState state = RangedProjectileData
                .projectile(projectile.getUuid()).orElse(null);
        if (state == null || state.kind() != RangedWeaponKind.CROSSBOWS) {
            return false;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(state.ownerId());
        if (owner == null
                || !allowed(owner, PermissionNodes.CROSSBOWS, true)
                || !allowed(owner, PermissionNodes.CROSSBOWS_TRICK_SHOT, true)
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return false;
        }
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(owner.getUuid(), CoreSkills.CROSSBOWS).level();
        int maximumBounces = FabricMmoFabricRuntime.crossbowsSettings().trickShotRank(level);
        if (maximumBounces <= state.bounceCount()) {
            return false;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        if (!FabricMmoFabricRuntime.requireApi().protection().canInteract(
                owner.getUuid(), worldId, hit.getBlockPos().getX(),
                hit.getBlockPos().getY(), hit.getBlockPos().getZ())) {
            return false;
        }

        Vec3d velocity = projectile.getVelocity();
        if (velocity.lengthSquared() <= 1.0E-8D) {
            return false;
        }
        Vec3d normal = normal(hit.getSide());
        Vec3d incoming = velocity.normalize();
        if (state.bounceCount() == 0) {
            double dot = Math.clamp(incoming.dotProduct(normal.negate()), -1.0D, 1.0D);
            if (Math.acos(dot) < FIRST_BOUNCE_MINIMUM_ANGLE) {
                return false;
            }
        }
        Vec3d reflected = incoming.subtract(normal.multiply(2.0D * incoming.dotProduct(normal)));
        if (reflected.lengthSquared() <= 1.0E-8D) {
            return false;
        }

        ItemStack projectileStack = projectile.getItemStack().copy();
        ArrowEntity ricochet = new ArrowEntity(
                world,
                hit.getPos().x + reflected.x * 0.05D,
                hit.getPos().y + reflected.y * 0.05D,
                hit.getPos().z + reflected.z * 0.05D,
                projectileStack,
                null);
        ricochet.setOwner(owner);
        ricochet.setCritical(projectile.isCritical());
        ricochet.pickupType = state.infinite()
                ? PersistentProjectileEntity.PickupPermission.DISALLOWED
                : projectile.pickupType;
        ((PersistentProjectileEntityAccessor) ricochet)
                .fabricmmo$setPierceLevel(projectile.getPierceLevel());
        ItemStack weapon = projectile.getWeaponStack();
        if (weapon != null && !weapon.isEmpty()) {
            ((PersistentProjectileEntityAccessor) ricochet).fabricmmo$setWeapon(weapon.copy());
        }
        ricochet.setVelocity(reflected.x, reflected.y, reflected.z, 1.0F, 1.0F);
        RangedProjectileData.putRenewed(ricochet.getUuid(), state.bounced());
        world.spawnEntity(ricochet);
        RangedProjectileData.removeProjectile(projectile.getUuid());
        projectile.discard();
        return true;
    }

    private static Vec3d normal(Direction direction) {
        return new Vec3d(
                direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }
}
