package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Applies mcMMO's LavaStoneAndCobbleFarming rule to formed Mining-XP blocks. */
public final class MiningBlockFormationTracker {
    private MiningBlockFormationTracker() {
    }

    public static void markFormedBlock(ServerWorld world, BlockPos pos, BlockState formed) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)
                || !FabricMmoFabricRuntime.miningSettings().lavaStoneExploitPrevention()
                || formed.isOf(Blocks.OBSIDIAN)) {
            return;
        }
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(formed.getBlock()).toString());
        if (FabricMmoFabricRuntime.miningXpFor(formed) <= 0) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        FabricMmoFabricRuntime.markPlayerPlaced(new BlockLocation(
                worldId, pos.getX(), pos.getY(), pos.getZ()));
    }
}
