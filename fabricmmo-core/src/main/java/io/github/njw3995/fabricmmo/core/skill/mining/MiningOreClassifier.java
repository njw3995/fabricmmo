package io.github.njw3995.fabricmmo.core.skill.mining;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

public final class MiningOreClassifier {
    private MiningOreClassifier() {
    }

    public static boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES)
                || state.isOf(Blocks.NETHER_QUARTZ_ORE)
                || state.isOf(Blocks.NETHER_GOLD_ORE)
                || state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    public static boolean illegalDrop(BlockState state) {
        return state.isOf(Blocks.SPAWNER)
                || state.isOf(Blocks.BUDDING_AMETHYST)
                || state.isOf(Blocks.INFESTED_STONE)
                || state.isOf(Blocks.INFESTED_COBBLESTONE)
                || state.isOf(Blocks.INFESTED_STONE_BRICKS)
                || state.isOf(Blocks.INFESTED_CRACKED_STONE_BRICKS)
                || state.isOf(Blocks.INFESTED_CHISELED_STONE_BRICKS)
                || state.isOf(Blocks.INFESTED_MOSSY_STONE_BRICKS)
                || state.isOf(Blocks.INFESTED_DEEPSLATE);
    }
}
