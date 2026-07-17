package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/** Marks every world position occupied by a successful player block placement. */
public final class MiningPlacementTracker {
    private MiningPlacementTracker() {
    }

    public static void markPlacement(World world, BlockPos placedPos) {
        if (world.isClient() || !FabricMmoFabricRuntime.running()
                || (world instanceof ServerWorld serverWorld
                && FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld))) {
            return;
        }
        BlockState state = world.getBlockState(placedPos);
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.add(placedPos.toImmutable());
        addDoubleBlockPosition(state, placedPos, positions);
        addBedPosition(state, placedPos, positions);
        String worldId = world.getRegistryKey().getValue().toString();
        for (BlockPos pos : positions) {
            if (world.getBlockState(pos).getBlock() == state.getBlock()) {
                FabricMmoFabricRuntime.markPlayerPlaced(
                        new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ()));
            }
        }
    }

    private static void addDoubleBlockPosition(
            BlockState state, BlockPos pos, Set<BlockPos> positions) {
        if (!state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            return;
        }
        DoubleBlockHalf half = state.get(Properties.DOUBLE_BLOCK_HALF);
        positions.add((half == DoubleBlockHalf.LOWER ? pos.up() : pos.down()).toImmutable());
    }

    private static void addBedPosition(BlockState state, BlockPos pos, Set<BlockPos> positions) {
        if (!state.contains(Properties.BED_PART) || !state.contains(Properties.HORIZONTAL_FACING)) {
            return;
        }
        BedPart part = state.get(Properties.BED_PART);
        Direction facing = state.get(Properties.HORIZONTAL_FACING);
        positions.add((part == BedPart.FOOT ? pos.offset(facing) : pos.offset(facing.getOpposite()))
                .toImmutable());
    }
}
