package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningBlastRegistry;
import net.minecraft.entity.TntEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(TntEntity.class)
abstract class TntEntityMixin {
    @ModifyConstant(method = "explode()V", constant = @Constant(floatValue = 4.0F))
    private float fabricmmo$applyBiggerBombs(float vanillaPower) {
        TntEntity tnt = (TntEntity) (Object) this;
        MiningBlastRegistry.BlastData data = MiningBlastRegistry.find(tnt.getUuid()).orElse(null);
        if (data == null || !data.biggerBombs() || !FabricMmoFabricRuntime.running()) {
            return vanillaPower;
        }
        return (float) (vanillaPower
                + FabricMmoFabricRuntime.miningSettings().blastRadiusModifier(data.rank()));
    }
}
