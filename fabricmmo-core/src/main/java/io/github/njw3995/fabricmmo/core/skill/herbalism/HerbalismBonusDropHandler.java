package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Applies Double Drops, Verdant Bounty, and Green Terra to Herbalism drops. */
public final class HerbalismBonusDropHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private HerbalismBonusDropHandler() {
    }

    public static List<ItemStack> apply(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool,
            List<ItemStack> vanillaDrops) {
        if (!(entity instanceof ServerPlayerEntity player)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return vanillaDrops;
        }
        HerbalismBlockBreakHandler.awardBeforeDrops(world, player, pos, state, tool);
        if (vanillaDrops.isEmpty()
                || !HerbalismBlockBreakHandler.eligibleForBonusDrops(world, player, pos, state)) {
            return vanillaDrops;
        }
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        if (!FabricMmoFabricRuntime.herbalismDropSettings().materialEnabled(blockId)) {
            return vanillaDrops;
        }
        boolean skillPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.HERBALISM, true);
        if (!skillPermission
                || !PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.HERBALISM_DOUBLE_DROPS,
                        skillPermission)) {
            return vanillaDrops;
        }
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (!api.protection().canBreak(
                player.getUuid(), world.getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ())) {
            return vanillaDrops;
        }
        int level = api.progression().query(player.getUuid(), CoreSkills.HERBALISM).level();
        boolean lucky = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.HERBALISM_LUCKY, false);
        HerbalismDropOutcome outcome = new HerbalismDropCalculator(
                world.getRandom()::nextDouble,
                FabricMmoFabricRuntime.herbalismSettings()).roll(
                        level, lucky, HerbalismAbilityHandler.isActive(player.getUuid()));
        if (outcome == HerbalismDropOutcome.NONE) {
            return vanillaDrops;
        }
        ArrayList<ItemStack> result = new ArrayList<>(
                vanillaDrops.size() * (1 + outcome.bonusCopies()));
        result.addAll(vanillaDrops);
        for (int copy = 0; copy < outcome.bonusCopies(); copy++) {
            for (ItemStack stack : vanillaDrops) {
                result.add(stack.copy());
            }
        }
        return List.copyOf(result);
    }
}
