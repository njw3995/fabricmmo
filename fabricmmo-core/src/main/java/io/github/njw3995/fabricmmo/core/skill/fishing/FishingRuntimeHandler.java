package io.github.njw3995.fabricmmo.core.skill.fishing;

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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Server-authoritative Fishing mechanics invoked from the bobber mixin. */
public final class FishingRuntimeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO/Fishing");
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();

    private FishingRuntimeHandler() {
    }

    public static FishingSettings.WaitBounds waitBounds(
            FishingBobberEntity bobber,
            int lureReductionTicks) {
        ServerPlayerEntity player = player(bobber);
        if (!masterAnglerActive(player)) {
            return new FishingSettings.WaitBounds(100, 600, 0, 0);
        }
        boolean boat = player.hasVehicle() && player.getVehicle() instanceof BoatEntity;
        return FabricMmoFabricRuntime.fishingSettings().masterAnglerBounds(
                level(player), boat, Math.max(0, lureReductionTicks));
    }

    /**
     * Vanilla subtracts Lure after choosing the wait value. Master Angler disables that separate
     * subtraction because the converted Lure bonus is already included in the maximum bound.
     */
    public static int lureReductionTicks(FishingBobberEntity bobber, int vanillaReductionTicks) {
        return masterAnglerActive(player(bobber)) ? 0 : vanillaReductionTicks;
    }

    public static ObjectArrayList<ItemStack> processCatch(
            FishingBobberEntity bobber,
            ItemStack rod,
            ObjectArrayList<ItemStack> generated) {
        ServerPlayerEntity player = player(bobber);
        if (!available(player) || generated.isEmpty()) {
            return generated;
        }
        ServerWorld world = (ServerWorld) bobber.getWorld();
        FishingSettings settings = FabricMmoFabricRuntime.fishingSettings();
        if (settings.exploitFixEnabled()) {
            FishingAntiExploitTracker.Decision decision =
                    FabricMmoFabricRuntime.fishingAntiExploit().evaluate(
                            player.getUuid(), bobber.getX(), bobber.getY(), bobber.getZ(),
                            System.currentTimeMillis());
            if (decision.lowResourcesWarning()) {
                player.sendMessage(FishingMessages.lowResources(decision.moveRange()), false);
            }
            if (decision.scarcityWarning()) {
                player.sendMessage(FishingMessages.scarcity(decision.moveRange()), false);
            } else if (decision.scaredWarning()) {
                player.sendMessage(FishingMessages.scared(), false);
            }
            if (decision.denied()) {
                return new ObjectArrayList<>();
            }
        }

        ItemStack original = generated.get(0).copy();
        if (settings.overrideVanillaTreasures() && !isFish(original)) {
            original = new ItemStack(Items.SALMON);
        }
        int fishXp = FabricMmoFabricRuntime.fishingXpTable().xpFor(original.getItem());
        int treasureXp = 0;
        ItemStack result = original;
        boolean treasureFound = false;

        int fishingLevel = level(player);
        int tier = settings.treasureHunterRank(fishingLevel);
        if (settings.dropsEnabled()
                && tier > 0
                && allowed(player, PermissionNodes.FISHING_TREASURE_HUNTER, true)) {
            int luck = enchantmentLevel(world, rod, Enchantments.LUCK_OF_THE_SEA);
            FishingTreasureRoller roller = new FishingTreasureRoller(
                    FabricMmoFabricRuntime.fishingTreasures());
            Optional<FishingTreasureRoller.TreasureRoll> rolled = roller.rollTreasure(
                    tier,
                    luck,
                    settings.lureModifierPercent(),
                    world.getRandom().nextDouble(),
                    world.getRandom().nextInt());
            if (rolled.isPresent()) {
                FishingTreasure treasure = rolled.orElseThrow().treasure();
                result = createTreasureStack(world, treasure);
                treasureXp = treasure.xp();
                treasureFound = true;
                boolean enchanted = treasure.enchantedBook();
                if (!enchanted
                        && fishingLevel >= settings.magicHunterUnlockLevel()
                        && allowed(player, PermissionNodes.FISHING_MAGIC_HUNTER, true)
                        && EnchantmentHelper.canHaveEnchantments(result)) {
                    enchanted = applyMagicHunter(world, result, tier, roller);
                }
                if (enchanted) {
                    player.sendMessage(FishingMessages.magicFound(), false);
                }
            }
        }

        if (treasureFound && settings.extraFish()) {
            ItemEntity extra = new ItemEntity(
                    world,
                    player.getX(),
                    player.getEyeY(),
                    player.getZ(),
                    original.copy());
            world.spawnEntity(extra);
        }
        awardXp(player, world, result, fishXp + treasureXp, CoreXpSources.FISHING_CATCH);
        ObjectArrayList<ItemStack> replacement = new ObjectArrayList<>();
        replacement.add(result);
        return replacement;
    }

    public static int vanillaXp(FishingBobberEntity bobber, int vanillaXp) {
        ServerPlayerEntity player = player(bobber);
        if (!available(player)
                || !allowed(player, PermissionNodes.FISHING_VANILLA_XP_BOOST, true)) {
            return vanillaXp;
        }
        return vanillaXp * FabricMmoFabricRuntime.fishingSettings()
                .vanillaXpMultiplier(level(player));
    }

    public static void shake(FishingBobberEntity bobber) {
        ServerPlayerEntity player = player(bobber);
        Entity hooked = bobber.getHookedEntity();
        if (!available(player)
                || !(hooked instanceof LivingEntity target)
                || !allowed(player, PermissionNodes.FISHING_SHAKE, true)) {
            return;
        }
        ServerWorld world = (ServerWorld) bobber.getWorld();
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (!api.protection().canDamage(
                player.getUuid(), target.getUuid(), world.getRegistryKey().getValue().toString())) {
            return;
        }
        int fishingLevel = level(player);
        FishingSettings settings = FabricMmoFabricRuntime.fishingSettings();
        if (settings.shakeRank(fishingLevel) <= 0) {
            return;
        }
        boolean lucky = allowed(player, PermissionNodes.FISHING_LUCKY, false);
        if (!FishingProbability.succeeds(
                world.getRandom().nextDouble(), settings.shakeChance(fishingLevel, lucky))) {
            return;
        }
        String entityPath = Registries.ENTITY_TYPE.getId(target.getType()).getPath();
        FishingTreasureRoller roller = new FishingTreasureRoller(
                FabricMmoFabricRuntime.fishingTreasures());
        Optional<FishingShakeTreasure> rolled = roller.rollShake(
                entityPath,
                world.getRandom().nextInt(100));
        if (rolled.isEmpty()) {
            return;
        }
        FishingShakeTreasure treasure = rolled.orElseThrow();
        ItemStack drop = treasure.inventorySteal()
                ? stealInventoryItem(target, treasure.wholeStacks(), world.getRandom())
                : createShakeStack(world, treasure);
        if (drop.isEmpty()) {
            return;
        }
        if (target instanceof SheepEntity sheep && drop.isOf(Items.WHITE_WOOL)) {
            if (sheep.isSheared()) {
                return;
            }
            sheep.setSheared(true);
        }
        world.spawnEntity(new ItemEntity(
                world, target.getX(), target.getY(), target.getZ(), drop));
        float damage = Math.min(Math.max(target.getMaxHealth() / 4.0F, 1.0F), 10.0F);
        target.damage(world.getDamageSources().playerAttack(player), damage);
        awardXp(player, world, drop, settings.shakeXp(), CoreXpSources.FISHING_SHAKE);
    }

    public static boolean iceFishing(FishingBobberEntity bobber, BlockPos hitPos) {
        ServerPlayerEntity player = player(bobber);
        if (!available(player)
                || !allowed(player, PermissionNodes.FISHING_ICE_FISHING, true)) {
            return false;
        }
        ServerWorld world = (ServerWorld) bobber.getWorld();
        BlockState state = world.getBlockState(hitPos);
        FishingSettings settings = FabricMmoFabricRuntime.fishingSettings();
        if (!state.isOf(Blocks.ICE) || level(player) < settings.iceFishingUnlockLevel()) {
            return false;
        }
        boolean icyBiome = world.getBiome(hitPos).value().isCold(hitPos);
        boolean waterBelow = world.getBlockState(hitPos.down(3)).isOf(Blocks.WATER);
        if (!icyBiome && !waterBelow) {
            return false;
        }
        String worldId = world.getRegistryKey().getValue().toString();
        FabricMmoApi api = FabricMmoFabricRuntime.requireApi();
        if (!api.protection().canModify(
                player.getUuid(), worldId, hitPos.getX(), hitPos.getY(), hitPos.getZ())) {
            return false;
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos relative = hitPos.add(x, 0, z);
                if (world.getBlockState(relative).isOf(Blocks.ICE)
                        && api.protection().canModify(
                                player.getUuid(), worldId,
                                relative.getX(), relative.getY(), relative.getZ())) {
                    world.setBlockState(relative, Blocks.WATER.getDefaultState(), 3);
                }
            }
        }
        world.spawnParticles(
                ParticleTypes.SPLASH,
                hitPos.getX() + 0.5D,
                hitPos.getY() + 0.5D,
                hitPos.getZ() + 0.5D,
                8,
                0.4D,
                0.1D,
                0.4D,
                0.05D);
        return true;
    }

    public static void playerDisconnected(java.util.UUID playerId) {
        if (FabricMmoFabricRuntime.running()) {
            FabricMmoFabricRuntime.fishingAntiExploit().remove(playerId);
        }
    }

    public static void reset() {
        if (FabricMmoFabricRuntime.running()) {
            FabricMmoFabricRuntime.fishingAntiExploit().clear();
        }
    }

    private static ItemStack createTreasureStack(ServerWorld world, FishingTreasure treasure) {
        ItemStack stack = new ItemStack(treasure.item(), treasure.amount());
        if (treasure.enchantedBook()) {
            applyRandomBookEnchantment(world, stack, treasure);
        } else {
            randomizeDurability(stack, world.getRandom());
        }
        return stack;
    }

    private static void applyRandomBookEnchantment(
            ServerWorld world,
            ItemStack stack,
            FishingTreasure treasure) {
        Registry<Enchantment> registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        List<BookEnchant> legal = new ArrayList<>();
        registry.streamEntries().forEach(entry -> {
            Identifier id = registry.getId(entry.value());
            if (id == null || !treasure.allowsEnchantment(NamespacedId.parse(id.toString()))) {
                return;
            }
            for (int enchantmentLevel = 1;
                    enchantmentLevel <= entry.value().getMaxLevel();
                    enchantmentLevel++) {
                legal.add(new BookEnchant(entry, enchantmentLevel));
            }
        });
        if (legal.isEmpty()) {
            LOGGER.warn("Fishing enchanted-book treasure has no legal enchantments");
            return;
        }
        BookEnchant selected = legal.get(world.getRandom().nextInt(legal.size()));
        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
                ItemEnchantmentsComponent.DEFAULT);
        builder.add(selected.enchantment(), selected.level());
        stack.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
    }

    private static ItemStack createShakeStack(
            ServerWorld world,
            FishingShakeTreasure treasure) {
        String potionId = switch (treasure.potionType().toUpperCase(java.util.Locale.ROOT)) {
            case "POISON" -> "poison";
            case "INSTANT_HEAL", "HEAL" -> "healing";
            case "FIRE_RESISTANCE" -> "fire_resistance";
            case "SPEED", "SWIFTNESS" -> "swiftness";
            default -> "";
        };
        if (!potionId.isEmpty()) {
            Registry<Potion> potions = world.getRegistryManager().get(RegistryKeys.POTION);
            Optional<RegistryEntry.Reference<Potion>> potion = potions.getEntry(
                    Identifier.of("minecraft", potionId));
            if (potion.isPresent()) {
                ItemStack stack = PotionContentsComponent.createStack(
                        treasure.item(), potion.orElseThrow());
                stack.setCount(treasure.amount());
                return stack;
            }
        }
        return new ItemStack(treasure.item(), treasure.amount());
    }

    private static ItemStack stealInventoryItem(
            LivingEntity target,
            boolean wholeStacks,
            Random random) {
        if (!(target instanceof PlayerEntity targetPlayer)) {
            return ItemStack.EMPTY;
        }
        var inventory = targetPlayer.getInventory();
        int slot = random.nextInt(inventory.size());
        ItemStack selected = inventory.getStack(slot);
        if (selected.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (wholeStacks) {
            return inventory.removeStack(slot);
        }
        ItemStack stolen = selected.copyWithCount(1);
        selected.decrement(1);
        return stolen;
    }

    private static boolean applyMagicHunter(
            ServerWorld world,
            ItemStack stack,
            int tier,
            FishingTreasureRoller roller) {
        Optional<FishingRarity> rarity = roller.rollEnchantmentRarity(
                tier, world.getRandom().nextDouble());
        if (rarity.isEmpty()) {
            return false;
        }
        List<FishingEnchantmentTreasure> candidates = new ArrayList<>(
                FabricMmoFabricRuntime.fishingTreasures().enchantments(rarity.orElseThrow()));
        Collections.shuffle(candidates, new java.util.Random(world.getRandom().nextLong()));
        Registry<Enchantment> registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        Map<RegistryEntry<Enchantment>, Integer> chosen = new HashMap<>();
        int specificChance = 1;
        for (FishingEnchantmentTreasure candidate : candidates) {
            Optional<RegistryEntry.Reference<Enchantment>> entry = registry.getEntry(
                    Identifier.of(candidate.enchantmentId().namespace(),
                            candidate.enchantmentId().path()));
            if (entry.isEmpty() || !entry.orElseThrow().value().isAcceptableItem(stack)) {
                continue;
            }
            RegistryEntry<Enchantment> enchantment = entry.orElseThrow();
            if (!FabricMmoFabricRuntime.fishingSettings().allowConflictingEnchants()) {
                boolean conflict = stack.getEnchantments().getEnchantments().stream()
                        .anyMatch(existing -> !Enchantment.canBeCombined(existing, enchantment))
                        || chosen.keySet().stream()
                        .anyMatch(existing -> !Enchantment.canBeCombined(existing, enchantment));
                if (conflict) {
                    continue;
                }
            }
            if (world.getRandom().nextInt(specificChance) != 0) {
                continue;
            }
            chosen.put(enchantment, candidate.level());
            specificChance *= 2;
        }
        chosen.forEach(stack::addEnchantment);
        return !chosen.isEmpty();
    }

    private static int enchantmentLevel(
            ServerWorld world,
            ItemStack stack,
            net.minecraft.registry.RegistryKey<Enchantment> key) {
        return world.getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                .getEntry(key)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack))
                .orElse(0);
    }

    private static void randomizeDurability(ItemStack stack, Random random) {
        if (stack.isDamageable() && stack.getMaxDamage() > 0) {
            stack.setDamage(random.nextInt(stack.getMaxDamage()));
        }
    }

    private static boolean isFish(ItemStack stack) {
        return stack.isOf(Items.COD)
                || stack.isOf(Items.SALMON)
                || stack.isOf(Items.TROPICAL_FISH)
                || stack.isOf(Items.PUFFERFISH);
    }

    private static void awardXp(
            ServerPlayerEntity player,
            ServerWorld world,
            ItemStack reward,
            int xp,
            NamespacedId source) {
        if (xp <= 0) {
            return;
        }
        XpAwardResult result = FabricMmoFabricRuntime.requireApi().progression().award(
                new XpAwardRequest(
                        player.getUuid(),
                        CoreSkills.FISHING,
                        source,
                        xp,
                        PlayerProgressionContext.enrich(
                                player,
                                Map.of(
                                        "item", Registries.ITEM.getId(reward.getItem()).toString(),
                                        "world", world.getRegistryKey().getValue().toString(),
                                        "upstreamReason", "PVE",
                                        "upstreamSource", "SELF"),
                                FabricMmoFabricRuntime.progressionSettings(),
                                CoreSkills.FISHING)));
        if (result.status() != XpAwardResult.Status.APPLIED) {
            LOGGER.warn("Fishing XP award for {} was not applied: {}",
                    player.getName().getString(), result.detail());
        } else if (result.newLevel() > result.oldLevel()) {
            player.sendMessage(net.minecraft.text.Text.literal(
                    "Fishing increased to " + result.newLevel() + "."), false);
        }
    }

    private static int level(ServerPlayerEntity player) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(player.getUuid(), CoreSkills.FISHING).level();
    }

    private static ServerPlayerEntity player(FishingBobberEntity bobber) {
        PlayerEntity owner = bobber.getPlayerOwner();
        return owner instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
    }

    private static boolean masterAnglerActive(ServerPlayerEntity player) {
        return available(player)
                && allowed(player, PermissionNodes.FISHING_MASTER_ANGLER, true)
                && FabricMmoFabricRuntime.fishingSettings().masterAnglerRank(level(player)) > 0;
    }

    private static boolean available(ServerPlayerEntity player) {
        return player != null
                && FabricMmoFabricRuntime.running()
                && !player.isCreative()
                && player.getWorld() instanceof ServerWorld world
                && !FabricMmoFabricRuntime.isWorldBlacklisted(world)
                && allowed(player, PermissionNodes.FISHING, true);
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private record BookEnchant(RegistryEntry<Enchantment> enchantment, int level) {
    }
}
