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
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private static final ThreadLocal<Set<BreakKey>> ACTIVE_BREAKS =
            ThreadLocal.withInitial(HashSet::new);

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

        if (FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            return;
        }

        String worldId = serverWorld.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        boolean playerPlaced = FabricMmoFabricRuntime.isPlayerPlaced(location);
        BreakKey breakKey = new BreakKey(serverPlayer.getUuid(), location);
        if (ACTIVE_BREAKS.get().add(breakKey)) {
            awardMiningXp(
                    serverWorld,
                    serverPlayer,
                    pos,
                    state,
                    worldId,
                    playerPlaced,
                    serverPlayer.getMainHandStack());
        }
    }

    static void awardBeforeDrops(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {
        if (FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        BreakKey breakKey = new BreakKey(player.getUuid(), location);
        if (!ACTIVE_BREAKS.get().add(breakKey)) {
            return;
        }
        awardMiningXp(
                world,
                player,
                pos,
                state,
                worldId,
                FabricMmoFabricRuntime.isPlayerPlaced(location),
                tool);
    }

    public static void finishBlockBreak(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos) {
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        BreakKey breakKey = new BreakKey(player.getUuid(), location);
        Set<BreakKey> activeBreaks = ACTIVE_BREAKS.get();
        activeBreaks.remove(breakKey);
        try {
            // Fabric's AFTER event runs before Block.afterBreak and its drop calculation. Keep
            // the ineligible marker until tryBreakBlock has fully returned so both Mining XP and
            // bonus-drop processing see the same placed/piston-moved status. Upstream also clears
            // tracker metadata after successful breaks in blacklisted worlds.
            if (FabricMmoFabricRuntime.running()) {
                FabricMmoFabricRuntime.clearPlayerPlaced(location);
            }
        } finally {
            if (activeBreaks.isEmpty()) {
                ACTIVE_BREAKS.remove();
            }
        }
    }

    private static void awardMiningXp(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            String worldId,
            boolean playerPlaced,
            ItemStack heldItem) {
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        int configuredXp = FabricMmoFabricRuntime.miningXpFor(blockId);
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
        if (io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems.running()) {
            io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems.require().diagnostics()
                    .blockDecision(player.getUuid(), blockId.toString(), configuredXp,
                            player.isCreative(), validTool, hasPermission, protectionAllowed,
                            playerPlaced, decision.status().name());
        }
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
                PlayerProgressionContext.enrich(
                        player,
                        Map.of(
                                "block", blockId.toString(),
                                "tool", toolId.toString(),
                                "world", worldId,
                                "x", Integer.toString(pos.getX()),
                                "y", Integer.toString(pos.getY()),
                                "z", Integer.toString(pos.getZ()),
                                "upstreamReason", "PVE",
                                "upstreamSource", "SELF"),
                        FabricMmoFabricRuntime.progressionSettings(),
                        CoreSkills.MINING)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Mining XP award for {} was not applied: {}", player.getName().getString(),
                    result.detail());
            return;
        }
        if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(MiningMessages.levelUp(result.newLevel()), false);
        }
        MiningAbilityHandler.applyToolDamage(player);
    }

    private record BreakKey(UUID playerId, BlockLocation location) {
    }

}
