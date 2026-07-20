package io.github.njw3995.fabricmmo.core.skill.smelting;

import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

/** Assigns ownership on player inventory clicks, matching upstream InventoryClickEvent behavior. */
public final class ProcessingBlockOwnershipHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private ProcessingBlockOwnershipHandler() {
    }

    public static void inventoryClicked(ScreenHandler handler, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        BlockEntity blockEntity = findProcessingBlock(handler);
        if (!(blockEntity instanceof OwnedProcessingBlock owned)) {
            return;
        }
        boolean permitted;
        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            permitted = PERMISSIONS.hasPermission(
                    serverPlayer.getCommandSource(), PermissionNodes.SMELTING, true);
        } else if (blockEntity instanceof BrewingStandBlockEntity) {
            permitted = PERMISSIONS.hasPermission(
                    serverPlayer.getCommandSource(), PermissionNodes.skill("alchemy"), true);
        } else {
            return;
        }
        if (permitted) {
            owned.fabricmmo$setOwner(serverPlayer.getUuid());
            blockEntity.markDirty();
        }
    }

    private static BlockEntity findProcessingBlock(ScreenHandler handler) {
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof AbstractFurnaceBlockEntity furnace) {
                return furnace;
            }
            if (slot.inventory instanceof BrewingStandBlockEntity brewingStand) {
                return brewingStand;
            }
        }
        return null;
    }
}
