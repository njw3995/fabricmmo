package io.github.njw3995.fabricmmo.core.skill.alchemy;

import io.github.njw3995.fabricmmo.api.event.AlchemyBrewEvent;
import io.github.njw3995.fabricmmo.api.event.AlchemyCatalysisEvent;
import io.github.njw3995.fabricmmo.api.progression.XpAwardRequest;
import io.github.njw3995.fabricmmo.core.fabric.FabricMmoFabricRuntime;
import io.github.njw3995.fabricmmo.core.access.AlchemyBrewingStandAccess;
import io.github.njw3995.fabricmmo.core.permission.FabricCommandPermissionService;
import io.github.njw3995.fabricmmo.core.permission.PermissionNodes;
import io.github.njw3995.fabricmmo.core.progression.CoreXpSources;
import io.github.njw3995.fabricmmo.core.progression.PlayerProgressionContext;
import io.github.njw3995.fabricmmo.core.skill.CoreSkills;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Server-authoritative brewing controller for mcMMO 2.3.000 Alchemy. */
public final class AlchemyRuntimeHandler {
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int DEFAULT_BREW_TICKS = 400;
    private static final int BLAZE_POWDER_FUEL = 20;
    private static final FabricCommandPermissionService PERMISSIONS =
            new FabricCommandPermissionService();
    private static final Set<BrewingStandBlockEntity> ACTIVE_STANDS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private AlchemyRuntimeHandler() {}

    /**
     * @return true when FabricMMO owns this tick and vanilla brewing must be skipped.
     */
    public static boolean tick(
            World world,
            BlockPos pos,
            BlockState state,
            BrewingStandBlockEntity stand,
            AlchemyBrewingStandAccess access) {
        if (!(world instanceof ServerWorld serverWorld) || !FabricMmoFabricRuntime.running()) {
            return false;
        }
        UUID ownerId = access.fabricmmo$alchemyOwner();
        if (ownerId == null || FabricMmoFabricRuntime.isWorldBlacklisted(serverWorld)) {
            if (access.fabricmmo$customAlchemyActive()) cancel(access, stand);
            return false;
        }

        ServerPlayerEntity owner = serverWorld.getServer().getPlayerManager().getPlayer(ownerId);
        int tier = ingredientTier(owner);
        BrewPlan plan = plan(access.fabricmmo$alchemyInventory(), tier);

        if (access.fabricmmo$customAlchemyActive()) {
            ACTIVE_STANDS.add(stand);
            Identifier currentIngredient = ingredientId(access.fabricmmo$alchemyInventory());
            if (currentIngredient == null
                    || !currentIngredient.toString().equals(access.fabricmmo$customAlchemyIngredient())
                    || plan == null) {
                cancel(access, stand);
                return true;
            }
            double remaining = access.fabricmmo$customAlchemyRemaining()
                    - access.fabricmmo$customAlchemySpeed();
            if (remaining < Math.max(access.fabricmmo$customAlchemySpeed(), 2.0D)) {
                finish(serverWorld, pos, stand, access, plan, ownerId, owner, false);
            } else {
                access.fabricmmo$setCustomAlchemyRemaining(remaining);
                stand.markDirty();
            }
            return true;
        }

        if (plan == null) return false;
        if (!ensureFuel(access)) return true;

        double speed = 1.0D;
        if (owner != null && allowed(owner, PermissionNodes.ALCHEMY_CATALYSIS, true)) {
            AlchemySettings settings = FabricMmoFabricRuntime.alchemySettings();
            int level = level(owner);
            speed = AlchemyFormula.catalysisSpeed(
                    level,
                    settings.catalysisUnlock(),
                    settings.catalysisMaxBonusLevel(),
                    settings.catalysisMinSpeed(),
                    settings.catalysisMaxSpeed(),
                    allowed(owner, PermissionNodes.ALCHEMY_LUCKY, false));
            AlchemyCatalysisEvent event = new AlchemyCatalysisEvent(
                    ownerId,
                    serverWorld.getRegistryKey().getValue().toString(),
                    pos.asLong(),
                    speed);
            FabricMmoFabricRuntime.requireApi().events().publish(event);
            speed = event.cancelled() ? 1.0D : event.speed();
        }

        access.fabricmmo$setAlchemyFuel(access.fabricmmo$alchemyFuel() - 1);
        Identifier ingredient = ingredientId(access.fabricmmo$alchemyInventory());
        access.fabricmmo$beginCustomAlchemy(
                DEFAULT_BREW_TICKS,
                speed,
                ingredient == null ? "" : ingredient.toString());
        ACTIVE_STANDS.add(stand);
        stand.markDirty();
        return true;
    }

    public static void setOwner(BrewingStandBlockEntity stand, ServerPlayerEntity player) {
        if (!FabricMmoFabricRuntime.running()
                || FabricMmoFabricRuntime.isWorldBlacklisted(player.getServerWorld())
                || !allowed(player, PermissionNodes.ALCHEMY, true)) {
            return;
        }
        ((AlchemyBrewingStandAccess) stand).fabricmmo$setAlchemyOwner(player.getUuid());
        stand.markDirty();
    }

    /** Null means vanilla should decide; false explicitly blocks a hopper move. */
    public static Boolean allowHopperInsert(int slot, ItemStack stack) {
        if (!FabricMmoFabricRuntime.running()) return null;
        AlchemySettings settings = FabricMmoFabricRuntime.alchemySettings();
        if (slot == INGREDIENT_SLOT && settings.preventHopperIngredients()
                && !isPotionItem(stack)) {
            return false;
        }
        if (slot >= 0 && slot < INGREDIENT_SLOT && settings.preventHopperBottles()
                && isPotionItem(stack)) {
            return false;
        }
        return null;
    }

    public static void finishAll(MinecraftServer server) {
        List<BrewingStandBlockEntity> stands;
        synchronized (ACTIVE_STANDS) {
            stands = List.copyOf(ACTIVE_STANDS);
        }
        for (BrewingStandBlockEntity stand : stands) {
            if (!(stand.getWorld() instanceof ServerWorld world)) continue;
            AlchemyBrewingStandAccess access = (AlchemyBrewingStandAccess) stand;
            if (!access.fabricmmo$customAlchemyActive()) continue;
            UUID ownerId = access.fabricmmo$alchemyOwner();
            if (ownerId == null) continue;
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerId);
            BrewPlan plan = plan(access.fabricmmo$alchemyInventory(), ingredientTier(owner));
            if (plan != null) {
                finish(world, stand.getPos(), stand, access, plan, ownerId, owner, true);
            } else {
                cancel(access, stand);
            }
        }
    }

    public static void reset() {
        synchronized (ACTIVE_STANDS) {
            ACTIVE_STANDS.clear();
        }
    }

    private static BrewPlan plan(DefaultedList<ItemStack> inventory, int tier) {
        if (inventory.size() <= FUEL_SLOT) return null;
        Identifier ingredient = ingredientId(inventory);
        if (ingredient == null
                || !FabricMmoFabricRuntime.alchemyPotionConfig()
                        .ingredientsForTier(tier).contains(ingredient)) {
            return null;
        }
        ArrayList<SlotBrew> outputs = new ArrayList<>(3);
        for (int slot = 0; slot < INGREDIENT_SLOT; slot++) {
            ItemStack inputStack = inventory.get(slot);
            if (inputStack.isEmpty() || !isPotionItem(inputStack)) continue;
            AlchemyPotionDefinition input = AlchemyPotionCodec.match(
                    FabricMmoFabricRuntime.alchemyPotionConfig(), inputStack);
            if (input == null) continue;
            AlchemyPotionDefinition output = FabricMmoFabricRuntime.alchemyPotionConfig()
                    .child(input, ingredient);
            if (output != null) outputs.add(new SlotBrew(slot, input, output, inputStack.getCount()));
        }
        return outputs.isEmpty() ? null : new BrewPlan(ingredient, List.copyOf(outputs));
    }

    private static boolean ensureFuel(AlchemyBrewingStandAccess access) {
        if (access.fabricmmo$alchemyFuel() > 0) return true;
        DefaultedList<ItemStack> inventory = access.fabricmmo$alchemyInventory();
        ItemStack fuelStack = inventory.get(FUEL_SLOT);
        if (!fuelStack.isOf(Items.BLAZE_POWDER) || fuelStack.isEmpty()) return false;
        fuelStack.decrement(1);
        access.fabricmmo$setAlchemyFuel(BLAZE_POWDER_FUEL);
        return true;
    }

    private static void finish(
            ServerWorld world,
            BlockPos pos,
            BrewingStandBlockEntity stand,
            AlchemyBrewingStandAccess access,
            BrewPlan plan,
            UUID ownerId,
            ServerPlayerEntity owner,
            boolean shutdown) {
        List<String> outputIds = plan.outputs().stream()
                .map(output -> output.output().id()).toList();
        AlchemyBrewEvent event = new AlchemyBrewEvent(
                ownerId,
                world.getRegistryKey().getValue().toString(),
                pos.asLong(),
                plan.ingredient().toString(),
                outputIds);
        FabricMmoFabricRuntime.requireApi().events().publish(event);
        if (event.cancelled() && !shutdown) {
            access.fabricmmo$clearCustomAlchemy();
            ACTIVE_STANDS.remove(stand);
            stand.markDirty();
            return;
        }

        DefaultedList<ItemStack> inventory = access.fabricmmo$alchemyInventory();
        for (SlotBrew output : plan.outputs()) {
            inventory.set(output.slot(), AlchemyPotionCodec.create(output.output(), output.amount()));
            if (owner != null) {
                int stage = AlchemyFormula.potionStage(
                        output.input().water(), output.input().shape(), output.output().shape());
                awardXp(owner, stage, output.input(), output.output());
            }
        }
        inventory.get(INGREDIENT_SLOT).decrement(1);
        access.fabricmmo$clearCustomAlchemy();
        ACTIVE_STANDS.remove(stand);
        stand.markDirty();
    }

    private static void awardXp(
            ServerPlayerEntity player,
            int stage,
            AlchemyPotionDefinition input,
            AlchemyPotionDefinition output) {
        double xp = FabricMmoFabricRuntime.alchemySettings().xpForStage(stage);
        if (xp <= 0.0D) return;
        Map<String, String> context = PlayerProgressionContext.enrich(
                player,
                Map.of(
                        "context", "POTION_BREWING",
                        "stage", Integer.toString(stage),
                        "input", input.id(),
                        "output", output.id()),
                FabricMmoFabricRuntime.progressionSettings(),
                CoreSkills.ALCHEMY);
        FabricMmoFabricRuntime.requireApi().progression().award(new XpAwardRequest(
                player.getUuid(), CoreSkills.ALCHEMY, CoreXpSources.ALCHEMY_BREW, xp, context));
    }

    private static int ingredientTier(ServerPlayerEntity owner) {
        if (owner == null || !allowed(owner, PermissionNodes.ALCHEMY_CONCOCTIONS, true)) return 1;
        return FabricMmoFabricRuntime.alchemySettings().concoctionsTier(level(owner));
    }

    private static int level(ServerPlayerEntity owner) {
        return FabricMmoFabricRuntime.requireApi().progression()
                .query(owner.getUuid(), CoreSkills.ALCHEMY).level();
    }

    private static boolean allowed(ServerPlayerEntity player, String node, boolean fallback) {
        return PERMISSIONS.hasPermission(player.getCommandSource(), node, fallback);
    }

    private static Identifier ingredientId(DefaultedList<ItemStack> inventory) {
        if (inventory.size() <= INGREDIENT_SLOT || inventory.get(INGREDIENT_SLOT).isEmpty()) {
            return null;
        }
        return Registries.ITEM.getId(inventory.get(INGREDIENT_SLOT).getItem());
    }

    private static boolean isPotionItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION)
                || stack.isOf(Items.LINGERING_POTION)
                || stack.contains(DataComponentTypes.POTION_CONTENTS);
    }

    private static void cancel(AlchemyBrewingStandAccess access, BrewingStandBlockEntity stand) {
        access.fabricmmo$clearCustomAlchemy();
        ACTIVE_STANDS.remove(stand);
        stand.markDirty();
    }

    private record SlotBrew(
            int slot,
            AlchemyPotionDefinition input,
            AlchemyPotionDefinition output,
            int amount) {}

    private record BrewPlan(Identifier ingredient, List<SlotBrew> outputs) {}
}
