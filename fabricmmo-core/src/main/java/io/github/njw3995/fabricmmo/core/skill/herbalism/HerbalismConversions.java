package io.github.njw3995.fabricmmo.core.skill.herbalism;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

/** Green Thumb, Green Terra, and Shroom Thumb block conversions. */
public final class HerbalismConversions {
    private HerbalismConversions() {
    }

    /** Blocks that upstream allows to ready Green Terra from a right-click interaction. */
    public static boolean canPrepareGreenTerra(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT
                || block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT_PATH
                || block == Blocks.FARMLAND;
    }

    public static boolean canGreen(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.COBBLESTONE_WALL
                || block == Blocks.STONE_BRICKS
                || block == Blocks.DIRT
                || block == Blocks.DIRT_PATH
                || block == Blocks.COBBLESTONE;
    }

    public static boolean convertGreen(ServerWorld world, BlockPos pos, BlockState state) {
        Block replacement;
        Block block = state.getBlock();
        if (block == Blocks.COBBLESTONE_WALL) {
            replacement = Blocks.MOSSY_COBBLESTONE_WALL;
        } else if (block == Blocks.STONE_BRICKS) {
            replacement = Blocks.MOSSY_STONE_BRICKS;
        } else if (block == Blocks.DIRT || block == Blocks.DIRT_PATH) {
            replacement = Blocks.GRASS_BLOCK;
        } else if (block == Blocks.COBBLESTONE) {
            replacement = Blocks.MOSSY_COBBLESTONE;
        } else {
            return false;
        }
        BlockState replacementState = copySharedProperties(state, replacement.getDefaultState());
        return world.setBlockState(pos, replacementState, Block.NOTIFY_ALL);
    }

    public static boolean canShroom(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.DIRT_PATH;
    }

    public static boolean convertShroom(ServerWorld world, BlockPos pos, BlockState state) {
        if (!canShroom(state)) {
            return false;
        }
        return world.setBlockState(pos, Blocks.MYCELIUM.getDefaultState(), Block.NOTIFY_ALL);
    }

    private static BlockState copySharedProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Property<?> sourceProperty : source.getProperties()) {
            Property<?> targetProperty = target.getBlock().getStateManager()
                    .getProperty(sourceProperty.getName());
            if (targetProperty == null || targetProperty.getType() != sourceProperty.getType()) {
                continue;
            }
            result = copyProperty(source, result, sourceProperty, targetProperty);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyProperty(
            BlockState source,
            BlockState target,
            Property sourceProperty,
            Property targetProperty) {
        Comparable value = source.get(sourceProperty);
        return target.with(targetProperty, value);
    }
}
