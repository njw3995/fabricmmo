package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastDamage;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @ModifyVariable(
            method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private float fabricmmo$modifyBlastMiningDamage(float damage, DamageSource source) {
        if (!(source.getSource() instanceof TntEntity tnt) || !FabricMmoFabricRuntime.running()) {
            return damage;
        }
        MiningBlastRegistry.BlastData data = MiningBlastRegistry.find(tnt.getUuid()).orElse(null);
        if (data == null) {
            return damage;
        }
        LivingEntity victim = (LivingEntity) (Object) this;
        if (victim.getUuid().equals(data.ownerId())) {
            return data.demolitionsExpertise()
                    ? MiningBlastDamage.ownerDamage(
                            damage,
                            FabricMmoFabricRuntime.miningSettings()
                                    .blastDamageDecreasePercent(data.rank()))
                    : damage;
        }
        return victim instanceof PlayerEntity
                ? MiningBlastDamage.bystanderDamage(damage)
                : damage;
    }
}
