package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.acrobatics.AcrobaticsRuntimeHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastDamage;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @ModifyVariable(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private float fabricmmo$modifySkillDamage(float damage, DamageSource source) {
        if (!FabricMmoFabricRuntime.running()) {
            return damage;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        float modified = victim instanceof ServerPlayerEntity player
                ? AcrobaticsRuntimeHandler.modifyCombatDamage(player, source, damage)
                : damage;
        if (!(source.getSource() instanceof TntEntity tnt)) {
            return modified;
        }
        MiningBlastRegistry.BlastData data = MiningBlastRegistry.find(tnt.getUuid()).orElse(null);
        if (data == null) {
            return modified;
        }
        if (victim.getUuid().equals(data.ownerId())) {
            return data.demolitionsExpertise()
                    ? MiningBlastDamage.ownerDamage(
                            modified,
                            FabricMmoFabricRuntime.miningSettings()
                                    .blastDamageDecreasePercent(data.rank()))
                    : modified;
        }
        return victim instanceof PlayerEntity
                ? MiningBlastDamage.bystanderDamage(modified)
                : modified;
    }
    @Inject(
            method = "modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
            at = @At("RETURN"),
            cancellable = true)
    private void fabricmmo$modifyAcrobaticsFallDamage(
            DamageSource source,
            float damage,
            CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof ServerPlayerEntity player) {
            callback.setReturnValue(AcrobaticsRuntimeHandler.modifyFallDamage(
                    player, source, callback.getReturnValueF()));
        }
    }

}
