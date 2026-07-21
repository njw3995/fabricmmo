package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.gathering.ConfiguredBlockXpTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Executes the recursive Tree Feller break while preserving per-block protection callbacks. */
public final class WoodcuttingTreeFellerProcessor {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private WoodcuttingTreeFellerProcessor() {
    }

    public static Result process(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos startingPos) {
        WoodcuttingSettings settings = FabricMmoFabricRuntime.woodcuttingSettings();
        ConfiguredBlockXpTable xpTable = FabricMmoFabricRuntime.woodcuttingXpTable();
        String worldId = world.getRegistryKey().getValue().toString();
        TreeFellerSearch search = new TreeFellerSearch(new TreeFellerSearch.BlockAccess() {
            @Override
            public TreeFellerSearch.Kind kind(TreeFellerSearch.Node node) {
                return WoodcuttingBlockClassifier.kind(
                        world.getBlockState(pos(node)), xpTable);
            }

            @Override
            public boolean eligible(TreeFellerSearch.Node node) {
                BlockPos pos = pos(node);
                BlockLocation location = new BlockLocation(
                        worldId, pos.getX(), pos.getY(), pos.getZ());
                return !FabricMmoFabricRuntime.isPlayerPlaced(location);
            }
        }, settings.treeFellerThreshold());
        TreeFellerSearch.Result searchResult = search.search(node(startingPos));
        Set<TreeFellerSearch.Node> nodes = searchResult.blocks();
        if (nodes.isEmpty()) {
            return new Result(0, 0, searchResult.thresholdReached(), false);
        }

        ItemStack tool = player.getMainHandStack();
        int logCount = (int) nodes.stream()
                .map(WoodcuttingTreeFellerProcessor::pos)
                .map(world::getBlockState)
                .filter(state -> WoodcuttingBlockClassifier.kind(state, xpTable) == TreeFellerSearch.Kind.WOOD_XP)
                .count();
        if (!applyDurability(tool, player, logCount, settings.abilityToolDamage())) {
            player.sendMessage(WoodcuttingMessages.splinter(), true);
            float health = player.getHealth();
            if (health > 1.0F) {
                int bound = Math.max(1, (int) (health - 1.0F));
                int damage = world.getRandom().nextInt(bound);
                if (damage > 0) {
                    player.damage(player.getDamageSources().generic(), damage);
                }
            }
            return new Result(0, 0, searchResult.thresholdReached(), true);
        }

        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        int totalXp = 0;
        int processedLogs = 0;
        int brokenBlocks = 0;
        boolean lucky = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.WOODCUTTING_LUCKY, false);
        boolean knockOnWood = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.WOODCUTTING_KNOCK_ON_WOOD, true);
        int level = api.progression().query(
                player.getUuid(), io.github.njw3995.fabricmmo.core.skill.CoreSkills.WOODCUTTING).level();
        boolean knockRankOne = knockOnWood && level >= settings.knockOnWoodRankOneLevel();
        boolean knockRankTwo = knockOnWood && level >= settings.knockOnWoodRankTwoLevel();

        TreeFellerContext context = TreeFellerContext.begin();
        try {
            for (TreeFellerSearch.Node node : nodes) {
                BlockPos pos = pos(node);
                BlockState state = world.getBlockState(pos);
                TreeFellerSearch.Kind kind = WoodcuttingBlockClassifier.kind(state, xpTable);
                if (kind == TreeFellerSearch.Kind.OTHER) {
                    continue;
                }
                if (!api.protection().canBreak(
                        player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ())) {
                    continue;
                }
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                        world, player, pos, state, blockEntity)) {
                    PlayerBlockBreakEvents.CANCELED.invoker().onBlockBreakCanceled(
                            world, player, pos, state, blockEntity);
                    continue;
                }
                List<ItemStack> vanillaDrops = Block.getDroppedStacks(
                        state, world, pos, blockEntity, player, tool);
                List<ItemStack> drops = new ArrayList<>();
                int blockXp = 0;
                int orbXp = 0;
                if (kind == TreeFellerSearch.Kind.WOOD_XP) {
                    int rawXp = FabricMmoFabricRuntime.woodcuttingXpFor(state);
                    if (rawXp > 0) {
                        blockXp = settings.treeFellerReducedXp()
                                ? Math.max(1, rawXp - processedLogs * 5)
                                : rawXp;
                    }
                    drops.addAll(vanillaDrops);
                    WoodcuttingDropOutcome outcome = WoodcuttingBonusDropHandler.roll(
                            world, player, pos, state, tool);
                    for (int copy = 0; copy < outcome.bonusCopies(); copy++) {
                        vanillaDrops.forEach(stack -> drops.add(stack.copy()));
                    }
                } else {
                    if (world.getRandom().nextInt(100) > 75) {
                        drops.addAll(vanillaDrops);
                    } else if (knockRankOne) {
                        vanillaDrops.stream()
                                .filter(WoodcuttingTreeFellerProcessor::isSaplingOrPropagule)
                                .forEach(drops::add);
                    }
                }
                if (WoodcuttingBlockClassifier.isNonWoodTreePart(state)
                        && knockRankTwo
                        && settings.knockOnWoodXpOrbs()
                        && world.getRandom().nextDouble()
                                < Math.min(1.0D, lucky ? 0.10D * WoodcuttingProbability.LUCKY_MODIFIER
                                        : 0.10D)) {
                    orbXp = Math.max(1, world.getRandom().nextInt(100));
                }

                if (!world.breakBlock(pos, false, player, 512)) {
                    continue;
                }
                for (ItemStack drop : drops) {
                    spawnAtBlockCenter(world, pos, drop);
                }
                if (orbXp > 0) {
                    ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), orbXp);
                }
                PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(
                        world, player, pos, state, blockEntity);
                FabricMmoFabricRuntime.clearPlayerPlaced(new BlockLocation(
                        worldId, pos.getX(), pos.getY(), pos.getZ()));
                brokenBlocks++;
                if (blockXp > 0) {
                    totalXp += blockXp;
                    processedLogs++;
                }
            }
        } finally {
            context.close();
        }
        if (totalXp > 0) {
            WoodcuttingBlockBreakHandler.awardTreeFellerTotal(
                    world, player, startingPos, totalXp, processedLogs);
        }
        return new Result(brokenBlocks, totalXp, searchResult.thresholdReached(), false);
    }

    private static void spawnAtBlockCenter(
            ServerWorld world,
            BlockPos pos,
            ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(
                world,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack);
        item.setToDefaultPickupDelay();
        world.spawnEntity(item);
    }

    private static boolean applyDurability(
            ItemStack tool,
            ServerPlayerEntity player,
            int logCount,
            int damagePerLog) {
        int damage = Math.max(0, logCount * damagePerLog);
        if (damage == 0 || !tool.isDamageable()) {
            return true;
        }
        boolean survives = tool.getDamage() + damage < tool.getMaxDamage();
        tool.damage(damage, player, EquipmentSlot.MAINHAND);
        return survives;
    }

    private static boolean isSaplingOrPropagule(ItemStack stack) {
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.contains("sapling") || path.contains("propagule");
    }

    private static TreeFellerSearch.Node node(BlockPos pos) {
        return new TreeFellerSearch.Node(pos.getX(), pos.getY(), pos.getZ());
    }

    private static BlockPos pos(TreeFellerSearch.Node node) {
        return new BlockPos(node.x(), node.y(), node.z());
    }

    public record Result(
            int brokenBlocks,
            int awardedXp,
            boolean thresholdReached,
            boolean toolSplintered) {
    }
}
