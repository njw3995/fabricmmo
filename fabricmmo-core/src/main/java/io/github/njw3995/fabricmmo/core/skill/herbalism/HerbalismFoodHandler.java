package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

/** Platform-equivalent Farmer's Diet hook for successful food consumption. */
public final class HerbalismFoodHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<Item> FARMER_FOODS = Set.of(
            Items.GLOW_BERRIES,
            Items.BAKED_POTATO,
            Items.BEETROOT,
            Items.BREAD,
            Items.CARROT,
            Items.GOLDEN_CARROT,
            Items.MUSHROOM_STEW,
            Items.PUMPKIN_PIE,
            Items.COOKIE,
            Items.MELON_SLICE,
            Items.POISONOUS_POTATO,
            Items.POTATO);
    private static final Map<UUID, PendingFood> PENDING = new HashMap<>();

    private HerbalismFoodHandler() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient()
                    || hand != Hand.MAIN_HAND
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()
                    || serverPlayer.isCreative()
                    || !FARMER_FOODS.contains(stack.getItem())
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(), PermissionNodes.HERBALISM, true)
                    || !PERMISSIONS.hasPermission(
                            serverPlayer.getCommandSource(),
                            PermissionNodes.HERBALISM_FARMERS_DIET,
                            true)) {
                return TypedActionResult.pass(stack);
            }
            int level = FabricMmoFabricRuntime.requireApi().progression()
                    .query(serverPlayer.getUuid(), CoreSkills.HERBALISM).level();
            int rank = FabricMmoFabricRuntime.herbalismSettings().farmersDietRank(level);
            if (rank > 0) {
                synchronized (PENDING) {
                    PENDING.put(serverPlayer.getUuid(), new PendingFood(
                            serverPlayer.getHungerManager().getFoodLevel(), rank, 64));
                }
            }
            return TypedActionResult.pass(stack);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            synchronized (PENDING) {
                PENDING.entrySet().removeIf(entry -> {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                    if (player == null) {
                        return true;
                    }
                    PendingFood pending = entry.getValue();
                    int current = player.getHungerManager().getFoodLevel();
                    if (current > pending.foodBefore()) {
                        player.getHungerManager().setFoodLevel(Math.min(20, current + pending.rank()));
                        return true;
                    }
                    if (pending.ticksRemaining() <= 1) {
                        return true;
                    }
                    entry.setValue(new PendingFood(
                            pending.foodBefore(), pending.rank(), pending.ticksRemaining() - 1));
                    return false;
                });
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            synchronized (PENDING) {
                PENDING.remove(handler.player.getUuid());
            }
        });
    }

    public static void reset() {
        synchronized (PENDING) {
            PENDING.clear();
        }
    }

    private record PendingFood(int foodBefore, int rank, int ticksRemaining) {
    }
}
