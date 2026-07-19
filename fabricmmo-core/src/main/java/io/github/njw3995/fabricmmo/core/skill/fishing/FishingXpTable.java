package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

/** Configurable mcMMO Fishing item XP table. */
public final class FishingXpTable {
    private final Map<NamespacedId, Integer> values;

    private FishingXpTable(Map<NamespacedId, Integer> values) {
        this.values = Map.copyOf(values);
    }

    public static FishingXpTable load(Path experienceFile) throws IOException {
        HashMap<NamespacedId, Integer> loaded = new HashMap<>(FishingXpDefaults.values());
        FlatYamlConfig yaml = FlatYamlConfig.load(experienceFile);
        yaml.valuesWithPrefix("Experience_Values.Fishing.").forEach((path, value) -> {
            String[] parts = path.split("\\.");
            if (parts.length != 3 || parts[2].equalsIgnoreCase("Shake")) {
                return;
            }
            loaded.put(new NamespacedId(
                    "minecraft", parts[2].toLowerCase(Locale.ROOT)), Integer.parseInt(value));
        });
        return new FishingXpTable(loaded);
    }

    public int xpFor(Item item) {
        return values.getOrDefault(NamespacedId.parse(Registries.ITEM.getId(item).toString()), 0);
    }

    public Map<NamespacedId, Integer> values() {
        return values;
    }
}
