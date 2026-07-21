package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.fabric.SharedServerSystems;
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

/** Ordinary Woodcutting XP bridge with placed-block, permission, protection, and tool checks. */
public final class WoodcuttingBlockBreakHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Woodcutting");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<Set<BreakKey>> ACTIVE_BREAKS =
            ThreadLocal.withInitial(HashSet::new);

    private WoodcuttingBlockBreakHandler() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(WoodcuttingBlockBreakHandler::afterBlockBreak);
    }

    private static void afterBlockBreak(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity) {
        if (TreeFellerContext.active()
                || !(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !FabricMmoFabricRuntime.running()) {
            return;
        }
        if (FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            return;
        }
        String worldId = serverWorld.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        if (TreeFellerContext.startingBreak(serverPlayer.getUuid(), location)) {
            return;
        }
        boolean playerPlaced = FabricMmoFabricRuntime.isPlayerPlaced(location);
        BreakKey key = new BreakKey(serverPlayer.getUuid(), location);
        if (ACTIVE_BREAKS.get().add(key)) {
            award(serverWorld, serverPlayer, pos, state, serverPlayer.getMainHandStack(),
                    playerPlaced, false);
        }
    }

    static void awardBeforeDrops(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {
        if (TreeFellerContext.active() || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        if (TreeFellerContext.startingBreak(player.getUuid(), location)) {
            return;
        }
        BreakKey key = new BreakKey(player.getUuid(), location);
        if (!ACTIVE_BREAKS.get().add(key)) {
            return;
        }
        award(world, player, pos, state, tool,
                FabricMmoFabricRuntime.isPlayerPlaced(location), false);
    }

    public static void finishBlockBreak(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos) {
        if (TreeFellerContext.active()) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        Set<BreakKey> active = ACTIVE_BREAKS.get();
        boolean trackedBreak = active.remove(new BreakKey(player.getUuid(), location));
        TreeFellerContext.clearStartingBreak(player.getUuid(), location);
        try {
            if (trackedBreak && FabricMmoFabricRuntime.running()) {
                FabricMmoFabricRuntime.clearPlayerPlaced(location);
            }
        } finally {
            if (active.isEmpty()) {
                ACTIVE_BREAKS.remove();
            }
        }
    }

    static XpAwardResult awardTreeFellerTotal(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos startingPos,
            int xp,
            int processedLogs) {
        ItemStack tool = player.getMainHandStack();
        NamespacedId toolId = NamespacedId.parse(
                Registries.ITEM.getId(tool.getItem()).toString());
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.WOODCUTTING,
                        CoreXpSources.WOODCUTTING_TREE_FELLER,
                        xp,
                        PlayerProgressionContext.enrich(
                                player,
                                Map.of(
                                        "tool", toolId.toString(),
                                        "world", world.getRegistryKey().getValue().toString(),
                                        "x", Integer.toString(startingPos.getX()),
                                        "y", Integer.toString(startingPos.getY()),
                                        "z", Integer.toString(startingPos.getZ()),
                                        "treeFeller", "true",
                                        "processedLogs", Integer.toString(processedLogs),
                                        "upstreamReason", "PVE",
                                        "upstreamSource", "SELF"),
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.WOODCUTTING)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Tree Feller XP award for {} was not applied: {}",
                    player.getName().getString(), result.detail());
        } else if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(WoodcuttingMessages.levelUp(result.oldLevel(), result.newLevel()), false);
        }
        return result;
    }

    private static void award(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            boolean playerPlaced,
            boolean treeFeller) {
        NamespacedId blockId = WoodcuttingBlockClassifier.id(state);
        var extension = FabricMmoFabricRuntime.gatheringContentFor(CoreSkills.WOODCUTTING, state);
        int configuredXp = FabricMmoFabricRuntime.woodcuttingXpFor(state);
        String worldId = world.getRegistryKey().getValue().toString();
        boolean validTool = extension
                .map(definition -> FabricMmoFabricRuntime.gatheringContentResolver()
                        .validTool(definition, tool))
                .orElseGet(() -> tool.isIn(ItemTags.AXES));
        boolean permission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.WOODCUTTING, true);
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        boolean protection = api.protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ());
        WoodcuttingXpDecision decision = WoodcuttingXpDecision.evaluate(
                configuredXp,
                player.isCreative(),
                validTool,
                permission,
                protection,
                playerPlaced && extension.map(definition -> definition.naturalBlocksOnly()).orElse(true));
        if (SharedServerSystems.running()) {
            SharedServerSystems.require().diagnostics().message(
                    player.getUuid(),
                    "WOODCUTTING block=" + blockId + " configuredXp=" + configuredXp
                            + " validTool=" + validTool + " permission=" + permission
                            + " protection=" + protection + " playerPlaced=" + playerPlaced
                            + " treeFeller=" + treeFeller + " result=" + decision.status());
        }
        if (decision.awardsXp()) {
            awardApplied(world, player, pos, state, tool, decision.xp(), treeFeller);
        }
    }

    private static XpAwardResult awardApplied(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            int xp,
            boolean treeFeller) {
        NamespacedId blockId = WoodcuttingBlockClassifier.id(state);
        NamespacedId toolId = NamespacedId.parse(
                Registries.ITEM.getId(tool.getItem()).toString());
        NamespacedId source = treeFeller
                ? CoreXpSources.WOODCUTTING_TREE_FELLER
                : CoreXpSources.WOODCUTTING_BLOCK_BREAK;
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.WOODCUTTING,
                        source,
                        xp,
                        PlayerProgressionContext.enrich(
                                player,
                                Map.of(
                                        "block", blockId.toString(),
                                        "tool", toolId.toString(),
                                        "world", world.getRegistryKey().getValue().toString(),
                                        "x", Integer.toString(pos.getX()),
                                        "y", Integer.toString(pos.getY()),
                                        "z", Integer.toString(pos.getZ()),
                                        "treeFeller", Boolean.toString(treeFeller),
                                        "upstreamReason", "PVE",
                                        "upstreamSource", "SELF"),
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.WOODCUTTING)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Woodcutting XP award for {} was not applied: {}",
                    player.getName().getString(), result.detail());
        } else if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(WoodcuttingMessages.levelUp(result.oldLevel(), result.newLevel()), false);
        }
        return result;
    }

    private record BreakKey(UUID playerId, BlockLocation location) {
    }
}
