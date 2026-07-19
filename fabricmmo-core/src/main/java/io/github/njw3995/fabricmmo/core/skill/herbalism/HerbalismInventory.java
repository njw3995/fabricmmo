package io.github.njw3995.fabricmmo.core.skill.herbalism;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

final class HerbalismInventory {
    private HerbalismInventory() {
    }

    static boolean contains(ServerPlayerEntity player, Item item) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    static boolean removeOne(ServerPlayerEntity player, Item item) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty() && stack.isOf(item)) {
                if (!player.isCreative()) {
                    stack.decrement(1);
                }
                return true;
            }
        }
        return false;
    }
}
