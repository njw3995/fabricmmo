package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.api.FabricMmoApi;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.api.progression.XpAwardResult;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import io.github.njw3995.fabricmmo.core.skill.excavation.ExcavationAbilityHandler;
import io.github.njw3995.fabricmmo.core.skill.mining.MiningAbilityHandler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-authoritative Repair and Salvage anvil interaction path. */
public final class UtilityAnvilInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final UtilityAnvilConfirmationService CONFIRMATIONS =
            UtilityAnvilConfirmationService.global();
    private static final Set<UUID> REPAIR_ANVIL_PLACED_NOTIFIED =
            ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SALVAGE_ANVIL_PLACED_NOTIFIED =
            ConcurrentHashMap.newKeySet();

    private UtilityAnvilInteractionHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()
                    || serverPlayer.isCreative()
                    || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
                return ActionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!protectedInteractionAllowed(serverPlayer, serverWorld, pos)) {
                return ActionResult.PASS;
            }
            ItemStack held = serverPlayer.getMainHandStack();
            Block clicked = serverWorld.getBlockState(pos).getBlock();
            return rightClick(serverPlayer, serverWorld, pos, clicked, held);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand != Hand.MAIN_HAND
                    || world.isClient()
                    || !(world instanceof ServerWorld serverWorld)
                    || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !FabricMmoFabricRuntime.running()) {
                return ActionResult.PASS;
            }
            Block clicked = serverWorld.getBlockState(pos).getBlock();
            cancelConfirmation(serverPlayer, clicked, serverPlayer.getMainHandStack());
            return ActionResult.PASS;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUuid();
            CONFIRMATIONS.clear(playerId);
            REPAIR_ANVIL_PLACED_NOTIFIED.remove(playerId);
            SALVAGE_ANVIL_PLACED_NOTIFIED.remove(playerId);
        });
    }

    public static void reset() {
        CONFIRMATIONS.clearAll();
        REPAIR_ANVIL_PLACED_NOTIFIED.clear();
        SALVAGE_ANVIL_PLACED_NOTIFIED.clear();
    }

    /** Mirrors upstream's once-per-login placement notification for utility anvils. */
    public static void onBlockPlaced(
            ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(world)) {
            return;
        }
        Identifier placedId = Registries.BLOCK.getId(world.getBlockState(pos).getBlock());
        RepairSettings repair = FabricMmoFabricRuntime.repairSettings();
        if (matchesBlock(placedId, repair.anvilMaterial())
                && allowed(player, PermissionNodes.REPAIR, true)
                && REPAIR_ANVIL_PLACED_NOTIFIED.add(player.getUuid())) {
            if (repair.anvilMessages()) {
                message(player, "Repair.Listener.Anvil");
            }
            if (repair.anvilPlacedSounds()) {
                play(player, FabricMmoFabricRuntime.utilityAnvilSounds().anvil(),
                        SoundCategory.BLOCKS);
            }
            return;
        }
        SalvageSettings salvage = FabricMmoFabricRuntime.salvageSettings();
        if (matchesBlock(placedId, salvage.anvilMaterial())
                && allowed(player, PermissionNodes.SALVAGE, true)
                && SALVAGE_ANVIL_PLACED_NOTIFIED.add(player.getUuid())) {
            if (salvage.anvilMessages()) {
                message(player, "Salvage.Listener.Anvil");
            }
            if (salvage.anvilPlacedSounds()) {
                play(player, FabricMmoFabricRuntime.utilityAnvilSounds().anvil(),
                        SoundCategory.BLOCKS);
            }
        }
    }

    private static ActionResult rightClick(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            Block clicked,
            ItemStack held) {
        RepairSettings repairSettings = FabricMmoFabricRuntime.repairSettings();
        SalvageSettings salvageSettings = FabricMmoFabricRuntime.salvageSettings();
        if (repairSettings.onlyActivateWhenSneaking() && !player.isSneaking()) {
            return ActionResult.PASS;
        }
        MinecraftUtilityDefinitions definitions = FabricMmoFabricRuntime.utilityDefinitions();
        Identifier clickedId = Registries.BLOCK.getId(clicked);

        if (matchesBlock(clickedId, repairSettings.anvilMaterial())
                && allowed(player, PermissionNodes.REPAIR, true)) {
            var repair = definitions.repair(held.getItem());
            if (repair.isPresent() && held.getCount() <= 1) {
                if (repairSettings.confirmationRequired()
                        && !CONFIRMATIONS.confirmOrPrompt(
                                player.getUuid(), UtilityAnvilConfirmationService.Kind.REPAIR, held)) {
                    prompt(player, UtilityAnvilConfirmationService.Kind.REPAIR);
                    return ActionResult.SUCCESS;
                }
                repair(player, world, pos, held, repair.orElseThrow(), repairSettings);
                return ActionResult.SUCCESS;
            }
        }

        if (matchesBlock(clickedId, salvageSettings.anvilMaterial())
                && allowed(player, PermissionNodes.SALVAGE, true)) {
            var salvage = definitions.salvage(held.getItem());
            if (salvage.isPresent() && held.getCount() <= 1) {
                int level = level(player, CoreSkills.SALVAGE);
                int scrapRank = salvageSettings.scrapCollectorRank(level);
                if (!allowed(player, PermissionNodes.SALVAGE_SCRAP_COLLECTOR, true)
                        || scrapRank <= 0) {
                    sendSalvageLocked(player, level, salvage.orElseThrow().definition(),
                            salvageSettings);
                    return ActionResult.SUCCESS;
                }
                if (salvageSettings.confirmationRequired()
                        && !CONFIRMATIONS.confirmOrPrompt(
                                player.getUuid(), UtilityAnvilConfirmationService.Kind.SALVAGE, held)) {
                    prompt(player, UtilityAnvilConfirmationService.Kind.SALVAGE);
                    return ActionResult.SUCCESS;
                }
                salvage(player, world, pos, held, salvage.orElseThrow(), salvageSettings);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    private static void cancelConfirmation(
            ServerPlayerEntity player, Block clicked, ItemStack held) {
        Identifier clickedId = Registries.BLOCK.getId(clicked);
        RepairSettings repair = FabricMmoFabricRuntime.repairSettings();
        SalvageSettings salvage = FabricMmoFabricRuntime.salvageSettings();
        if (matchesBlock(clickedId, repair.anvilMaterial())
                && FabricMmoFabricRuntime.utilityDefinitions().repair(held.getItem()).isPresent()
                && CONFIRMATIONS.cancel(
                        player.getUuid(), UtilityAnvilConfirmationService.Kind.REPAIR)) {
            player.sendMessage(UtilityAnvilMessages.text(
                    "Skills.Cancelled",
                    UtilityAnvilMessages.localizedSkillName(
                            UtilityAnvilConfirmationService.Kind.REPAIR)), false);
            return;
        }
        if (matchesBlock(clickedId, salvage.anvilMaterial())
                && FabricMmoFabricRuntime.utilityDefinitions().salvage(held.getItem()).isPresent()
                && CONFIRMATIONS.cancel(
                        player.getUuid(), UtilityAnvilConfirmationService.Kind.SALVAGE)) {
            player.sendMessage(UtilityAnvilMessages.text(
                    "Skills.Cancelled",
                    UtilityAnvilMessages.localizedSkillName(
                            UtilityAnvilConfirmationService.Kind.SALVAGE)), false);
        }
    }

    private static void repair(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            ItemStack item,
            MinecraftUtilityDefinitions.ResolvedRepair resolved,
            RepairSettings settings) {
        RepairDefinition definition = resolved.definition();
        if (!settings.allowCustomModelData() && item.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            message(player, "Anvil.Repair.Reject.CustomModelData");
            return;
        }
        if (item.contains(DataComponentTypes.UNBREAKABLE)) {
            message(player, "Anvil.Unbreakable");
            return;
        }
        if (!allowed(player, definition.itemType().repairPermission(), true)
                || !allowed(player, definition.materialCategory().repairPermission(), true)) {
            message(player, "mcMMO.NoPermission");
            return;
        }
        int skillLevel = level(player, CoreSkills.REPAIR);
        if (skillLevel < definition.minimumLevel()) {
            message(player, "Repair.Skills.Adept", definition.minimumLevel(), item.getName().getString());
            return;
        }
        if (item.getCount() != 1) {
            message(player, "Repair.Skills.StackedItems");
            return;
        }
        boolean rebind = settings.confirmationRequired()
                && CONFIRMATIONS.isAwaiting(
                        player.getUuid(), UtilityAnvilConfirmationService.Kind.REPAIR, item);

        // Upstream removes temporary ability enchantments before reading or rewriting enchants.
        MiningAbilityHandler.removeTemporaryToolBuffForRepair(player, item);
        ExcavationAbilityHandler.prepareToolForUtilityAnvil(player);
        int oldDamage = item.getDamage();
        if (oldDamage <= 0) {
            message(player, "Repair.Skills.FullDurability");
            return;
        }
        int materialSlot = findRepairMaterial(player.getInventory(), resolved.material(),
                settings.useEnchantedMaterials());
        if (materialSlot < 0) {
            message(player, "Skills.NeedMore.Extra", resolved.material().getName().getString(), "");
            return;
        }
        boolean lucky = allowed(player, PermissionNodes.REPAIR_LUCKY, false);
        boolean mastery = settings.repairMasteryEnabled()
                && settings.repairMasteryUnlocked(skillLevel)
                && allowed(player, PermissionNodes.REPAIR_MASTERY, true);
        boolean superRepair = settings.superRepairUnlocked(skillLevel)
                && allowed(player, PermissionNodes.REPAIR_SUPER_REPAIR, true)
                && roll(world, settings.superRepairChance(skillLevel, lucky));
        if (superRepair) {
            message(player, "Repair.Skills.FeltEasy");
        }
        int maximumDurability = maximumDurability(item, definition.configuredMaximumDurability());
        int newDamage = RepairFormula.repairedDamage(
                oldDamage,
                maximumDurability,
                definition.minimumQuantity(),
                skillLevel,
                mastery,
                settings.masteryMaximumBonusPercentage(),
                settings.masteryMaximumBonusLevel(),
                superRepair);

        applyArcaneForging(player, world, item, skillLevel, settings, lucky);
        consumeOne(player.getInventory(), materialSlot);
        double xp = RepairFormula.xp(
                oldDamage,
                newDamage,
                maximumDurability,
                definition.xpMultiplier(),
                settings.baseXp(),
                settings.materialXpMultiplier(definition.materialCategory()));
        item.setDamage(newDamage);
        awardRepairXp(player, world, pos, item, xp);
        if (settings.anvilUseSounds()) {
            play(player, FabricMmoFabricRuntime.utilityAnvilSounds().anvil(), SoundCategory.BLOCKS);
            play(player, FabricMmoFabricRuntime.utilityAnvilSounds().itemBreak(), SoundCategory.PLAYERS);
        }
        if (rebind) {
            CONFIRMATIONS.rebind(
                    player.getUuid(), UtilityAnvilConfirmationService.Kind.REPAIR, item);
        }
    }

    private static void applyArcaneForging(
            ServerPlayerEntity player,
            ServerWorld world,
            ItemStack item,
            int skillLevel,
            RepairSettings settings,
            boolean lucky) {
        ItemEnchantmentsComponent original = item.getEnchantments();
        if (original.isEmpty() || !settings.arcaneMayLoseEnchants()
                || allowed(player, PermissionNodes.REPAIR_ENCHANT_BYPASS, false)) {
            return;
        }
        boolean bypass = allowed(player, PermissionNodes.ARCANE_BYPASS, false);
        int rank = settings.arcaneForgingRank(skillLevel);
        boolean arcanePermission = allowed(player, PermissionNodes.REPAIR_ARCANE_FORGING, true);
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(original);
        boolean downgraded = false;
        int kept = 0;
        List<Object2IntMap.Entry<RegistryEntry<Enchantment>>> entries =
                new ArrayList<>(original.getEnchantmentEntries());
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : entries) {
            int level = entry.getIntValue();
            ArcaneForgingFormula.Result result = ArcaneForgingFormula.resolve(
                    level,
                    entry.getKey().value().getMaxLevel(),
                    settings.arcaneMaximumEnchantLevel(),
                    settings.unsafeEnchantments(),
                    bypass,
                    settings.arcaneMayLoseEnchants(),
                    rank,
                    arcanePermission,
                    roll(world, settings.keepEnchantChance(rank, lucky)),
                    settings.arcaneDowngradesEnabled(),
                    roll(world, settings.avoidDowngradeChance(rank, lucky)));
            if (result.outcome() == ArcaneForgingFormula.Outcome.LOST) {
                builder.remove(candidate -> candidate.equals(entry.getKey()));
            } else {
                builder.set(entry.getKey(), result.level());
                kept++;
                downgraded |= result.outcome() == ArcaneForgingFormula.Outcome.DOWNGRADED;
            }
        }
        item.set(DataComponentTypes.ENCHANTMENTS, builder.build());
        if (bypass) {
            message(player, "Repair.Arcane.Perfect");
        } else if (rank == 0 || !arcanePermission) {
            message(player, "Repair.Arcane.Lost");
        } else if (kept == 0) {
            message(player, "Repair.Arcane.Fail");
        } else if (downgraded || kept < entries.size()) {
            message(player, "Repair.Arcane.Downgrade");
        } else {
            message(player, "Repair.Arcane.Perfect");
        }
    }

    private static void salvage(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            ItemStack item,
            MinecraftUtilityDefinitions.ResolvedSalvage resolved,
            SalvageSettings settings) {
        SalvageDefinition definition = resolved.definition();
        if (!settings.allowCustomModelData() && item.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            message(player, "Anvil.Salvage.Reject.CustomModelData");
            return;
        }
        if (item.contains(DataComponentTypes.UNBREAKABLE)) {
            message(player, "Anvil.Unbreakable");
            return;
        }
        if (!allowed(player, definition.itemType().salvagePermission(), true)
                || !allowed(player, definition.materialCategory().salvagePermission(), true)) {
            message(player, "mcMMO.NoPermission");
            return;
        }
        MiningAbilityHandler.removeTemporaryToolBuffForRepair(player, item);
        ExcavationAbilityHandler.prepareToolForUtilityAnvil(player);
        int level = level(player, CoreSkills.SALVAGE);
        if (level < definition.minimumLevel()) {
            message(player, "Salvage.Skills.Adept.Level",
                    definition.minimumLevel(), item.getName().getString());
            return;
        }
        int rank = settings.scrapCollectorRank(level);
        int maxDurability = maximumDurability(item, definition.configuredMaximumDurability());
        int amount = SalvageFormula.recoveredAmount(
                item.getDamage(), maxDurability, definition.maximumQuantity(), rank);
        if (amount <= 0) {
            message(player, "Salvage.Skills.TooDamaged");
            return;
        }
        ItemStack book = arcaneSalvage(player, world, item, level, settings);
        ItemStack materials = new ItemStack(resolved.material(), amount);
        message(player, "Salvage.Skills.Lottery.Normal", amount, item.getName().getString());
        player.getInventory().setStack(player.getInventory().selectedSlot, ItemStack.EMPTY);
        spawnTowardsPlayer(world, pos, player, book);
        spawnTowardsPlayer(world, pos, player, materials);
        if (settings.anvilUseSounds()) {
            play(player, FabricMmoFabricRuntime.utilityAnvilSounds().itemBreak(), SoundCategory.PLAYERS);
        }
        message(player, "Salvage.Skills.Success");
        CONFIRMATIONS.clear(player.getUuid());
    }

    private static ItemStack arcaneSalvage(
            ServerPlayerEntity player,
            ServerWorld world,
            ItemStack item,
            int level,
            SalvageSettings settings) {
        ItemEnchantmentsComponent enchants = item.getEnchantments();
        if (enchants.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int rank = settings.arcaneSalvageRank(level);
        if (rank <= 0 || !allowed(player, PermissionNodes.SALVAGE_ARCANE, true)) {
            message(player, "Salvage.Skills.ArcaneFailed");
            return ItemStack.EMPTY;
        }
        boolean bypass = allowed(player, PermissionNodes.SALVAGE_ENCHANT_BYPASS, false);
        boolean lucky = allowed(player, PermissionNodes.SALVAGE_LUCKY, false);
        ItemEnchantmentsComponent.Builder stored =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        boolean downgraded = false;
        int extracted = 0;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry
                : enchants.getEnchantmentEntries()) {
            ArcaneSalvageFormula.Result result = ArcaneSalvageFormula.resolve(
                    entry.getIntValue(),
                    settings.maximumEnchantLevel(),
                    settings.unsafeEnchantments(),
                    settings.enchantLossEnabled(),
                    settings.enchantDowngradeEnabled(),
                    bypass,
                    roll(world, settings.fullExtractionChance(rank, lucky)),
                    roll(world, settings.partialExtractionChance(rank, lucky)));
            if (result.outcome() != ArcaneSalvageFormula.Outcome.LOST) {
                stored.set(entry.getKey(), result.level());
                extracted++;
                downgraded |= result.outcome() == ArcaneSalvageFormula.Outcome.PARTIAL;
            }
        }
        if (extracted == 0) {
            message(player, "Salvage.Skills.ArcaneFailed");
            return ItemStack.EMPTY;
        }
        message(player, downgraded
                ? "Salvage.Skills.ArcanePartial" : "Salvage.Skills.ArcaneSuccess");
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponentTypes.STORED_ENCHANTMENTS, stored.build());
        return book;
    }

    private static void awardRepairXp(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            ItemStack item,
            double xp) {
        if (!(xp > 0.0D)) {
            return;
        }
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.REPAIR,
                        CoreXpSources.REPAIR_ANVIL,
                        xp,
                        PlayerProgressionContext.enrich(
                                player,
                                Map.of(
                                        "item", Registries.ITEM.getId(item.getItem()).toString(),
                                        "world", world.getRegistryKey().getValue().toString(),
                                        "x", Integer.toString(pos.getX()),
                                        "y", Integer.toString(pos.getY()),
                                        "z", Integer.toString(pos.getZ()),
                                        "upstreamReason", "PVE",
                                        "upstreamSource", "SELF"),
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.REPAIR)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Repair XP award for {} was not applied: {}",
                    player.getUuid(), result.detail());
        }
    }

    private static void sendSalvageLocked(
            ServerPlayerEntity player,
            int level,
            SalvageDefinition definition,
            SalvageSettings settings) {
        int unlock = settings.scrapCollectorRetro()[0];
        if (settings.progressionMode() == io.github.njw3995.fabricmmo.api.progression.ProgressionMode.STANDARD) {
            unlock = settings.scrapCollectorStandard()[0];
        }
        if (level < definition.minimumLevel()) {
            message(player, "Salvage.Skills.ScrapCollector.LockedAndItem",
                    unlock, definition.minimumLevel(), pretty(definition.itemName()));
        } else {
            message(player, "Salvage.Skills.ScrapCollector.Locked", unlock);
        }
    }

    private static int findRepairMaterial(
            PlayerInventory inventory, Item material, boolean enchantedAllowed) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack candidate = inventory.getStack(slot);
            if (candidate.isOf(material)
                    && (enchantedAllowed || !EnchantmentHelper.hasEnchantments(candidate))) {
                return slot;
            }
        }
        return -1;
    }

    private static void consumeOne(PlayerInventory inventory, int slot) {
        ItemStack material = inventory.getStack(slot);
        material.decrement(1);
        if (material.isEmpty()) {
            inventory.setStack(slot, ItemStack.EMPTY);
        }
        inventory.markDirty();
    }

    private static int maximumDurability(ItemStack item, int configured) {
        int stackMaximum = item.getMaxDamage();
        return stackMaximum > 0 ? stackMaximum : Math.max(1, configured);
    }

    private static void spawnTowardsPlayer(
            ServerWorld world, BlockPos pos, ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        Vec3d origin = Vec3d.ofCenter(pos).add(0.0D, 1.1D, 0.0D);
        Vec3d target = player.getPos().add(0.0D, 0.5D, 0.0D);
        double distance = origin.distanceTo(target);
        double speed = Math.min(0.6D, Math.max(0.3D, distance * 0.2D));
        Vec3d direction = target.subtract(origin);
        Vec3d velocity = direction.lengthSquared() == 0.0D
                ? Vec3d.ZERO : direction.normalize().multiply(speed);
        ItemEntity entity = new ItemEntity(
                world, origin.x, origin.y, origin.z, stack, velocity.x, velocity.y, velocity.z);
        entity.setOwner(player.getUuid());
        world.spawnEntity(entity);
    }

    private static boolean protectedInteractionAllowed(
            ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        String worldId = world.getRegistryKey().getValue().toString();
        return api.protection().canInteract(
                player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ())
                && api.protection().canModify(
                        player.getUuid(), worldId, pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean matchesBlock(Identifier clicked, String configuredName) {
        String configured = configuredName.trim().toLowerCase(Locale.ROOT);
        Identifier configuredId = configured.indexOf(':') >= 0
                ? Identifier.tryParse(configured) : Identifier.ofVanilla(configured);
        return configuredId != null && configuredId.equals(clicked);
    }

    private static int level(ServerPlayerEntity player, io.github.njw3995.fabricmmo.api.NamespacedId skill) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), skill).level();
    }

    private static boolean roll(ServerWorld world, double chancePercent) {
        return chancePercent >= 100.0D
                || chancePercent > 0.0D && world.getRandom().nextDouble() * 100.0D < chancePercent;
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static void prompt(
            ServerPlayerEntity player, UtilityAnvilConfirmationService.Kind kind) {
        message(player, "Skills.ConfirmOrCancel", UtilityAnvilMessages.localizedSkillName(kind));
    }

    private static void message(ServerPlayerEntity player, String key, Object... values) {
        player.sendMessage(UtilityAnvilMessages.text(key, values), false);
    }

    private static void play(
            ServerPlayerEntity player,
            UtilityAnvilSoundSettings.Sound configured,
            SoundCategory category) {
        if (!configured.enabled() || configured.volume() <= 0.0D) {
            return;
        }
        Identifier id = Identifier.tryParse(configured.id());
        if (id == null) {
            LOGGER.warn("Skipping invalid utility anvil CustomSoundId: {}", configured.id());
            return;
        }
        player.playSoundToPlayer(
                SoundEvent.of(id), category, (float) configured.volume(), (float) configured.pitch());
    }

    private static String pretty(String upstreamName) {
        String[] words = upstreamName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
