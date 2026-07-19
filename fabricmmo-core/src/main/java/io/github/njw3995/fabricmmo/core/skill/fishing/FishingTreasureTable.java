package io.github.njw3995.fabricmmo.core.skill.fishing;

import io.github.njw3995.fabricmmo.api.NamespacedId;
import io.github.njw3995.fabricmmo.core.config.FlatYamlConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** Strict scalar loader for upstream fishing_treasures.yml. */
public final class FishingTreasureTable {
    private static final Map<String, String> ENTITY_ALIASES = Map.of(
            "PIG_ZOMBIE", "zombified_piglin",
            "SNOWMAN", "snow_golem",
            "MOOSHROOM", "mooshroom");

    private final Map<FishingRarity, List<FishingTreasure>> treasures;
    private final Map<Integer, Map<FishingRarity, Double>> itemRates;
    private final Map<FishingRarity, List<FishingEnchantmentTreasure>> enchantments;
    private final Map<Integer, Map<FishingRarity, Double>> enchantmentRates;
    private final Map<String, List<FishingShakeTreasure>> shake;

    FishingTreasureTable(
            Map<FishingRarity, List<FishingTreasure>> treasures,
            Map<Integer, Map<FishingRarity, Double>> itemRates,
            Map<FishingRarity, List<FishingEnchantmentTreasure>> enchantments,
            Map<Integer, Map<FishingRarity, Double>> enchantmentRates,
            Map<String, List<FishingShakeTreasure>> shake) {
        this.treasures = immutableEnumLists(treasures);
        this.itemRates = immutableRates(itemRates);
        this.enchantments = immutableEnumLists(enchantments);
        this.enchantmentRates = immutableRates(enchantmentRates);
        this.shake = immutableLists(shake);
    }

    public static FishingTreasureTable load(Path file) throws IOException {
        FlatYamlConfig yaml = FlatYamlConfig.load(file);
        return new FishingTreasureTable(
                loadTreasures(yaml, loadBookFilters(file)),
                loadRates(yaml, "Item_Drop_Rates."),
                loadEnchantments(yaml),
                loadRates(yaml, "Enchantment_Drop_Rates."),
                loadShake(yaml));
    }

    /** Parses the PLAYER.INVENTORY Shake definition without touching Minecraft registries. */
    static ShakeInventoryDefinition loadPlayerInventoryShakeDefinition(Path file) throws IOException {
        return loadInventoryShakeDefinition(FlatYamlConfig.load(file), "PLAYER");
    }

    public List<FishingTreasure> treasures(FishingRarity rarity) {
        return treasures.getOrDefault(rarity, List.of());
    }

    public double itemRate(int tier, FishingRarity rarity) {
        return rate(itemRates, tier, rarity);
    }

    public List<FishingEnchantmentTreasure> enchantments(FishingRarity rarity) {
        return enchantments.getOrDefault(rarity, List.of());
    }

    public double enchantmentRate(int tier, FishingRarity rarity) {
        return rate(enchantmentRates, tier, rarity);
    }

    public List<FishingShakeTreasure> shake(String entityPath) {
        return shake.getOrDefault(normalizeEntity(entityPath), List.of());
    }

    public int treasureCount() {
        return treasures.values().stream().mapToInt(List::size).sum();
    }

    public int shakeEntityCount() {
        return shake.size();
    }

    private static Map<FishingRarity, List<FishingTreasure>> loadTreasures(
            FlatYamlConfig yaml,
            BookFilters bookFilters) {
        Map<String, Map<String, String>> entries = group(yaml, "Fishing.", 1);
        EnumMap<FishingRarity, List<FishingTreasure>> result = enumLists();
        entries.forEach((key, values) -> {
            Item item = resolveItem(key);
            if (item == null) {
                return;
            }
            String rarityValue = values.get("Rarity");
            String amountValue = values.get("Amount");
            String xpValue = values.get("XP");
            if (rarityValue == null || amountValue == null || xpValue == null) {
                return;
            }
            FishingRarity rarity = FishingRarity.parse(rarityValue);
            result.get(rarity).add(new FishingTreasure(
                    item,
                    Integer.parseInt(amountValue),
                    Integer.parseInt(xpValue),
                    rarity,
                    item == Items.ENCHANTED_BOOK ? bookFilters.whitelist() : Set.of(),
                    item == Items.ENCHANTED_BOOK ? bookFilters.blacklist() : Set.of()));
        });
        return result;
    }

    private static BookFilters loadBookFilters(Path file) throws IOException {
        Set<NamespacedId> whitelist = new HashSet<>();
        Set<NamespacedId> blacklist = new HashSet<>();
        boolean inBook = false;
        Set<NamespacedId> active = null;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String withoutComment = raw.split("#", 2)[0];
            if (withoutComment.isBlank()) {
                continue;
            }
            int indent = withoutComment.length() - withoutComment.stripLeading().length();
            String line = withoutComment.trim();
            if (indent == 4 && line.endsWith(":")) {
                inBook = line.equalsIgnoreCase("ENCHANTED_BOOK:");
                active = null;
                continue;
            }
            if (!inBook) {
                continue;
            }
            if (indent == 8 && line.equalsIgnoreCase("Enchantments_Whitelist:")) {
                active = whitelist;
                continue;
            }
            if (indent == 8 && line.equalsIgnoreCase("Enchantments_Blacklist:")) {
                active = blacklist;
                continue;
            }
            if (indent <= 8 && line.endsWith(":")) {
                active = null;
                continue;
            }
            if (active != null && line.startsWith("-")) {
                String configured = line.substring(1).trim().toLowerCase(Locale.ROOT);
                if (!configured.isEmpty()) {
                    active.add(configured.contains(":")
                            ? NamespacedId.parse(configured)
                            : new NamespacedId("minecraft", configured));
                }
            }
        }
        return new BookFilters(Set.copyOf(whitelist), Set.copyOf(blacklist));
    }

    private static Map<FishingRarity, List<FishingEnchantmentTreasure>> loadEnchantments(
            FlatYamlConfig yaml) {
        EnumMap<FishingRarity, List<FishingEnchantmentTreasure>> result = enumLists();
        yaml.valuesWithPrefix("Enchantments_Rarity.").forEach((path, value) -> {
            String[] parts = path.split("\\.");
            if (parts.length != 3) {
                return;
            }
            FishingRarity rarity = FishingRarity.parse(parts[1]);
            NamespacedId id = new NamespacedId(
                    "minecraft", parts[2].toLowerCase(Locale.ROOT));
            result.get(rarity).add(new FishingEnchantmentTreasure(
                    id, Integer.parseInt(value), rarity));
        });
        return result;
    }

    private static Map<String, List<FishingShakeTreasure>> loadShake(FlatYamlConfig yaml) {
        Map<String, Map<String, Map<String, String>>> grouped = new LinkedHashMap<>();
        yaml.valuesWithPrefix("Shake.").forEach((path, value) -> {
            String[] parts = path.split("\\.");
            if (parts.length < 4) {
                return;
            }
            grouped.computeIfAbsent(parts[1], ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(parts[2], ignored -> new LinkedHashMap<>())
                    .put(String.join(".", java.util.Arrays.copyOfRange(parts, 3, parts.length)), value);
        });
        Map<String, List<FishingShakeTreasure>> result = new HashMap<>();
        grouped.forEach((entity, drops) -> {
            ArrayList<FishingShakeTreasure> loaded = new ArrayList<>();
            drops.forEach((key, values) -> {
                if (key.equalsIgnoreCase("INVENTORY")) {
                    ShakeInventoryDefinition definition = parseInventoryShakeDefinition(values);
                    if (definition != null) {
                        loaded.add(FishingShakeTreasure.inventory(
                                definition.chancePercent(),
                                definition.dropLevel(),
                                definition.wholeStacks()));
                    }
                    return;
                }
                Item item = resolveItem(key);
                String chanceValue = values.get("Drop_Chance");
                if (item == null || chanceValue == null) {
                    return;
                }
                loaded.add(new FishingShakeTreasure(
                        item,
                        Integer.parseInt(values.getOrDefault("Amount", "1")),
                        Double.parseDouble(chanceValue),
                        Integer.parseInt(values.getOrDefault("Drop_Level", "0")),
                        values.getOrDefault("PotionData.PotionType", "")));
            });
            if (!loaded.isEmpty()) {
                result.put(normalizeEntity(entity), List.copyOf(loaded));
            }
        });
        return result;
    }


    private static ShakeInventoryDefinition loadInventoryShakeDefinition(
            FlatYamlConfig yaml,
            String entityName) {
        String prefix = "Shake." + entityName + ".INVENTORY.";
        Map<String, String> values = new LinkedHashMap<>();
        yaml.valuesWithPrefix(prefix).forEach((path, value) -> {
            String key = path.substring(prefix.length());
            values.put(key, value);
        });
        return parseInventoryShakeDefinition(values);
    }

    private static ShakeInventoryDefinition parseInventoryShakeDefinition(Map<String, String> values) {
        String chanceValue = values.get("Drop_Chance");
        if (chanceValue == null) {
            return null;
        }
        return new ShakeInventoryDefinition(
                Double.parseDouble(chanceValue),
                Integer.parseInt(values.getOrDefault("Drop_Level", "0")),
                Boolean.parseBoolean(values.getOrDefault("Whole_Stacks", "false")));
    }

    private static Map<Integer, Map<FishingRarity, Double>> loadRates(
            FlatYamlConfig yaml,
            String prefix) {
        Map<Integer, Map<FishingRarity, Double>> result = new HashMap<>();
        yaml.valuesWithPrefix(prefix).forEach((path, value) -> {
            String[] parts = path.split("\\.");
            if (parts.length != 3 || !parts[1].startsWith("Tier_")) {
                return;
            }
            int tier = Integer.parseInt(parts[1].substring("Tier_".length()));
            result.computeIfAbsent(tier, ignored -> new EnumMap<>(FishingRarity.class))
                    .put(FishingRarity.parse(parts[2]), Double.parseDouble(value));
        });
        return result;
    }

    private static Map<String, Map<String, String>> group(
            FlatYamlConfig yaml,
            String prefix,
            int keyIndex) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        yaml.valuesWithPrefix(prefix).forEach((path, value) -> {
            String[] parts = path.split("\\.");
            if (parts.length <= keyIndex + 1) {
                return;
            }
            result.computeIfAbsent(parts[keyIndex], ignored -> new LinkedHashMap<>())
                    .put(String.join(".", java.util.Arrays.copyOfRange(
                            parts, keyIndex + 1, parts.length)), value);
        });
        return result;
    }

    private static Item resolveItem(String configuredKey) {
        String base = configuredKey.split("\\|", 2)[0].toLowerCase(Locale.ROOT);
        Item item = Registries.ITEM.get(Identifier.of("minecraft", base));
        return item == Items.AIR && !base.equals("air") ? null : item;
    }

    private static String normalizeEntity(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        return ENTITY_ALIASES.getOrDefault(upper, upper.toLowerCase(Locale.ROOT));
    }

    private static double rate(
            Map<Integer, Map<FishingRarity, Double>> rates,
            int tier,
            FishingRarity rarity) {
        return rates.getOrDefault(Math.max(1, Math.min(8, tier)), Map.of())
                .getOrDefault(rarity, 0.0D);
    }

    private static <T> EnumMap<FishingRarity, List<T>> enumLists() {
        EnumMap<FishingRarity, List<T>> result = new EnumMap<>(FishingRarity.class);
        for (FishingRarity rarity : FishingRarity.values()) {
            result.put(rarity, new ArrayList<>());
        }
        return result;
    }

    private static <T> Map<FishingRarity, List<T>> immutableEnumLists(
            Map<FishingRarity, List<T>> source) {
        EnumMap<FishingRarity, List<T>> result = new EnumMap<>(FishingRarity.class);
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    private static Map<Integer, Map<FishingRarity, Double>> immutableRates(
            Map<Integer, Map<FishingRarity, Double>> source) {
        Map<Integer, Map<FishingRarity, Double>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, Map.copyOf(value)));
        return Map.copyOf(result);
    }

    record ShakeInventoryDefinition(double chancePercent, int dropLevel, boolean wholeStacks) {
        ShakeInventoryDefinition {
            if (chancePercent < 0.0D || dropLevel < 0) {
                throw new IllegalArgumentException("Invalid inventory Shake definition");
            }
        }
    }

    private record BookFilters(Set<NamespacedId> whitelist, Set<NamespacedId> blacklist) {
    }

    private static <T> Map<String, List<T>> immutableLists(Map<String, List<T>> source) {
        Map<String, List<T>> result = new HashMap<>();
        source.forEach((key, value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }
}
