package io.github.njw3995.fabricmmo.core.skill.repair;

import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/** Complete repair.vanilla.yml loader; unavailable Minecraft materials are filtered at runtime. */
public final class RepairDefinitionTable {
    private final Map<String, RepairDefinition> entries;

    private RepairDefinitionTable(Map<String, RepairDefinition> entries) {
        this.entries = Map.copyOf(entries);
    }

    public static RepairDefinitionTable load(Path file) throws IOException {
        FlatYamlConfig yaml = FlatYamlConfig.load(file);
        TreeSet<String> names = new TreeSet<>();
        for (String path : yaml.valuesWithPrefix("Repairables.").keySet()) {
            String[] parts = path.split("\\.");
            if (parts.length >= 3) {
                names.add(parts[1]);
            }
        }
        Map<String, RepairDefinition> definitions = new LinkedHashMap<>();
        for (String name : names) {
            String prefix = "Repairables." + name + '.';
            UtilityItemType itemType = UtilityItemType.parse(
                    yaml.string(prefix + "ItemType", UtilityItemInference.itemType(name).name()));
            UtilityMaterialCategory category = UtilityMaterialCategory.parse(
                    yaml.string(prefix + "ItemMaterialCategory",
                            UtilityItemInference.materialCategory(name).name()));
            String material = yaml.string(prefix + "RepairMaterial",
                    UtilityItemInference.material(name)).toUpperCase(Locale.ROOT);
            int quantity = yaml.integer(prefix + "MinimumQuantity",
                    UtilityItemInference.recipeQuantity(name));
            definitions.put(name, new RepairDefinition(
                    name,
                    yaml.integer(prefix + "MinimumLevel", 0),
                    yaml.decimal(prefix + "XpMultiplier", 1.0D),
                    itemType,
                    category,
                    material,
                    quantity,
                    yaml.integer(prefix + "MaximumDurability", 0)));
        }
        return new RepairDefinitionTable(definitions);
    }

    public Map<String, RepairDefinition> entries() {
        return entries;
    }

    public Optional<RepairDefinition> find(String itemName) {
        Objects.requireNonNull(itemName, "itemName");
        return Optional.ofNullable(entries.get(itemName.toUpperCase(Locale.ROOT)));
    }
}
