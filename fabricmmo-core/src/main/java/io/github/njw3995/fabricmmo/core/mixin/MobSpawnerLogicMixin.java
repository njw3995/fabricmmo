package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Covers custom-NBT spawner entities that skip MobEntity.initialize. */
@Mixin(MobSpawnerLogic.class)
abstract class MobSpawnerLogicMixin {
    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;"
                            + "spawnNewEntityAndPassengers(Lnet/minecraft/entity/Entity;)Z"))
    private boolean fabricmmo$markSpawnerOrigin(ServerWorld world, Entity entity) {
        CombatMobOrigin.mark(entity, CombatXpSettings.Origin.SPAWNER);
        return world.spawnNewEntityAndPassengers(entity);
    }
}
