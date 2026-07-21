package io.github.njw3995.fabricmmo.core.skill.repair;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runtime-resolved Minecraft 1.21.1 view of the complete upstream utility mappings. */
public final class MinecraftUtilityDefinitions {
    private static final Logger LOGGER = LoggerFactory.getLogger("FabricMMO");

    private final Map<Item, ResolvedRepair> repairs;
    private final Map<Item, ResolvedSalvage> salvages;

    private MinecraftUtilityDefinitions(
            Map<Item, ResolvedRepair> repairs,
            Map<Item, ResolvedSalvage> salvages) {
        this.repairs = Map.copyOf(repairs);
        this.salvages = Map.copyOf(salvages);
    }

    public static MinecraftUtilityDefinitions resolve(
            RepairDefinitionTable repairTable,
            SalvageDefinitionTable salvageTable) {
        IdentityHashMap<Item, ResolvedRepair> repairs = new IdentityHashMap<>();
        IdentityHashMap<Item, ResolvedSalvage> salvages = new IdentityHashMap<>();
        int skippedRepair = 0;
        int skippedSalvage = 0;

        for (RepairDefinition definition : repairTable.entries().values()) {
            Optional<Item> item = item(definition.itemName());
            Optional<Item> material = item(definition.repairMaterialName());
            if (item.isEmpty() || material.isEmpty()) {
                skippedRepair++;
                continue;
            }
            repairs.put(item.orElseThrow(),
                    new ResolvedRepair(definition, item.orElseThrow(), material.orElseThrow()));
        }
        for (SalvageDefinition definition : salvageTable.entries().values()) {
            Optional<Item> item = item(definition.itemName());
            Optional<Item> material = item(definition.salvageMaterialName());
            if (item.isEmpty() || material.isEmpty()) {
                skippedSalvage++;
                continue;
            }
            salvages.put(item.orElseThrow(),
                    new ResolvedSalvage(definition, item.orElseThrow(), material.orElseThrow()));
        }
        LOGGER.info(
                "Loaded {} Repair and {} Salvage mappings for Minecraft 1.21.1 (skipped {} Repair and {} Salvage entries unavailable in this game version)",
                repairs.size(), salvages.size(), skippedRepair, skippedSalvage);
        return new MinecraftUtilityDefinitions(repairs, salvages);
    }

    public Optional<ResolvedRepair> repair(Item item) {
        return Optional.ofNullable(repairs.get(item));
    }

    public Optional<ResolvedSalvage> salvage(Item item) {
        return Optional.ofNullable(salvages.get(item));
    }

    public int repairCount() {
        return repairs.size();
    }

    public int salvageCount() {
        return salvages.size();
    }

    private static Optional<Item> item(String upstreamName) {
        Identifier id = Identifier.ofVanilla(upstreamName.toLowerCase(java.util.Locale.ROOT));
        return Registries.ITEM.getOrEmpty(id);
    }

    public record ResolvedRepair(RepairDefinition definition, Item item, Item material) {
    }

    public record ResolvedSalvage(SalvageDefinition definition, Item item, Item material) {
    }
}
