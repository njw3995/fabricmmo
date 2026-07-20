package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.skill.combat.CombatMobOrigin;
import io.github.njw3995.fabricmmo.core.skill.combat.CombatXpSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Marks chickens hatched by thrown eggs with upstream's EGG combat-XP origin. */
@Mixin(EggEntity.class)
abstract class EggEntityMixin {
    @Redirect(
            method = "onCollision",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;"
                            + "spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean fabricmmo$markThrownEggOrigin(World world, Entity entity) {
        CombatMobOrigin.mark(entity, CombatXpSettings.Origin.EGG);
        return world.spawnEntity(entity);
    }
}
