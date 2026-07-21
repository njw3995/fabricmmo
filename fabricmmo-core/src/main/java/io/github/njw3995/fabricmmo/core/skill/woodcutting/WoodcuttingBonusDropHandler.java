package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
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
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Applies Harvest Lumber and Clean Cuts to ordinary Woodcutting drops. */
public final class WoodcuttingBonusDropHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private WoodcuttingBonusDropHandler() {
    }

    public static List<ItemStack> apply(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool,
            List<ItemStack> vanillaDrops) {
        if (TreeFellerContext.active()
                || !(entity instanceof ServerPlayerEntity player)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return vanillaDrops;
        }
        WoodcuttingBlockBreakHandler.awardBeforeDrops(world, player, pos, state, tool);
        if (vanillaDrops.isEmpty()) {
            return vanillaDrops;
        }
        WoodcuttingDropOutcome outcome = roll(world, player, pos, state, tool);
        if (outcome == WoodcuttingDropOutcome.NONE) {
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

    static WoodcuttingDropOutcome roll(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {
        NamespacedId sourceId = WoodcuttingBlockClassifier.id(state);
        var extension = FabricMmoFabricRuntime.gatheringContentFor(CoreSkills.WOODCUTTING, state);
        boolean validTool = extension
                .map(definition -> FabricMmoFabricRuntime.gatheringContentResolver()
                        .validTool(definition, tool))
                .orElseGet(() -> tool.isIn(ItemTags.AXES));
        boolean bonusDrops = extension.map(definition -> definition.bonusDrops())
                .orElseGet(() -> FabricMmoFabricRuntime.woodcuttingDropSettings()
                        .materialEnabled(sourceId));
        if (FabricMmoFabricRuntime.woodcuttingXpFor(state) <= 0
                || !bonusDrops
                || player.isCreative()
                || !validTool) {
            return WoodcuttingDropOutcome.NONE;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        if (TreeFellerContext.startingBreak(player.getUuid(), location)) {
            return WoodcuttingDropOutcome.NONE;
        }
        boolean skillPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.WOODCUTTING, true);
        if (!skillPermission
                || (FabricMmoFabricRuntime.isPlayerPlaced(location)
                        && extension.map(definition -> definition.naturalBlocksOnly()).orElse(true))) {
            return WoodcuttingDropOutcome.NONE;
        }
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (!api.protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ())) {
            return WoodcuttingDropOutcome.NONE;
        }
        boolean harvestPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(),
                PermissionNodes.WOODCUTTING_HARVEST_LUMBER,
                skillPermission);
        boolean cleanCutsPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(),
                PermissionNodes.WOODCUTTING_CLEAN_CUTS,
                skillPermission);
        boolean lucky = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.WOODCUTTING_LUCKY, false);
        int level = api.progression().query(player.getUuid(), CoreSkills.WOODCUTTING).level();
        WoodcuttingDropSettings settings = FabricMmoFabricRuntime.woodcuttingDropSettings();
        return new WoodcuttingDropCalculator(world.getRandom()::nextDouble, settings).roll(
                new WoodcuttingDropContext(
                        level,
                        FabricMmoFabricRuntime.woodcuttingSettings().progressionMode(),
                        harvestPermission,
                        cleanCutsPermission,
                        lucky));
    }
}
