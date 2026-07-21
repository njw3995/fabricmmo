package io.github.njw3995.fabricmmo.core.skill.repair;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** Corrects client-side armor quick-equip prediction after a utility-anvil item use is denied. */
public final class UtilityAnvilInventorySynchronizer {
    private UtilityAnvilInventorySynchronizer() {
    }

    public static void resync(ServerPlayerEntity player) {
        syncNow(player);

        MinecraftServer server = player.getServer();
        if (server != null) {
            server.execute(() -> {
                if (!player.isRemoved()) {
                    syncNow(player);
                }
            });
        }
    }

    private static void syncNow(ServerPlayerEntity player) {
        player.getInventory().markDirty();
        player.playerScreenHandler.syncState();
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.currentScreenHandler.syncState();
        }
    }
}
