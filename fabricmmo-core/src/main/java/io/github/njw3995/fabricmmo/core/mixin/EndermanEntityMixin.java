package io.github.njw3995.fabricmmo.core.mixin;

import io.github.njw3995.fabricmmo.core.block.TravelingBlockCarrier;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EndermanEntity.class)
abstract class EndermanEntityMixin implements TravelingBlockCarrier {
    @Unique
    private boolean fabricmmo$carryingIneligibleBlock;

    @Override
    public boolean fabricmmo$isCarryingIneligibleBlock() {
        return fabricmmo$carryingIneligibleBlock;
    }

    @Override
    public void fabricmmo$setCarryingIneligibleBlock(boolean value) {
        fabricmmo$carryingIneligibleBlock = value;
    }
}
