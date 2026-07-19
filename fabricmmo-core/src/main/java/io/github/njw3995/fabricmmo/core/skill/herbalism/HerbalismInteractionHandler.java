package io.github.njw3995.fabricmmo.core.skill.herbalism;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Green Thumb block conversion, Shroom Thumb, Hylian Luck, and berry harvesting. */
public final class HerbalismInteractionHandler {
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final List<PendingBerryAward> BERRY_AWARDS = new ArrayList<>();

    private HerbalismInteractionHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !available(serverPlayer, serverWorld)) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = serverWorld.getBlockState(pos);
            ItemStack held = serverPlayer.getMainHandStack();

            ActionResult greenThumb = greenThumbBlock(serverPlayer, serverWorld, pos, state, held);
            if (greenThumb != ActionResult.PASS) {
                return greenThumb;
            }
            ActionResult shroomThumb = shroomThumb(serverPlayer, serverWorld, pos, state, held);
            if (shroomThumb != ActionResult.PASS) {
                return shroomThumb;
            }
            queueBerryAward(serverPlayer, serverWorld, pos, state);
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !available(serverPlayer, serverWorld)
                    || !serverPlayer.getMainHandStack().isIn(ItemTags.SWORDS)) {
                return ActionResult.PASS;
            }
            return hylianLuck(serverPlayer, serverWorld, pos, serverWorld.getBlockState(pos));
        });

        ServerTickEvents.END_SERVER_TICK.register(HerbalismInteractionHandler::tickBerryAwards);
    }

    public static void reset() {
        synchronized (BERRY_AWARDS) {
            BERRY_AWARDS.clear();
        }
    }

    private static ActionResult greenThumbBlock(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state,
            ItemStack held) {
        if (!held.isOf(Items.WHEAT_SEEDS) || !HerbalismConversions.canGreen(state)) {
            return ActionResult.PASS;
        }
        int level = level(player);
        HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
        String blockPath = Registries.BLOCK.getId(state.getBlock()).getPath();
        if (settings.greenThumbRank(level) <= 0
                || !allowed(
                        player,
                        PermissionNodes.herbalismGreenThumbBlock(blockPath),
                        true)
                || !canBreak(player, world, pos)) {
            return ActionResult.PASS;
        }
        // Upstream consumes the seed before evaluating the Green Thumb roll.
        if (!HerbalismInventory.removeOne(player, Items.WHEAT_SEEDS)) {
            return ActionResult.PASS;
        }
        boolean lucky = allowed(player, PermissionNodes.HERBALISM_LUCKY, false);
        if (!HerbalismProbability.roll(
                world.getRandom().nextDouble(), settings.greenThumbChance(level, lucky))) {
            message(player, HerbalismMessages.greenThumbFailed());
            return ActionResult.SUCCESS;
        }
        HerbalismConversions.convertGreen(world, pos, state);
        return ActionResult.SUCCESS;
    }

    private static ActionResult shroomThumb(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state,
            ItemStack held) {
        if ((!held.isOf(Items.BROWN_MUSHROOM) && !held.isOf(Items.RED_MUSHROOM))
                || !HerbalismConversions.canShroom(state)
                || !allowed(player, PermissionNodes.HERBALISM_SHROOM_THUMB, true)
                || !canBreak(player, world, pos)) {
            return ActionResult.PASS;
        }
        if (!HerbalismInventory.contains(player, Items.BROWN_MUSHROOM)) {
            message(player, HerbalismMessages.needMore("Brown Mushroom"));
            return ActionResult.SUCCESS;
        }
        if (!HerbalismInventory.contains(player, Items.RED_MUSHROOM)) {
            message(player, HerbalismMessages.needMore("Red Mushroom"));
            return ActionResult.SUCCESS;
        }
        HerbalismInventory.removeOne(player, Items.BROWN_MUSHROOM);
        HerbalismInventory.removeOne(player, Items.RED_MUSHROOM);
        int level = level(player);
        boolean lucky = allowed(player, PermissionNodes.HERBALISM_LUCKY, false);
        if (!HerbalismProbability.roll(
                world.getRandom().nextDouble(),
                FabricMmoFabricRuntime.herbalismSettings().shroomThumbChance(level, lucky))) {
            message(player, HerbalismMessages.shroomThumbFailed());
            return ActionResult.SUCCESS;
        }
        HerbalismConversions.convertShroom(world, pos, state);
        return ActionResult.SUCCESS;
    }

    private static ActionResult hylianLuck(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state) {
        if (!allowed(player, PermissionNodes.HERBALISM_HYLIAN_LUCK, true)
                || !canBreak(player, world, pos)) {
            return ActionResult.PASS;
        }
        NamespacedId blockId = NamespacedId.parse(
                Registries.BLOCK.getId(state.getBlock()).toString());
        List<HerbalismHylianTreasure> treasures =
                FabricMmoFabricRuntime.herbalismTreasures().treasures(blockId);
        if (treasures.isEmpty()) {
            return ActionResult.PASS;
        }
        int level = level(player);
        HerbalismSettings settings = FabricMmoFabricRuntime.herbalismSettings();
        boolean lucky = allowed(player, PermissionNodes.HERBALISM_LUCKY, false);
        if (!HerbalismProbability.roll(
                world.getRandom().nextDouble(), settings.hylianLuckChance(level, lucky))) {
            return ActionResult.PASS;
        }
        for (HerbalismHylianTreasure treasure : treasures) {
            if (level < treasure.requiredLevel(settings.progressionMode())
                    || !HerbalismProbability.roll(
                            world.getRandom().nextDouble(), treasure.dropChancePercent())) {
                continue;
            }
            Item item = Registries.ITEM.get(Identifier.of(
                    treasure.itemId().namespace(), treasure.itemId().path()));
            ItemStack stack = new ItemStack(item, treasure.amount());
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            world.spawnEntity(new ItemEntity(
                    world,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    stack));
            if (treasure.xp() > 0) {
                awardXp(player, world, pos, treasure.xp(), CoreXpSources.HERBALISM_HYLIAN_LUCK,
                        Map.of("treasure", treasure.itemId().toString()));
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static void queueBerryAward(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state) {
        String path = Registries.BLOCK.getId(state.getBlock()).getPath();
        if (!path.equals("sweet_berry_bush")) {
            return;
        }
        IntProperty age = HerbalismPlantRules.ageProperty(state);
        if (age == null) {
            return;
        }
        int multiplier = state.get(age) == 2 ? 1 : state.get(age) == 3 ? 2 : 0;
        if (multiplier == 0) {
            return;
        }
        int xp = FabricMmoFabricRuntime.herbalismXpFor(
                NamespacedId.parse(Registries.BLOCK.getId(state.getBlock()).toString()));
        if (xp <= 0) {
            return;
        }
        synchronized (BERRY_AWARDS) {
            BERRY_AWARDS.add(new PendingBerryAward(
                    player.getUuid(), world, pos.toImmutable(), xp * multiplier, 1));
        }
    }

    private static void tickBerryAwards(MinecraftServer server) {
        synchronized (BERRY_AWARDS) {
            Iterator<PendingBerryAward> iterator = BERRY_AWARDS.iterator();
            ArrayList<PendingBerryAward> delayed = new ArrayList<>();
            while (iterator.hasNext()) {
                PendingBerryAward pending = iterator.next();
                iterator.remove();
                if (pending.ticksRemaining() > 1) {
                    delayed.add(new PendingBerryAward(
                            pending.playerId(), pending.world(), pending.pos(), pending.xp(),
                            pending.ticksRemaining() - 1));
                    continue;
                }
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(pending.playerId());
                if (player == null) {
                    continue;
                }
                BlockState current = pending.world().getBlockState(pending.pos());
                IntProperty age = HerbalismPlantRules.ageProperty(current);
                if (Registries.BLOCK.getId(current.getBlock()).getPath().equals("sweet_berry_bush")
                        && age != null && current.get(age) <= 1) {
                    awardXp(player, pending.world(), pending.pos(), pending.xp(),
                            CoreXpSources.HERBALISM_BERRY_HARVEST, Map.of());
                }
            }
            BERRY_AWARDS.addAll(delayed);
        }
    }

    private static void awardXp(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            int xp,
            NamespacedId source,
            Map<String, String> metadata) {
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        XpAwardResult result = api.progression().award(new XpAwardRequest(
                player.getUuid(),
                CoreSkills.HERBALISM,
                source,
                xp,
                PlayerProgressionContext.enrich(
                        player,
                        merge(metadata, Map.of(
                                "world", world.getRegistryKey().getValue().toString(),
                                "x", Integer.toString(pos.getX()),
                                "y", Integer.toString(pos.getY()),
                                "z", Integer.toString(pos.getZ()),
                                "upstreamReason", "PVE",
                                "upstreamSource", "SELF")),
                        FabricMmoFabricRuntime.progressionSettings(),
                        CoreSkills.HERBALISM)));
        if (result.status() == XpAwardResult.Status.APPLIED
                && result.newLevel() > result.oldLevel()) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "Herbalism increased to " + result.newLevel() + "."), false);
        }
    }

    private static Map<String, String> merge(
            Map<String, String> first,
            Map<String, String> second) {
        java.util.HashMap<String, String> merged = new java.util.HashMap<>(first);
        merged.putAll(second);
        return Map.copyOf(merged);
    }

    private static boolean available(ServerPlayerEntity player, ServerWorld world) {
        return FabricMmoFabricRuntime.running()
                && !player.isCreative()
                && !FabricMmoFabricRuntime.isWorldBlacklisted(world)
                && allowed(player, PermissionNodes.HERBALISM, true);
    }

    private static boolean canBreak(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos) {
        return FabricMmoFabricRuntime.requireApi().protection().canBreak(
                player.getUuid(), world.getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.HERBALISM).level();
    }

    private static void message(ServerPlayerEntity player, net.minecraft.text.Text text) {
        if (FabricMmoFabricRuntime.herbalismSettings().abilityMessages()) {
            player.sendMessage(text, true);
        }
    }

    private record PendingBerryAward(
            UUID playerId,
            ServerWorld world,
            BlockPos pos,
            int xp,
            int ticksRemaining) {
    }
}
