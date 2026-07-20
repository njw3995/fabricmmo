package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Identifies zombified piglins created by portal random ticks. */
@Mixin(NetherPortalBlock.class)
abstract class NetherPortalBlockMixin {
    @Redirect(
            method = "randomTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityType;spawn("
                            + "Lnet/minecraft/server/world/ServerWorld;"
                            + "Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/entity/SpawnReason;)"
                            + "Lnet/minecraft/entity/Entity;"))
    private Entity fabricmmo$markNetherPortalOrigin(
            EntityType<?> type,
            ServerWorld world,
            BlockPos position,
            SpawnReason spawnReason) {
        Entity entity = type.spawn(world, position, spawnReason);
        if (entity != null) {
            CombatMobOrigin.mark(entity, CombatXpSettings.Origin.NETHER_PORTAL);
        }
        return entity;
    }
}
