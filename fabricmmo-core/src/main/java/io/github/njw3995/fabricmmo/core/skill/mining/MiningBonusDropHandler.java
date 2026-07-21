package io.github.njw3995.fabricmmo.core.skill.mining;

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
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class MiningBonusDropHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private MiningBonusDropHandler() {
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
                || !FabricMmoFabricRuntime.running()) {
            return vanillaDrops;
        }

        if (FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return vanillaDrops;
        }

        // Upstream awards Mining XP before rolling Double Drops and Mother Lode, so a level
        // reached by this block affects this block's bonus-drop chance. The AFTER event remains
        // as a fallback for successful breaks whose block implementation does not spawn drops.
        MiningBlockBreakHandler.awardBeforeDrops(world, player, pos, state, tool);
        if (vanillaDrops.isEmpty()) {
            return vanillaDrops;
        }

        MiningDropSettings settings = FabricMmoFabricRuntime.miningDropSettings();
        NamespacedId sourceId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        var extension = FabricMmoFabricRuntime.gatheringContentFor(CoreSkills.MINING, state);
        boolean validTool = extension
                .map(definition -> FabricMmoFabricRuntime.gatheringContentResolver()
                        .validTool(definition, tool))
                .orElseGet(() -> tool.isIn(ItemTags.PICKAXES) || tool.isIn(ItemTags.HOES));
        boolean skillPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.MINING, true);
        boolean doubleDropsPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.MINING_DOUBLE_DROPS, skillPermission);
        boolean motherLodePermission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.MINING_MOTHER_LODE, skillPermission);
        boolean lucky = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.MINING_LUCKY, false);
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        boolean protectionAllowed = api.protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ());
        int skillLevel = api.progression().query(player.getUuid(), CoreSkills.MINING).level();
        boolean silkTouch = EnchantmentHelper.getLevel(
                world.getRegistryManager()
                        .get(RegistryKeys.ENCHANTMENT)
                        .entryOf(Enchantments.SILK_TOUCH),
                tool) > 0;

        MiningBonusDropDecision decision = MiningBonusDropDecision.evaluate(
                skillLevel,
                FabricMmoFabricRuntime.miningSettings().progressionMode(),
                settings,
                player.isCreative(),
                validTool,
                skillPermission,
                protectionAllowed,
                FabricMmoFabricRuntime.isPlayerPlaced(location)
                        && extension.map(definition -> definition.naturalBlocksOnly()).orElse(true),
                extension.map(definition -> definition.bonusDrops())
                        .orElseGet(() -> settings.materialEnabled(sourceId)),
                silkTouch,
                doubleDropsPermission,
                motherLodePermission,
                FabricMmoFabricRuntime.isSuperBreakerActive(player.getUuid()),
                lucky);
        if (!decision.eligible()) {
            return vanillaDrops;
        }

        MiningDropCalculator calculator = new MiningDropCalculator(
                world.getRandom()::nextDouble, settings);
        MiningDropOutcome outcome = calculator.roll(decision.context());
        if (outcome == MiningDropOutcome.NONE) {
            return vanillaDrops;
        }

        List<MiningDropStack> descriptors = vanillaDrops.stream()
                .map(stack -> new MiningDropStack(
                        NamespacedId.parse(Registries.ITEM.getId(stack.getItem()).toString()),
                        stack.getCount(),
                        stack.getMaxCount(),
                        stack.getItem() instanceof BlockItem))
                .toList();
        List<MiningDropStack> multiplied = MiningDropQuantityModifier.apply(
                descriptors, settings, outcome);
        List<ItemStack> result = new ArrayList<>(vanillaDrops.size());
        for (int index = 0; index < vanillaDrops.size(); index++) {
            ItemStack original = vanillaDrops.get(index);
            int count = multiplied.get(index).count();
            if (count == original.getCount()) {
                result.add(original);
            } else {
                ItemStack copy = original.copy();
                copy.setCount(count);
                result.add(copy);
            }
        }
        return List.copyOf(result);
    }
}
