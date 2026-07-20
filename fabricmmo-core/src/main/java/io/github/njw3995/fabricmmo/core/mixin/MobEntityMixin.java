package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingRuntimeHandler;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Maps vanilla spawn reasons to persistent mcMMO combat-XP origin flags. */
@Mixin(MobEntity.class)
abstract class MobEntityMixin {
    @Inject(
            method = "initialize(Lnet/minecraft/world/ServerWorldAccess;"
                    + "Lnet/minecraft/world/LocalDifficulty;"
                    + "Lnet/minecraft/entity/SpawnReason;"
                    + "Lnet/minecraft/entity/EntityData;)Lnet/minecraft/entity/EntityData;",
            at = @At("HEAD"))
    private void fabricmmo$trackCombatXpOrigin(
            ServerWorldAccess world,
            LocalDifficulty difficulty,
            SpawnReason spawnReason,
            EntityData entityData,
            CallbackInfoReturnable<EntityData> callback) {
        CombatMobOrigin.mark(
                (MobEntity) (Object) this,
                CombatMobOrigin.fromSpawnReason(spawnReason));
    }
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void fabricmmo$protectFriendlyTargets(LivingEntity target, CallbackInfo ci) {
        if (target != null && !TamingRuntimeHandler.allowTarget((MobEntity) (Object) this, target)) {
            ci.cancel();
        }
    }

}
