package io.github.njw3995.fabricmmo.core.skill.mining;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Applies and removes Super Breaker's temporary Efficiency increase.
 * The original level is stored on the item so copied, dropped, or saved stacks can be repaired.
 */
final class SuperBreakerAttributeBoost {
    private static final String ORIGINAL_EFFICIENCY_KEY =
            "fabricmmo_super_breaker_original_efficiency";
    private static final Map<UUID, BoostedTool> BOOSTED_TOOLS = new HashMap<>();

    private SuperBreakerAttributeBoost() {
    }

    static synchronized void update(ServerPlayerEntity player, int bonusLevels) {
        UUID playerId = player.getUuid();
        ItemStack held = player.getMainHandStack();
        RegistryEntry<Enchantment> efficiency = efficiency(player);
        BoostedTool existing = BOOSTED_TOOLS.get(playerId);
        boolean changed = false;

        if (existing != null && existing.stack() != held) {
            changed |= restoreTrackedStack(existing);
            BOOSTED_TOOLS.remove(playerId);
            existing = null;
        }

        changed |= restoreMarkedStacks(player, efficiency, held);

        if (bonusLevels <= 0 || held.isEmpty() || !held.isIn(ItemTags.PICKAXES)) {
            existing = BOOSTED_TOOLS.remove(playerId);
            if (existing != null) {
                changed |= restoreTrackedStack(existing);
            }
            changed |= restoreMarkedStack(held, efficiency);
            if (changed) {
                syncInventory(player);
            }
            return;
        }

        if (existing != null && existing.stack() == held) {
            if (hasOriginalEfficiencyMarker(held)
                    && EnchantmentHelper.getLevel(efficiency, held) == existing.boostedLevel()) {
                if (changed) {
                    syncInventory(player);
                }
                return;
            }

            if (!restoreMarkedStack(held, efficiency)) {
                setEfficiency(held, efficiency, existing.originalLevel());
            }
            BOOSTED_TOOLS.remove(playerId);
            changed = true;
        } else {
            changed |= restoreMarkedStack(held, efficiency);
        }

        int originalLevel = EnchantmentHelper.getLevel(efficiency, held);
        int boostedLevel = Math.addExact(originalLevel, bonusLevels);
        markOriginalEfficiency(held, originalLevel);
        setEfficiency(held, efficiency, boostedLevel);
        BOOSTED_TOOLS.put(playerId,
                new BoostedTool(held, efficiency, originalLevel, boostedLevel));
        syncInventory(player);
    }

    static synchronized void clear(ServerPlayerEntity player) {
        RegistryEntry<Enchantment> efficiency = efficiency(player);
        BoostedTool existing = BOOSTED_TOOLS.remove(player.getUuid());
        boolean changed = existing != null && restoreTrackedStack(existing);
        changed |= restoreMarkedStacks(player, efficiency, null);
        if (changed) {
            syncInventory(player);
        }
    }

    static synchronized void restoreDroppedStack(
            ServerPlayerEntity player,
            ItemStack droppedStack) {
        RegistryEntry<Enchantment> efficiency = efficiency(player);
        BoostedTool existing = BOOSTED_TOOLS.remove(player.getUuid());
        boolean changed = existing != null && restoreTrackedStack(existing);
        changed |= restoreMarkedStack(droppedStack, efficiency);
        changed |= restoreMarkedStacks(player, efficiency, null);
        if (changed) {
            syncInventory(player);
        }
    }

    static synchronized boolean restoreForRepair(
            ServerPlayerEntity player,
            ItemStack repairedStack) {
        RegistryEntry<Enchantment> efficiency = efficiency(player);
        BoostedTool existing = BOOSTED_TOOLS.get(player.getUuid());
        boolean changed = false;
        if (existing != null && existing.stack() == repairedStack) {
            BOOSTED_TOOLS.remove(player.getUuid());
            changed |= restoreTrackedStack(existing);
        }
        changed |= restoreMarkedStack(repairedStack, efficiency);
        if (changed) {
            syncInventory(player);
        }
        return changed;
    }

    static synchronized void cleanupInventory(ServerPlayerEntity player) {
        RegistryEntry<Enchantment> efficiency = efficiency(player);
        BoostedTool existing = BOOSTED_TOOLS.remove(player.getUuid());
        boolean changed = existing != null && restoreTrackedStack(existing);
        changed |= restoreMarkedStacks(player, efficiency, null);
        if (changed) {
            syncInventory(player);
        }
    }

    static synchronized void reset() {
        for (BoostedTool boostedTool : BOOSTED_TOOLS.values()) {
            restoreTrackedStack(boostedTool);
        }
        BOOSTED_TOOLS.clear();
    }

    private static RegistryEntry<Enchantment> efficiency(ServerPlayerEntity player) {
        return player.getServerWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .entryOf(Enchantments.EFFICIENCY);
    }

    private static boolean restoreTrackedStack(BoostedTool boostedTool) {
        if (restoreMarkedStack(boostedTool.stack(), boostedTool.efficiency())) {
            return true;
        }
        if (boostedTool.stack().isEmpty()) {
            return false;
        }
        int currentLevel = EnchantmentHelper.getLevel(
                boostedTool.efficiency(), boostedTool.stack());
        if (currentLevel != boostedTool.boostedLevel()) {
            return false;
        }
        setEfficiency(
                boostedTool.stack(), boostedTool.efficiency(), boostedTool.originalLevel());
        return true;
    }

    private static boolean restoreMarkedStacks(
            ServerPlayerEntity player,
            RegistryEntry<Enchantment> efficiency,
            ItemStack excluded) {
        Set<ItemStack> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            changed |= restoreOnce(
                    player.getInventory().getStack(slot), efficiency, excluded, visited);
        }
        for (Slot slot : player.currentScreenHandler.slots) {
            changed |= restoreOnce(slot.getStack(), efficiency, excluded, visited);
        }
        changed |= restoreOnce(
                player.currentScreenHandler.getCursorStack(), efficiency, excluded, visited);
        return changed;
    }

    private static boolean restoreOnce(
            ItemStack stack,
            RegistryEntry<Enchantment> efficiency,
            ItemStack excluded,
            Set<ItemStack> visited) {
        return stack != excluded
                && visited.add(stack)
                && restoreMarkedStack(stack, efficiency);
    }

    private static void markOriginalEfficiency(ItemStack stack, int originalLevel) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack,
                nbt -> nbt.putInt(ORIGINAL_EFFICIENCY_KEY, originalLevel));
    }

    private static boolean hasOriginalEfficiencyMarker(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .contains(ORIGINAL_EFFICIENCY_KEY);
    }

    private static boolean restoreMarkedStack(
            ItemStack stack,
            RegistryEntry<Enchantment> efficiency) {
        if (stack.isEmpty()) {
            return false;
        }
        NbtComponent component = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound customData = component.copyNbt();
        if (!customData.contains(ORIGINAL_EFFICIENCY_KEY)) {
            return false;
        }

        int originalLevel = customData.getInt(ORIGINAL_EFFICIENCY_KEY);
        setEfficiency(stack, efficiency, originalLevel);
        customData.remove(ORIGINAL_EFFICIENCY_KEY);
        if (customData.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        }
        return true;
    }

    private static void setEfficiency(
            ItemStack stack,
            RegistryEntry<Enchantment> efficiency,
            int level) {
        EnchantmentHelper.apply(stack, builder -> {
            if (level > 0) {
                builder.set(efficiency, level);
            } else {
                builder.remove(entry -> entry.equals(efficiency));
            }
        });
    }

    private static void syncInventory(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }

    private record BoostedTool(
            ItemStack stack,
            RegistryEntry<Enchantment> efficiency,
            int originalLevel,
            int boostedLevel) {
    }
}
