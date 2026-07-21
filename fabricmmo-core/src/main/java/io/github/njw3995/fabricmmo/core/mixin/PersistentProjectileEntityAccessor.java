package io.github.njw3995.fabricmmo.core.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {
    @Accessor("weapon")
    ItemStack fabricmmo$getWeapon();

    @Accessor("weapon")
    void fabricmmo$setWeapon(ItemStack weapon);

    @Invoker("setPierceLevel")
    void fabricmmo$setPierceLevel(byte pierceLevel);
}
