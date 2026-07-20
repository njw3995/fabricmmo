package io.github.njw3995.fabricmmo.core.skill.smelting;

import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-authoritative mcMMO 2.3.000 Smelting furnace behavior. */
public final class SmeltingRuntimeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmeltingRuntimeHandler.class);
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private SmeltingRuntimeHandler() {
    }

    public static int fuelTime(AbstractFurnaceBlockEntity furnace, int vanillaBurnTime) {
        if (!FabricMmoFabricRuntime.running() || vanillaBurnTime <= 0
                || !(furnace.getWorld() instanceof ServerWorld world)
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return vanillaBurnTime;
        }
        ItemStack source = furnace.getStack(0);
        if (!isConfiguredSmeltable(source)) {
            return vanillaBurnTime;
        }
        Optional<ServerPlayerEntity> owner = onlineOwner(furnace, world);
        if (owner.isEmpty() || !allowed(owner.orElseThrow(),
                PermissionNodes.SMELTING_FUEL_EFFICIENCY, true)) {
            return vanillaBurnTime;
        }
        int level = level(owner.orElseThrow());
        return SmeltingFormula.fuelTime(
                vanillaBurnTime,
                FabricMmoFabricRuntime.smeltingSettings().fuelEfficiencyRank(level));
    }

    public static void processCraft(
            AbstractFurnaceBlockEntity furnace,
            SmeltingCraftSnapshot snapshot) {
        if (!FabricMmoFabricRuntime.running()
                || !(furnace.getWorld() instanceof ServerWorld world)
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)
                || !isConfiguredSmeltable(snapshot.source())) {
            return;
        }
        Optional<ServerPlayerEntity> owner = onlineOwner(furnace, world);
        if (owner.isEmpty()) {
            return;
        }
        ServerPlayerEntity player = owner.orElseThrow();
        int xp = FabricMmoFabricRuntime.smeltingSettings().xpForInput(
                Registries.ITEM.getId(snapshot.source().getItem()).toString());
        awardXp(player, world, furnace.getPos(), snapshot.source(), xp);
        processSecondSmelt(player, world, furnace, snapshot.resultAmountBefore());
    }

    public static int extractionMultiplier(
            ServerPlayerEntity player,
            AbstractFurnaceBlockEntity furnace,
            ItemStack output) {
        ServerWorld world = player.getServerWorld();
        if (FabricMmoFabricRuntime.isWorldBlacklisted(world)
                || !allowed(player, PermissionNodes.SMELTING_VANILLA_XP, true)
                || !SmeltingRecipeEligibility.isOreSmeltingOutput(world, output)
                || !canInteract(player, world, furnace.getPos())) {
            return 1;
        }
        return FabricMmoFabricRuntime.smeltingSettings().vanillaXpMultiplier(level(player));
    }

    private static void processSecondSmelt(
            ServerPlayerEntity player,
            ServerWorld world,
            AbstractFurnaceBlockEntity furnace,
            int resultAmountBefore) {
        ItemStack result = furnace.getStack(2);
        SmeltingSettings settings = FabricMmoFabricRuntime.smeltingSettings();
        if (result.isEmpty()
                || !settings.secondSmeltEnabledForOutput(
                        Registries.ITEM.getId(result.getItem()).toString())
                || !allowed(player, PermissionNodes.SMELTING_SECOND_SMELT, true)
                || !SmeltingFormula.hasRoomForSecondSmelt(
                        resultAmountBefore, result.getMaxCount())) {
            return;
        }
        boolean lucky = allowed(player, PermissionNodes.SMELTING_LUCKY, false);
        double chance = settings.secondSmeltChance(level(player), lucky);
        if (chance <= 0.0D
                || chance < 100.0D && world.getRandom().nextDouble() * 100.0D >= chance) {
            return;
        }
        if (result.getCount() < result.getMaxCount()) {
            result.increment(1);
            furnace.markDirty();
        }
    }

    private static void awardXp(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            ItemStack source,
            int xp) {
        if (xp <= 0) {
            return;
        }
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.SMELTING,
                        CoreXpSources.SMELTING_FURNACE,
                        xp,
                        PlayerProgressionContext.enrich(
                                player,
                                Map.of(
                                        "item", Registries.ITEM.getId(source.getItem()).toString(),
                                        "world", world.getRegistryKey().getValue().toString(),
                                        "x", Integer.toString(pos.getX()),
                                        "y", Integer.toString(pos.getY()),
                                        "z", Integer.toString(pos.getZ()),
                                        "upstreamReason", "PVE",
                                        "upstreamSource", "PASSIVE"),
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.SMELTING)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.debug("Smelting XP award for {} was not applied: {}",
                    player.getUuid(), result.detail());
        }
    }

    private static Optional<ServerPlayerEntity> onlineOwner(
            AbstractFurnaceBlockEntity furnace,
            ServerWorld world) {
        if (!(furnace instanceof OwnedProcessingBlock owned)) {
            return Optional.empty();
        }
        UUID owner = owned.fabricmmo$getOwner().orElse(null);
        return owner == null
                ? Optional.empty()
                : Optional.ofNullable(world.getServer().getPlayerManager().getPlayer(owner));
    }

    private static boolean isConfiguredSmeltable(ItemStack source) {
        return !source.isEmpty()
                && FabricMmoFabricRuntime.smeltingSettings().xpForInput(
                        Registries.ITEM.getId(source.getItem()).toString()) >= 1;
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.SMELTING).level();
    }

    private static boolean allowed(
            ServerPlayerEntity player,
            String permission,
            boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), permission, fallback);
    }

    private static boolean canInteract(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos) {
        return FabricMmoFabricRuntime.requireApi().protection().canInteract(
                player.getUuid(),
                world.getRegistryKey().getValue().toString(),
                pos.getX(), pos.getY(), pos.getZ());
    }
}
