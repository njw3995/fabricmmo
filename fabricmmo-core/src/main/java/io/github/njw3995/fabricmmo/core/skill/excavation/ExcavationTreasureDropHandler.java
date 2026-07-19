package io.github.njw3995.fabricmmo.core.skill.excavation;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.block.BlockLocation;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Injects Archaeology treasures into the vanilla block drop list. */
public final class ExcavationTreasureDropHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private ExcavationTreasureDropHandler() {
    }

    public static List<ItemStack> apply(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool,
            List<ItemStack> vanillaDrops) {
        if (!(entity instanceof ServerPlayerEntity player)
                || !FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return vanillaDrops;
        }
        ExcavationBlockBreakHandler.awardBeforeDrops(world, player, pos, state, tool);
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        if (FabricMmoFabricRuntime.excavationXpFor(blockId) <= 0
                || player.isCreative()
                || !tool.isIn(ItemTags.SHOVELS)
                || !PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.EXCAVATION, true)
                || !PERMISSIONS.hasPermission(
                        player.getCommandSource(), PermissionNodes.EXCAVATION_ARCHAEOLOGY, true)) {
            return vanillaDrops;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        BlockLocation location = new BlockLocation(worldId, pos.getX(), pos.getY(), pos.getZ());
        if (FabricMmoFabricRuntime.isPlayerPlaced(location)
                || !FabricMmoFabricRuntime.requireApi().protection().canBreak(
                        player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ())) {
            return vanillaDrops;
        }
        int level = FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.EXCAVATION).level();
        boolean active = active(player);
        boolean lucky = PERMISSIONS.hasPermission(
                player.getCommandSource(), PermissionNodes.EXCAVATION_LUCKY, false);
        ExcavationTreasureRoller.Outcome outcome = new ExcavationTreasureRoller(
                world.getRandom()::nextDouble).roll(
                        blockId,
                        level,
                        active,
                        lucky,
                        true,
                        FabricMmoFabricRuntime.excavationSettings(),
                        FabricMmoFabricRuntime.excavationTreasures());
        for (int treasureXp : outcome.treasureXpAwards()) {
            ExcavationBlockBreakHandler.awardTreasureXp(
                    world, player, pos, blockId, treasureXp);
        }
        for (int orbXp : outcome.vanillaOrbAwards()) {
            ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), orbXp);
        }
        if (outcome.treasureDrops().isEmpty()) {
            return vanillaDrops;
        }
        ArrayList<ItemStack> result = new ArrayList<>(
                vanillaDrops.size() + outcome.treasureDrops().size());
        result.addAll(vanillaDrops);
        for (ExcavationTreasure treasure : outcome.treasureDrops()) {
            Identifier itemId = Identifier.of(
                    treasure.itemId().namespace(), treasure.itemId().path());
            Item item = Registries.ITEM.getOrEmpty(itemId).orElse(null);
            if (item != null) {
                result.add(new ItemStack(item, treasure.amount()));
            }
        }
        return List.copyOf(result);
    }

    private static boolean active(ServerPlayerEntity player) {
        try {
            return FabricMmoFabricRuntime.excavationAbilities().isActive(player.getUuid());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read Giga Drill Breaker state", exception);
        }
    }
}
