package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import io.github.njw3995.fabricmmo.core.skill.taming.TamingRuntimeHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks vanilla breeding children before they enter the world. */
@Mixin(AnimalEntity.class)
abstract class AnimalEntityMixin {
    @Inject(
            method = "breed(Lnet/minecraft/server/world/ServerWorld;"
                    + "Lnet/minecraft/entity/passive/AnimalEntity;"
                    + "Lnet/minecraft/entity/passive/PassiveEntity;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void fabricmmo$preventCotwBreeding(
            ServerWorld world,
            AnimalEntity mate,
            PassiveEntity child,
            CallbackInfo ci) {
        if (!TamingRuntimeHandler.allowBreeding((AnimalEntity) (Object) this, mate)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "breed(Lnet/minecraft/server/world/ServerWorld;"
                    + "Lnet/minecraft/entity/passive/AnimalEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;"
                            + "spawnEntityAndPassengers(Lnet/minecraft/entity/Entity;)V"))
    private void fabricmmo$markBredOrigin(ServerWorld world, Entity child) {
        CombatMobOrigin.mark(child, CombatXpSettings.Origin.BRED);
        world.spawnEntityAndPassengers(child);
    }
}
