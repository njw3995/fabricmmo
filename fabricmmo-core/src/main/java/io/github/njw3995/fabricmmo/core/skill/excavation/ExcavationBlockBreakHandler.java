package io.github.njw3995.fabricmmo.core.skill.excavation;

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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Excavation XP bridge with tool, permission, protection, and placed-block checks. */
public final class ExcavationBlockBreakHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Excavation");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final ThreadLocal<Set<BreakKey>> ACTIVE_BREAKS =
            ThreadLocal.withInitial(HashSet::new);

    private ExcavationBlockBreakHandler() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(ExcavationBlockBreakHandler::afterBlockBreak);
    }

    private static void afterBlockBreak(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            return;
        }
        String worldId = serverWorld.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        BreakKey key = new BreakKey(serverPlayer.getUuid(), location);
        if (ACTIVE_BREAKS.get().add(key)) {
            award(serverWorld, serverPlayer, pos, state, serverPlayer.getMainHandStack(),
                    FabricMmoFabricRuntime.isPlayerPlaced(location));
        }
    }

    static void awardBeforeDrops(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        BreakKey key = new BreakKey(player.getUuid(), location);
        if (!ACTIVE_BREAKS.get().add(key)) {
            return;
        }
        award(world, player, pos, state, tool,
                FabricMmoFabricRuntime.isPlayerPlaced(location));
    }

    public static void finishBlockBreak(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos) {
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        Set<BreakKey> active = ACTIVE_BREAKS.get();
        boolean tracked = active.remove(new BreakKey(player.getUuid(), location));
        try {
            if (tracked && FabricMmoFabricRuntime.running()) {
                FabricMmoFabricRuntime.clearPlayerPlaced(location);
            }
        } finally {
            if (active.isEmpty()) {
                ACTIVE_BREAKS.remove();
            }
        }
    }

    private static void award(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            boolean playerPlaced) {
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        int configuredXp = FabricMmoFabricRuntime.excavationXpFor(blockId);
        String worldId = world.getRegistryKey().getValue().toString();
        boolean validTool = tool.isIn(ItemTags.SHOVELS);
        boolean permission = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.EXCAVATION, true);
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        boolean protection = api.protection().canBreak(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ());
        boolean allowed = configuredXp > 0
                && !player.isCreative()
                && validTool
                && permission
                && protection
                && !playerPlaced;
        if (SharedServerSystems.running()) {
            SharedServerSystems.require().diagnostics().message(
                    player.getUuid(),
                    "EXCAVATION block=" + blockId + " configuredXp=" + configuredXp
                            + " validTool=" + validTool + " permission=" + permission
                            + " protection=" + protection + " playerPlaced=" + playerPlaced
                            + " result=" + (allowed ? "award" : "deny"));
        }
        if (!allowed) {
            return;
        }
        boolean active = gigaDrillActive(player.getUuid());
        int checks = active ? 3 : 1;
        for (int check = 0; check < checks; check++) {
            awardXp(
                    world,
                    player,
                    pos,
                    blockId,
                    tool,
                    configuredXp,
                    active,
                    CoreXpSources.EXCAVATION_BLOCK_BREAK);
        }
        if (active && FabricMmoFabricRuntime.excavationSettings().abilityToolDamage() > 0) {
            tool.damage(
                    FabricMmoFabricRuntime.excavationSettings().abilityToolDamage(),
                    player,
                    EquipmentSlot.MAINHAND);
        }
    }

    static XpAwardResult awardTreasureXp(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            NamespacedId blockId,
            int xp) {
        return awardXp(
                world,
                player,
                pos,
                blockId,
                player.getMainHandStack(),
                xp,
                gigaDrillActive(player.getUuid()),
                CoreXpSources.EXCAVATION_TREASURE);
    }

    private static XpAwardResult awardXp(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            NamespacedId blockId,
            ItemStack tool,
            int xp,
            boolean active,
            NamespacedId sourceId) {
        NamespacedId toolId = NamespacedId.parse(
                Registries.ITEM.getId(tool.getItem()).toString());
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.EXCAVATION,
                        sourceId,
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
                                        "gigaDrillBreaker", Boolean.toString(active),
                                        "upstreamReason", "PVE",
                                        "upstreamSource", "SELF"),
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.EXCAVATION)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Excavation XP award for {} was not applied: {}",
                    player.getName().getString(), result.detail());
        } else if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(Text.literal("Excavation increased to " + result.newLevel() + "."),
                    false);
        }
        return result;
    }

    private static boolean gigaDrillActive(UUID playerId) {
        try {
            return FabricMmoFabricRuntime.excavationAbilities().isActive(playerId);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Giga Drill Breaker state", exception);
        }
    }

    private record BreakKey(UUID playerId, BlockLocation location) {
    }
}
