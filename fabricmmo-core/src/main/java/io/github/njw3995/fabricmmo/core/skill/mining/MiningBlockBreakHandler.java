package io.github.njw3995.fabricmmo.core.skill.mining;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MiningBlockBreakHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Mining");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private MiningBlockBreakHandler() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(MiningBlockBreakHandler::afterBlockBreak);
    }

    private static void afterBlockBreak(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !FabricMmoFabricRuntime.running()) {
            return;
        }

        String worldId = serverWorld.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        boolean playerPlaced = FabricMmoFabricRuntime.isPlayerPlaced(location);
        try {
            awardMiningXp(serverWorld, serverPlayer, pos, state, worldId, playerPlaced);
        } finally {
            // Upstream clears the ineligible marker after a successful block break.
            FabricMmoFabricRuntime.clearPlayerPlaced(location);
        }
    }

    private static void awardMiningXp(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            String worldId,
            boolean playerPlaced) {
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        int configuredXp = FabricMmoFabricRuntime.miningXpFor(blockId);
        ItemStack heldItem = player.getMainHandStack();
        boolean validTool = heldItem.isIn(ItemTags.PICKAXES) || heldItem.isIn(ItemTags.HOES);
        boolean hasPermission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.MINING, true);
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        boolean protectionAllowed = api.protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ());

        MiningXpDecision decision = MiningXpDecision.evaluate(
                configuredXp,
                player.isCreative(),
                validTool,
                hasPermission,
                protectionAllowed,
                playerPlaced);
        if (!decision.awardsXp()) {
            return;
        }

        NamespacedId toolId = NamespacedId.parse(
                Registries.ITEM.getId(heldItem.getItem()).toString());
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player.getUuid(),
                CoreSkills.MINING,
                CoreXpSources.MINING_BLOCK_BREAK,
                decision.xp(),
                Map.of(
                        "block", blockId.toString(),
                        "tool", toolId.toString(),
                        "world", worldId,
                        "x", Integer.toString(pos.getX()),
                        "y", Integer.toString(pos.getY()),
                        "z", Integer.toString(pos.getZ()),
                        "upstreamReason", "PVE",
                        "upstreamSource", "SELF")));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Mining XP award for {} was not applied: {}", player.getName().getString(),
                    result.detail());
        }
    }
}
