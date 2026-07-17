package io.github.njw3995.fabricmmo.core.block;

import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Captures every block position modified by one successful BlockItem placement. */
public final class BlockPlacementCapture {
    private static final ThreadLocal<Session> ACTIVE = new ThreadLocal<>();

    private BlockPlacementCapture() {
    }

    public static void begin(ServerWorld world) {
        Session current = ACTIVE.get();
        if (current == null) {
            ACTIVE.set(new Session(world));
        } else {
            current.depth++;
        }
    }

    public static void record(ServerWorld world, BlockPos pos, BlockState newState) {
        Session current = ACTIVE.get();
        if (current == null || current.world != world || newState.isAir()) {
            return;
        }
        current.positions.add(pos.toImmutable());
    }

    public static void finish(boolean successful) {
        Session current = ACTIVE.get();
        if (current == null) {
            return;
        }
        if (!successful) {
            current.failed = true;
        }
        if (current.depth-- > 0) {
            return;
        }
        ACTIVE.remove();
        if (current.failed || !FabricMmoFabricRuntime.running()) {
            return;
        }
        String worldId = current.world.getRegistryKey().getValue().toString();
        for (BlockPos pos : current.positions) {
            if (!current.world.getBlockState(pos).isAir()) {
                FabricMmoFabricRuntime.markPlayerPlaced(new BlockLocation(
                        worldId, pos.getX(), pos.getY(), pos.getZ()));
            }
        }
    }

    private static final class Session {
        private final ServerWorld world;
        private final Set<BlockPos> positions = new LinkedHashSet<>();
        private int depth;
        private boolean failed;

        private Session(ServerWorld world) {
            this.world = world;
        }
    }
}
